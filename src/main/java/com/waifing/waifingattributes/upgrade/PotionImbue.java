package com.waifing.waifingattributes.upgrade;

import com.waifing.waifingattributes.ModAttributes;
import com.waifing.waifingattributes.WaifingAttributesMod;
import com.waifing.waifingattributes.config.WaifingConfig;
import com.waifing.waifingattributes.util.ChanceUtil;
import com.waifing.waifingattributes.util.Fx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Potion on the cursor, right-clicked onto a weapon: the weapon stores the potion's effects (already scaled to the
 * configured fraction of the duration) and a number of hits. Every melee hit applies them to the target.
 * The hit handler only does work when the weapon in the player's hand carries the tag.
 */
@Mod.EventBusSubscriber(modid = WaifingAttributesMod.MODID)
public final class PotionImbue {
    public static final String TAG = "WaifingImbue";

    public static void run(UpgradeContext ctx) {
        ServerPlayer p = ctx.player();
        ItemStack weapon = ctx.target();
        ItemStack potion = ctx.catalyst();

        double durAttr = p.getAttributeValue(ModAttributes.BLACKSMITH_IMBUE_WEAPON_DURATION.get());
        double hitsAttr = p.getAttributeValue(ModAttributes.BLACKSMITH_IMBUE_WEAPON_HITS.get());
        List<MobEffectInstance> effects = PotionUtils.getMobEffects(potion);
        // blacksmith_imbue_weapon_hits: 0.01 = 1 hit ... 1.0 = hitsAtMax (100) hits.
        int hits = (int) Math.round(ChanceUtil.chance(hitsAttr) * WaifingConfig.IMBUE_HITS.get());
        // blacksmith_imbue_weapon_duration: fraction of the potion's duration each hit applies (0.01 = 1% ... 1.0 = 100%)
        double factor = ChanceUtil.chance(durAttr);
        if (effects.isEmpty() || factor <= 0.0D || hitsAttr <= 0.0D || has(weapon)) {
            Fx.denied(p); // plain water, or the weapon is already imbued
            return;
        }

        ListTag list = new ListTag();
        for (MobEffectInstance e : effects) {
            int dur = e.getDuration();
            if (!e.getEffect().isInstantenous() && dur > 0) dur = Math.max(1, (int) Math.round(dur * factor));
            list.add(new MobEffectInstance(e.getEffect(), dur, e.getAmplifier(), e.isAmbient(), e.isVisible(), e.showIcon())
                    .save(new CompoundTag()));
        }
        CompoundTag c = new CompoundTag();
        c.putInt("Hits", Math.max(1, hits));
        c.put("Effects", list);
        weapon.getOrCreateTag().put(TAG, c);

        ctx.consumeCatalyst();
        if (WaifingConfig.IMBUE_RETURN_BOTTLE.get() && !p.getAbilities().instabuild) {
            p.getInventory().placeItemBackInInventory(new ItemStack(Items.GLASS_BOTTLE));
        }
        Fx.sound(p, WaifingConfig.POTION_OK_SOUND.get());
        Fx.particles(p, ParticleTypes.WITCH, 20);
    }

    // ------------------------------------------------------------------ arrows
    /**
     * Potion on the cursor, RIGHT-click a stack of plain arrows: the whole stack becomes arrows with the potion's effects. Unlike
     * vanilla tipped arrows (1/8 of the duration), each arrow applies the potion's own potency and duration (upgrades included)
     * times blacksmith_imbue_weapon_duration. The effects are stored as custom effects only, so vanilla does not divide them.
     */
    public static void runArrows(UpgradeContext ctx) {
        ServerPlayer p = ctx.player();
        ItemStack arrows = ctx.target();
        List<MobEffectInstance> effects = PotionUtils.getMobEffects(ctx.catalyst());
        double factor = ChanceUtil.chance(p.getAttributeValue(ModAttributes.BLACKSMITH_IMBUE_WEAPON_DURATION.get()));
        if (effects.isEmpty() || factor <= 0.0D) {
            Fx.denied(p); // plain water, or no duration attribute
            return;
        }
        List<MobEffectInstance> scaled = new ArrayList<>();
        for (MobEffectInstance e : effects) {
            int dur = e.getDuration();
            if (!e.getEffect().isInstantenous() && dur > 0) dur = Math.max(1, (int) Math.round(dur * factor));
            scaled.add(new MobEffectInstance(e.getEffect(), dur, e.getAmplifier(), e.isAmbient(), e.isVisible(), e.showIcon()));
        }
        ItemStack out = new ItemStack(Items.TIPPED_ARROW, arrows.getCount());
        PotionUtils.setCustomEffects(out, scaled);
        out.getOrCreateTag().putInt("CustomPotionColor", PotionUtils.getColor(scaled));
        ctx.replaceTarget(out);

        ctx.consumeCatalyst();
        if (WaifingConfig.IMBUE_RETURN_BOTTLE.get() && !p.getAbilities().instabuild) {
            p.getInventory().placeItemBackInInventory(new ItemStack(Items.GLASS_BOTTLE));
        }
        Fx.sound(p, WaifingConfig.POTION_OK_SOUND.get());
        Fx.particles(p, ParticleTypes.WITCH, 20);
    }

    // ------------------------------------------------------------------ food
    public static final String FOOD_TAG = "WaifingFoodImbue";

    /** Any edible item that is not already imbued (potions and other drinks with their own effects are left alone). */
    public static boolean isImbuableFood(ItemStack stack) {
        return !stack.isEmpty() && stack.isEdible() && !(stack.getItem() instanceof net.minecraft.world.item.PotionItem)
                && !hasFood(stack);
    }

    /** Potion on the cursor, food clicked: ONE food item gets the potion's effects (1:1), the rest of the stack stays as it was. */
    public static void runFood(UpgradeContext ctx) {
        ServerPlayer p = ctx.player();
        ItemStack food = ctx.target();
        ItemStack potion = ctx.catalyst();
        List<MobEffectInstance> effects = PotionUtils.getMobEffects(potion);
        if (effects.isEmpty()) {
            Fx.denied(p);
            return;
        }
        // chef_imbue_food: fraction of the potion's duration the eater gets (0.01 = 1% ... 1.0 = 100%)
        double factor = ChanceUtil.chance(ctx.player().getAttributeValue(ModAttributes.CHEF_IMBUE_FOOD.get()));
        if (factor <= 0.0D) {
            Fx.denied(p);
            return;
        }
        ListTag list = new ListTag();
        for (MobEffectInstance e : effects) {
            int dur = e.getDuration();
            if (!e.getEffect().isInstantenous() && dur > 0) dur = Math.max(1, (int) Math.round(dur * factor));
            list.add(new MobEffectInstance(e.getEffect(), dur, e.getAmplifier(), e.isAmbient(), e.isVisible(), e.showIcon())
                    .save(new CompoundTag()));
        }
        CompoundTag c = new CompoundTag();
        c.put("Effects", list);

        ctx.consumeCatalyst();
        if (food.getCount() > 1) {
            ItemStack one = food.copyWithCount(1);
            one.getOrCreateTag().put(FOOD_TAG, c);
            food.shrink(1);
            ctx.give(one);
        } else {
            food.getOrCreateTag().put(FOOD_TAG, c);
        }
        if (WaifingConfig.IMBUE_RETURN_BOTTLE.get() && !p.getAbilities().instabuild) {
            p.getInventory().placeItemBackInInventory(new ItemStack(Items.GLASS_BOTTLE));
        }
        Fx.sound(p, WaifingConfig.POTION_OK_SOUND.get());
        Fx.particles(p, ParticleTypes.WITCH, 20);
    }

    public static boolean hasFood(ItemStack stack) {
        CompoundTag t = stack.getTag();
        return t != null && t.contains(FOOD_TAG, 10);
    }

    public static List<MobEffectInstance> foodEffects(ItemStack stack) {
        List<MobEffectInstance> out = new ArrayList<>();
        if (!hasFood(stack)) return out;
        ListTag list = stack.getTag().getCompound(FOOD_TAG).getList("Effects", 10);
        for (int i = 0; i < list.size(); i++) {
            MobEffectInstance e = MobEffectInstance.load(list.getCompound(i));
            if (e != null) out.add(e);
        }
        return out;
    }

    /** The imbued food has been eaten: its effects go to the player. The stack keeps its tag after being shrunk. */
    @SubscribeEvent
    public static void onEaten(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack eaten = event.getItem();
        if (!hasFood(eaten)) return;
        for (MobEffectInstance e : foodEffects(eaten)) {
            if (e.getEffect().isInstantenous()) e.getEffect().applyInstantenousEffect(null, null, player, e.getAmplifier(), 1.0D);
            else player.addEffect(new MobEffectInstance(e));
        }
    }

    public static boolean has(ItemStack stack) {
        CompoundTag t = stack.getTag();
        return t != null && t.contains(TAG, 10);
    }

    public static int hits(ItemStack stack) {
        return has(stack) ? stack.getTag().getCompound(TAG).getInt("Hits") : 0;
    }

    public static List<MobEffectInstance> effects(ItemStack stack) {
        List<MobEffectInstance> out = new ArrayList<>();
        if (!has(stack)) return out;
        ListTag list = stack.getTag().getCompound(TAG).getList("Effects", 10);
        for (int i = 0; i < list.size(); i++) {
            MobEffectInstance e = MobEffectInstance.load(list.getCompound(i));
            if (e != null) out.add(e);
        }
        return out;
    }

    /** True while the imbued effects are being applied: an instant effect (harming...) hurts the target and re-enters onHurt. */
    private static boolean applying;

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingHurtEvent event) {
        if (applying) return; // re-entrancy guard (effects such as harming deal damage themselves)
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (event.getSource().getDirectEntity() != player) return; // melee only
        ItemStack weapon = player.getMainHandItem();
        if (!has(weapon)) return;
        LivingEntity target = event.getEntity();
        if (target == player) return;

        // Read the effects and spend the hit BEFORE applying anything, so nothing can make this hit count twice.
        List<MobEffectInstance> effects = effects(weapon);
        CompoundTag c = weapon.getTag().getCompound(TAG);
        int left = c.getInt("Hits") - 1;
        if (left <= 0) weapon.removeTagKey(TAG);
        else c.putInt("Hits", left);

        applying = true;
        try {
            for (MobEffectInstance e : effects) {
                if (e.getEffect().isInstantenous()) {
                    e.getEffect().applyInstantenousEffect(player, player, target, e.getAmplifier(), 1.0D);
                } else {
                    target.addEffect(new MobEffectInstance(e), player);
                }
            }
        } finally {
            applying = false;
        }
    }

    private PotionImbue() {}
}
