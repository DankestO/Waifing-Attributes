package com.waifing.waifingattributes.rogue;

import com.waifing.waifingattributes.ModAttributes;
import com.waifing.waifingattributes.WaifingAttributesMod;
import com.waifing.waifingattributes.compat.IronsSummons;
import com.waifing.waifingattributes.util.ChanceUtil;
import com.waifing.waifingattributes.config.WaifingConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraft.tags.DamageTypeTags;
import com.waifing.waifingattributes.compat.ConsecrationCompat;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Stealth and combat attributes. All event driven (no ticks, no mixins); with the attribute at 0 each handler leaves right away.
 *
 *  hunter_stealth   (PERCENT): mobs see you from a shorter distance. Vanilla detection range is range x visibility of the target and
 *                   Forge lets us scale that visibility (LivingVisibilityEvent): 0.5 = mobs notice you from half the distance.
 *  hunter_ambush    (AMOUNT, max 4.0): surprise hit. Extra damage on a mob that has no target and that nobody hurt in the last 5 seconds
 *                   (vanilla forgets the attacker after 100 ticks). The hit itself makes the mob aware, so it is one hit per encounter,
 *                   whoever lands it: it does not scale in multiplayer. Never against players.
 *  rogue_backstab   (AMOUNT, max 1.0): extra damage when the hit comes from behind (within 60 degrees). Melee and instant spells are judged
 *                   by your position, projectiles (arrows, bullets, spell projectiles) by their flight direction. Works on players too.
 *                   Backstab and ambush ADD UP: 1 + 1.0 + 4.0 = x6 at most.
 *  rogue_nightkin   (AMOUNT, max 2.0):
 *                   1.0 = undead mobs (MobType.UNDEAD, which vanilla and most mods use, or the optional tag minecraft:undead) never pick you
 *                         as a target. Bosses (forge:bosses) and Iron's Spellbooks summons are excluded. A mob you hurt can target you.
 *                   2.0 = you ARE undead (PlayerMixin), idle undead nearby attack whoever you fight (players and non-undead monsters) and
 *                         whoever hurts you, and daylight burns you (unless wearing a helmet) and weakens you. The daylight check is the
 *                         only periodic code of the mod: once per second, and only for players with nightkin 2.0.
 *                         While vulnerable (burning, Consecration's fire/holy vulnerability) the player cannot heal at all.
 *  tamer_pet_damage (AMOUNT, max 3.0): extra damage of your pets (any OwnableEntity whose owner is you). Iron's summons are excluded:
 *                   they scale with irons_spellbooks:summon_damage.
 *  explorer_fall    (PERCENT): less fall damage, 1.0 = none.
 */
@Mod.EventBusSubscriber(modid = WaifingAttributesMod.MODID)
public final class RogueHandler {
    /** 1.20.1 has no vanilla undead entity tag; this one is optional (empty unless a datapack or mod fills it) for undead without MobType.UNDEAD. */
    private static final TagKey<EntityType<?>> UNDEAD_TAG = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("minecraft", "undead"));

    @SubscribeEvent
    public static void onVisibility(LivingEvent.LivingVisibilityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p instanceof FakePlayer) return;
        double stealth = ChanceUtil.chance(p.getAttributeValue(ModAttributes.HUNTER_STEALTH.get()));
        if (stealth > 0.0D) event.modifyVisibility(1.0D - stealth);
    }

    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent event) {
        if (!(event.getNewTarget() instanceof ServerPlayer p) || p instanceof FakePlayer) return;
        if (p.getAttributeValue(ModAttributes.ROGUE_NIGHTKIN.get()) <= 0.0D) return;
        if (!(event.getEntity() instanceof Mob mob) || mob.getLastHurtByMob() == p) return;
        if (isUndeadKin(mob)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        DamageSource src = event.getSource();
        if (src.is(DamageTypes.THORNS)) return;
        Entity attacker = src.getEntity();
        LivingEntity target = event.getEntity();

        if (attacker instanceof ServerPlayer p && !(p instanceof FakePlayer)) {
            double mult = 1.0D;
            double back = p.getAttributeValue(ModAttributes.ROGUE_BACKSTAB.get());
            if (back > 0.0D && fromBehind(src, p, target)) mult += back;
            double ambush = p.getAttributeValue(ModAttributes.HUNTER_AMBUSH.get());
            if (ambush > 0.0D && target instanceof Mob mob && mob.getTarget() == null && mob.getLastHurtByMob() == null) mult += ambush;
            if (mult > 1.0D) event.setAmount((float) (event.getAmount() * mult));
            // nightkin 2.0: the undead around you join your fight
            if (isUndeadPlayer(p) && target != p && (target instanceof Player || (target instanceof Monster && !isUndead(target)))) callHelp(p, target);
        } else if (attacker instanceof OwnableEntity pet && !IronsSummons.isSummon(attacker) && pet.getOwner() instanceof ServerPlayer owner) {
            double bonus = owner.getAttributeValue(ModAttributes.TAMER_PET_DAMAGE.get());
            if (bonus > 0.0D) event.setAmount((float) (event.getAmount() * (1.0D + bonus)));
        }
        // nightkin 2.0: remember fire damage (vulnerability when Consecration is not installed)
        if (target instanceof ServerPlayer up && src.is(DamageTypeTags.IS_FIRE) && isUndeadPlayer(up)) LAST_FIRE.put(up, up.level().getGameTime());
        // nightkin 2.0: the undead around you defend you
        if (target instanceof ServerPlayer vp && !(vp instanceof FakePlayer) && attacker instanceof LivingEntity la && la != vp
                && !isUndead(la) && isUndeadPlayer(vp)) callHelp(vp, la);
    }

    // ------------------------------------------------------------------ nightkin helpers
    private static final Map<ServerPlayer, Long> LAST_HELP = new WeakHashMap<>();

    private static final Map<ServerPlayer, Long> LAST_FIRE = new WeakHashMap<>();

    /**
     * nightkin 2.0: no healing of any kind (food, potions, spells, Consecration) while vulnerable. Mobs have no natural regeneration,
     * players do: without this, Consecration's vulnerability only stopped its own small regeneration and food healed the player anyway.
     * Vulnerable = burning, or Consecration says so (fire / holy damage), or, without Consecration, fire damage in the last seconds.
     */
    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p instanceof FakePlayer || !isUndeadPlayer(p)) return;
        if (!WaifingConfig.NIGHTKIN_NO_HEAL_VULNERABLE.get()) return;
        if (isVulnerable(p)) event.setCanceled(true);
    }

    private static boolean isVulnerable(ServerPlayer p) {
        if (p.isOnFire()) return true;
        if (ConsecrationCompat.present()) return ConsecrationCompat.isVulnerable(p);
        Long last = LAST_FIRE.get(p);
        return last != null && p.level().getGameTime() - last < WaifingConfig.NIGHTKIN_FIRE_VULNERABLE.get() * 20L;
    }

    private static boolean isUndead(LivingEntity e) {
        return e.getMobType() == MobType.UNDEAD || e.getType().is(UNDEAD_TAG);
    }

    /** Undead that nightkin affects: not bosses, not Iron's summons. */
    private static boolean isUndeadKin(Mob mob) {
        return isUndead(mob) && !mob.getType().is(Tags.EntityTypes.BOSSES) && !IronsSummons.isSummon(mob);
    }

    private static boolean isUndeadPlayer(ServerPlayer p) {
        return p.getAttributeValue(ModAttributes.ROGUE_NIGHTKIN.get()) >= 2.0D;
    }

    private static void callHelp(ServerPlayer p, LivingEntity enemy) {
        int radius = WaifingConfig.NIGHTKIN_ALLY_RADIUS.get();
        int max = WaifingConfig.NIGHTKIN_ALLY_MAX.get();
        if (radius <= 0 || max <= 0 || !enemy.isAlive()) return;
        long now = p.level().getGameTime();
        Long last = LAST_HELP.get(p);
        if (last != null && now - last < WaifingConfig.NIGHTKIN_ALLY_COOLDOWN.get()) return;
        LAST_HELP.put(p, now);
        if (enemy instanceof OwnableEntity o && o.getOwner() == p) return; // never against your own pets
        List<Mob> helpers = p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(radius),
                m -> m != enemy && m.isAlive() && m.getTarget() == null && m.getLastHurtByMob() != p && isUndeadKin(m));
        int n = 0;
        for (Mob m : helpers) {
            m.setTarget(enemy);
            if (++n >= max) break;
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p)) return;
        if ((p.tickCount + p.getId()) % 20 != 0) return; // once per second, spread over players
        if (p instanceof FakePlayer || p.isCreative() || p.isSpectator() || !isUndeadPlayer(p)) return;
        Level level = p.level();
        if (!level.isDay() || p.getLightLevelDependentMagicValue() <= 0.5F) return;
        if (p.isInWaterRainOrBubble() || p.isInPowderSnow) return;
        if (!level.canSeeSky(BlockPos.containing(p.getX(), p.getEyeY(), p.getZ()))) return;

        int weakness = WaifingConfig.NIGHTKIN_SUN_WEAKNESS.get();
        if (weakness > 0) p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, weakness - 1, true, false, true));
        if (!WaifingConfig.NIGHTKIN_SUN_BURN.get()) return;
        ItemStack helmet = p.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.isEmpty()) {
            p.setSecondsOnFire(8);
        } else if (helmet.isDamageableItem() && p.getRandom().nextBoolean()) {
            helmet.hurtAndBreak(1, p, e -> e.broadcastBreakEvent(EquipmentSlot.HEAD));
        }
    }

    /** The damage travels the way the target faces = it comes from behind (within 60 degrees). */
    private static boolean fromBehind(DamageSource src, ServerPlayer p, LivingEntity target) {
        double dx, dz;
        Entity direct = src.getDirectEntity();
        Vec3 motion = direct instanceof Projectile ? direct.getDeltaMovement() : Vec3.ZERO;
        if (motion.x * motion.x + motion.z * motion.z > 1.0E-4D) { dx = motion.x; dz = motion.z; }
        else { dx = target.getX() - p.getX(); dz = target.getZ() - p.getZ(); }
        Vec3 look = target.getViewVector(1.0F);
        double len = Math.sqrt(dx * dx + dz * dz) * Math.sqrt(look.x * look.x + look.z * look.z);
        if (len < 1.0E-6D) return false;
        return (look.x * dx + look.z * dz) / len > 0.5D;
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p instanceof FakePlayer) return;
        double f = ChanceUtil.chance(p.getAttributeValue(ModAttributes.EXPLORER_FALL.get()));
        if (f > 0.0D) event.setDamageMultiplier((float) (event.getDamageMultiplier() * (1.0D - f)));
    }

    private RogueHandler() {}
}
