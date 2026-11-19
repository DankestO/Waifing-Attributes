package com.waifing.waifingattributes.util;

import com.waifing.waifingattributes.config.WaifingConfig;
import net.minecraft.util.RandomSource;

public final class ChanceUtil {

    /** PERCENT attributes: 0.01 = 1% ... 1.0 = 100%; anything above 1.0 is still 100%. */
    public static double chance(double attr) {
        return Math.max(0.0D, Math.min(1.0D, Math.round(attr * 10000.0D) / 10000.0D));
    }

    /** Roll a PERCENT attribute. */
    public static boolean roll(double attr, RandomSource rnd) {
        double c = chance(attr);
        return c > 0.0D && rnd.nextDouble() < c;
    }

    /** Quality-level attributes: 1.0 = one level more, 2.0 = two... (whole part; the fraction does not count). */
    public static int levels(double attr) {
        return (int) Math.floor(attr + 1.0E-6);
    }

    /** Value 2.5 -> 2 guaranteed + 50% for a third. Value 0.5 -> 50% of 1. */
    public static int rolls(double value, RandomSource rnd, int cap) {
        double v = Math.round(value * 10000.0D) / 10000.0D;
        if (v <= 0) return 0;
        int whole = (int) Math.floor(v);
        double frac = v - whole;
        if (frac > 1.0E-6 && rnd.nextDouble() < frac) whole++;
        return Math.min(whole, cap);
    }

    /**
     * Generic "tier" model shared by equipment, potions and enchantment books.
     *
     * t = ceil(attr), f = fractional part of attr (1.0 when it is a whole number).
     *   slot < t  -> 100%
     *   slot == t -> f
     *   slot > t  -> f * falloff^(slot - t)
     *
     * Examples (falloff 0.5): attr 0.4 -> 40/20/10% | attr 1.0 -> 100/50/25% | attr 1.6 -> 100/60/30/15%.
     * {@code slot} is 1-based: the n-th upgrade step of the thing being upgraded.
     */
    public static double tier(double attr, int slot, double falloff) {
        double a = Math.round(attr * 10000.0D) / 10000.0D;
        if (a <= 0) return 0.0D;
        if (slot < 1) return 1.0D;
        int t = (int) Math.ceil(a);
        double f = a - Math.floor(a);
        if (f < 1.0E-9) f = 1.0D;
        if (slot < t) return 1.0D;
        if (slot == t) return f;
        return f * Math.pow(falloff, slot - t);
    }

    /** Success probability for equipment slot {@code target} (1..max). */
    public static double equipmentSuccess(double attr, int target) {
        return tier(attr, target, WaifingConfig.EQUIP_TIER_FALLOFF.get());
    }

    /** Potion: chance of upgrade step {@code step} (1..maxLevels). Attribute 10.0 makes every one of the 10 steps certain. */
    public static double potionSuccess(double attr, int step) {
        return tier(attr, step, WaifingConfig.POTION_TIER_FALLOFF.get());
    }

    /** Integer amount scaled by {@code factor}, rounding at random so small values keep the right average (3 * 0.5 = 1 or 2). */
    public static int scaleRandom(int amount, double factor, RandomSource rnd) {
        double v = amount * factor;
        int whole = (int) Math.floor(v);
        if (rnd.nextDouble() < v - whole) whole++;
        return whole;
    }

    private ChanceUtil() {}
}
