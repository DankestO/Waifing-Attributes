package com.waifing.waifingattributes.compat;

import com.waifing.waifingattributes.WaifingAttributesMod;
import net.minecraft.world.item.Item;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;

/**
 * Optional Tinkers' Construct support, reached ONLY by reflection (no compile-time dependency).
 * Builds a copy of a tool's StatsNBT whose durability stat is raised by the Waifing bonus.
 */
public final class TinkersStats {
    private static final String TC_ITEM_PACKAGE = "slimeknights.tconstruct.library.tools.item.";

    private static boolean initDone;
    private static boolean usable;
    private static Method builderM, setM, buildM, getM, containedM;
    private static Object durabilityStat;

    private static final ClassValue<Boolean> TINKERS_ITEM = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            for (Class<?> c = type; c != null; c = c.getSuperclass()) {
                if (c.getName().startsWith(TC_ITEM_PACKAGE)) return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
    };

    /** True for Tinkers' tools/armor: their durability is handled through ToolStack, not ItemStack#getMaxDamage. */
    public static boolean isTinkersItem(Item item) {
        return TINKERS_ITEM.get(item.getClass());
    }

    /** Returns a StatsNBT equal to {@code base} but with durability + bonus (or {@code base} itself if anything fails). */
    public static synchronized Object withDurabilityBonus(Object base, int bonus) {
        if (!init()) return base;
        try {
            Object builder = builderM.invoke(null);
            boolean hasDurability = false;
            for (Object stat : new ArrayList<>((Set<?>) containedM.invoke(base))) {
                Object value = getM.invoke(base, stat);
                if (stat == durabilityStat) {
                    value = ((Number) value).floatValue() + bonus;
                    hasDurability = true;
                }
                setM.invoke(builder, stat, value);
            }
            if (!hasDurability) return base;
            return buildM.invoke(builder);
        } catch (Throwable t) {
            WaifingAttributesMod.LOGGER.warn("Could not apply the durability bonus to a Tinkers' tool: {}", t.toString());
            return base;
        }
    }

    private static boolean init() {
        if (initDone) return usable;
        initDone = true;
        try {
            Class<?> stats = Class.forName("slimeknights.tconstruct.library.tools.nbt.StatsNBT");
            Class<?> builder = Class.forName("slimeknights.tconstruct.library.tools.nbt.StatsNBT$Builder");
            Class<?> stat = Class.forName("slimeknights.tconstruct.library.tools.stat.IToolStat");
            Class<?> toolStats = Class.forName("slimeknights.tconstruct.library.tools.stat.ToolStats");
            builderM = stats.getMethod("builder");
            containedM = stats.getMethod("getContainedStats");
            getM = stats.getMethod("get", stat);
            setM = builder.getMethod("set", stat, Object.class);
            buildM = builder.getMethod("build");
            Field f = toolStats.getField("DURABILITY");
            durabilityStat = f.get(null);
            usable = true;
        } catch (Throwable t) {
            WaifingAttributesMod.LOGGER.warn("Tinkers' Construct stats API not found, durability bonus disabled for its tools: {}", t.toString());
        }
        return usable;
    }

    private TinkersStats() {}
}
