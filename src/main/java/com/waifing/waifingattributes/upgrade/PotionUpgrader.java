package com.waifing.waifingattributes.upgrade;

import com.waifing.waifingattributes.ModAttributes;
import com.waifing.waifingattributes.config.WaifingConfig;
import com.waifing.waifingattributes.util.ChanceUtil;
import com.waifing.waifingattributes.util.Fx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Ingredient or catalyst on the cursor, right-clicked onto a potion (a stack counts as one: one catalyst, one roll).
 *
 * 1. Alchemy: if the ingredient has a brewing recipe for that potion, it is brewed (see {@link PotionBrewer}).
 * 2. Upgrade (catalyst, lapis by default), only once vanilla cannot improve the potion any more:
 *    - potions with duration: +10% of the BASE duration per level (10 levels per cycle);
 *    - instant potions (healing, harming...): +1 potency per level (1 level per cycle; the Rebirth is the 2nd level).
 * 3. Rebirth (diamond by default) when the cycle is complete: a new cycle starts and the potency rises one level
 *    (in potions that have duration the duration is kept). Effects without levels (fire resistance...) only reset the counter.
 */
public final class PotionUpgrader {

    public static void run(UpgradeContext ctx) {
        ServerPlayer p = ctx.player();
        ItemStack main = ctx.target();
        ItemStack cat = ctx.catalyst();

        if (PotionBrewer.brew(ctx)) return;

        List<MobEffectInstance> src = PotionUtils.getMobEffects(main);
        if (src.isEmpty()) {
            Fx.denied(p);
            return;
        }
        boolean catalyst = WaifingConfig.isPotionCatalyst(cat);
        boolean rebirthItem = WaifingConfig.isPotionRebirthListed(cat);

        PotionData d = PotionData.read(main);
        boolean timed = false;
        for (MobEffectInstance e : src) if (!e.getEffect().isInstantenous()) timed = true;
        double attr = p.getAttributeValue(ModAttributes.ALCHEMIST_POTION_UPGRADE.get());

        if (d.cycle < steps(timed)) {
            if (!catalyst) {
                Fx.denied(p);
                return;
            }
            if (!d.touched && WaifingConfig.POTION_REQUIRE_VANILLA_MAX.get() && !vanillaMaxed(main)) {
                Fx.denied(p); // vanilla can still improve it with redstone or glowstone
                return;
            }
            stepUpgrade(ctx, src, d, attr, timed);
        } else {
            if (!rebirthItem) {
                Fx.denied(p);
                return;
            }
            rebirth(ctx, src, d, timed);
        }
    }

    // ------------------------------------------------------------------ upgrade level
    private static void stepUpgrade(UpgradeContext ctx, List<MobEffectInstance> src, PotionData d, double attr, boolean timed) {
        ServerPlayer p = ctx.player();
        ItemStack main = ctx.target();
        int maxAmp = WaifingConfig.POTION_MAX_AMPLIFIER.get();
        int maxDur = WaifingConfig.POTION_MAX_DURATION.get();

        // Nothing left to gain: do not waste the catalyst.
        boolean room = false;
        for (MobEffectInstance e : src) {
            if (timed ? (!e.getEffect().isInstantenous() && e.getDuration() > 0 && e.getDuration() < maxDur)
                    : e.getAmplifier() < maxAmp) room = true;
        }
        if (!room) {
            Fx.denied(p);
            return;
        }

        int step = d.cycle + 1;
        boolean ok = p.getRandom().nextDouble() < ChanceUtil.potionSuccess(attr, step);
        if (ok || WaifingConfig.CONSUME_CATALYST_ON_FAIL.get()) ctx.consumeCatalyst(ModAttributes.ALCHEMIST_KEEP_CATALYST);
        if (!ok) {
            fail(ctx, WaifingConfig.POTION_DESTROY_ON_FAIL.get());
            return;
        }

        List<MobEffectInstance> out = new ArrayList<>();
        if (timed) {
            if (d.base == null) {
                d.base = new int[src.size()];
                for (int i = 0; i < src.size(); i++) d.base[i] = src.get(i).getDuration();
            }
            d.total++;
            double pct = WaifingConfig.POTION_DURATION_STEP.get();
            for (int i = 0; i < src.size(); i++) {
                MobEffectInstance e = src.get(i);
                int dur = e.getDuration();
                if (!e.getEffect().isInstantenous() && dur > 0 && i < d.base.length) {
                    long v = Math.round(d.base[i] * (1.0D + pct * d.total));
                    dur = (int) Math.min(v, (long) maxDur);
                }
                out.add(copy(e, dur, e.getAmplifier()));
            }
        } else {
            for (MobEffectInstance e : src) out.add(copy(e, e.getDuration(), Math.min(e.getAmplifier() + 1, maxAmp)));
        }
        d.cycle = step;
        d.store(main, out);
        Fx.sound(p, WaifingConfig.POTION_OK_SOUND.get());
        Fx.particles(p, ParticleTypes.HAPPY_VILLAGER, 12);
    }

    // ------------------------------------------------------------------ rebirth
    private static void rebirth(UpgradeContext ctx, List<MobEffectInstance> src, PotionData d, boolean timed) {
        ServerPlayer p = ctx.player();
        ItemStack main = ctx.target();
        double chance = p.getAttributeValue(ModAttributes.ALCHEMIST_POTION_REBIRTH.get()); // PERCENT attribute
        if (chance <= 0.0D) {
            Fx.denied(p);
            return;
        }
        ctx.consumeCatalyst(ModAttributes.ALCHEMIST_KEEP_CATALYST);
        if (ChanceUtil.roll(chance, p.getRandom())) {
            int maxAmp = WaifingConfig.POTION_MAX_AMPLIFIER.get();
            List<MobEffectInstance> out = new ArrayList<>();
            for (MobEffectInstance e : src) {
                int amp = e.getAmplifier();
                if (!hasNoLevels(e)) amp = Math.min(amp + 1, maxAmp); // timed or instant: the rebirth is the extra potency level
                out.add(copy(e, e.getDuration(), amp)); // the duration is kept
            }
            d.cycle = 0;
            d.rebirths++;
            d.store(main, out);
            Fx.rebirthSuccess(p);
            Fx.particles(p, ParticleTypes.TOTEM_OF_UNDYING, 40);
        } else if (WaifingConfig.POTION_REBIRTH_DESTROY.get()) {
            ctx.destroyTarget();
            Fx.sound(p, WaifingConfig.POTION_FAIL_SOUND.get());
            Fx.particles(p, ParticleTypes.POOF, 20);
        } else {
            Fx.sound(p, WaifingConfig.POTION_FAIL_SOUND.get());
        }
    }

    // ------------------------------------------------------------------ helpers
    /** Upgrade levels per cycle: potions with duration use {@code maxLevels}; instant potions (potency only) use {@code instantMaxLevels}. */
    public static int steps(boolean timed) {
        return timed ? ModAttributes.MAX_UPGRADE_LEVELS : ModAttributes.INSTANT_POTION_LEVELS;
    }

    public static boolean isTimed(ItemStack potion) {
        for (MobEffectInstance e : PotionUtils.getMobEffects(potion)) if (!e.getEffect().isInstantenous()) return true;
        return false;
    }

    private static boolean hasNoLevels(MobEffectInstance e) {
        ResourceLocation id = ForgeRegistries.MOB_EFFECTS.getKey(e.getEffect());
        return id != null && WaifingConfig.isNoLevelEffect(id);
    }

    private static MobEffectInstance copy(MobEffectInstance e, int dur, int amp) {
        return new MobEffectInstance(e.getEffect(), dur, amp, e.isAmbient(), e.isVisible(), e.showIcon());
    }

    /** True when vanilla cannot improve this potion any more (it is already long_/strong_, or has no such version, or is custom). */
    private static boolean vanillaMaxed(ItemStack stack) {
        Potion potion = PotionUtils.getPotion(stack);
        if (potion == Potions.EMPTY) return true;
        ResourceLocation key = ForgeRegistries.POTIONS.getKey(potion);
        if (key == null) return true;
        String path = key.getPath();
        if (path.startsWith("long_") || path.startsWith("strong_")) return true;
        return !ForgeRegistries.POTIONS.containsKey(new ResourceLocation(key.getNamespace(), "long_" + path))
                && !ForgeRegistries.POTIONS.containsKey(new ResourceLocation(key.getNamespace(), "strong_" + path));
    }

    private static void fail(UpgradeContext ctx, boolean destroy) {
        ServerPlayer p = ctx.player();
        Fx.sound(p, WaifingConfig.POTION_FAIL_SOUND.get());
        if (destroy) {
            ctx.destroyTarget(); // the bottle(s) explode
            Fx.particles(p, ParticleTypes.POOF, 20);
        }
    }

    private PotionUpgrader() {}
}
