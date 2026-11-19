package com.waifing.waifingattributes.compat;

import com.waifing.waifingattributes.WaifingAttributesMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

/**
 * Quality Food (OPTIONAL dependency, not compiled against: everything goes through reflection).
 * Hunter raises the quality level of the food dropped by the mobs it kills, on top of whatever quality Quality Food
 * gave it (none, iron, gold, diamond = 0..3). Items Quality Food does not consider food are never touched.
 */
public final class QualityBoost {
    private static final String MOD = "quality_food";
    private static final String TAG = "quality_food"; // the quality lives in stack.tag.quality_food.quality

    private static Boolean loaded;
    private static boolean failed;
    private static Method qualityGet;
    private static Method apply;

    public static boolean present() {
        if (loaded == null) {
            ModList list = ModList.get();
            if (list == null) return false;
            loaded = list.isLoaded(MOD);
        }
        return loaded && !failed;
    }

    private static boolean init() {
        if (apply != null) return true;
        try {
            Class<?> quality = Class.forName("de.cadentem.quality_food.core.Quality");
            Class<?> utils = Class.forName("de.cadentem.quality_food.util.QualityUtils");
            qualityGet = quality.getMethod("get", int.class);
            apply = utils.getMethod("applyQuality", ItemStack.class, quality, boolean.class);
            return true;
        } catch (Throwable t) {
            failed = true;
            WaifingAttributesMod.LOGGER.warn("Could not hook Quality Food, quality bonus disabled: {}", t.toString());
            return false;
        }
    }

    public static int level(ItemStack stack) {
        CompoundTag t = stack.getTagElement(TAG);
        return t == null ? 0 : t.getInt("quality");
    }

    /** Quality Food's own method (validates the item, adds its effects); {@code true} = never lowers an existing quality. */
    private static void raise(ItemStack stack, int toLevel) {
        try {
            apply.invoke(null, stack, qualityGet.invoke(null, toLevel), true);
        } catch (Throwable ignored) {
            // leave the stack as it is
        }
    }

    /**
     * Raises the quality of {@code stack} (every unit in it) by {@code levels}, never above diamond. Quality Food decides
     * whether the item is food at all: anything else is left untouched.
     */
    public static void raiseBy(ItemStack stack, int levels) {
        if (levels <= 0 || stack.isEmpty() || !present() || !init()) return;
        int cur = level(stack);
        if (cur >= 3) return;
        raise(stack, Math.min(3, cur + levels));
    }

    private QualityBoost() {}
}
