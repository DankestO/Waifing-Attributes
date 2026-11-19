package com.waifing.waifingattributes.util;

import com.waifing.waifingattributes.WaifingAttributesMod;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.fml.ModList;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * "Natural" maximum level of an enchantment: vanilla's, or Apotheosis' when that mod is installed.
 * Apotheosis is an OPTIONAL dependency, so it is reached by reflection and never referenced at compile time.
 */
public final class EnchantLevels {
    private static volatile boolean resolved;
    private static MethodHandle apothMax;

    public static int naturalMax(Enchantment ench) {
        MethodHandle h = apotheosis();
        if (h != null) {
            try {
                return (int) h.invoke(ench);
            } catch (Throwable ignored) {
                // fall back to vanilla
            }
        }
        return ench.getMaxLevel();
    }

    private static MethodHandle apotheosis() {
        if (!resolved) {
            ModList list = ModList.get();
            if (list == null) return null; // too early, try again later
            synchronized (EnchantLevels.class) {
                if (!resolved) {
                    try {
                        if (list.isLoaded("apotheosis")) {
                            Class<?> hooks = Class.forName("dev.shadowsoffire.apotheosis.ench.asm.EnchHooks");
                            apothMax = MethodHandles.publicLookup().findStatic(hooks, "getMaxLevel",
                                    MethodType.methodType(int.class, Enchantment.class));
                            WaifingAttributesMod.LOGGER.info("Apotheosis detected: using its enchantment max levels.");
                        }
                    } catch (Throwable t) {
                        WaifingAttributesMod.LOGGER.warn("Could not hook Apotheosis max levels, using vanilla ones: {}", t.toString());
                    }
                    resolved = true;
                }
            }
        }
        return apothMax;
    }

    private EnchantLevels() {}
}
