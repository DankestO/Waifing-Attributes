package com.waifing.waifingattributes.upgrade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * Upgrade state of a potion, stored in the item's NBT under {@code WaifingPotion}:
 * Origin (the vanilla potion it came from), Base (base duration of every effect, ticks), Total (duration levels
 * ever applied), Cycle (levels of the current cycle, reset by a Rebirth) and Rebirths.
 */
public final class PotionData {
    public static final String TAG = "WaifingPotion";

    public String origin = "minecraft:empty";
    @Nullable public int[] base;
    public int total;
    public int cycle;
    public int rebirths;
    public boolean touched;

    public static boolean has(ItemStack stack) {
        CompoundTag t = stack.getTag();
        return t != null && t.contains(TAG, 10);
    }

    public static PotionData read(ItemStack stack) {
        PotionData d = new PotionData();
        CompoundTag t = stack.getTag();
        if (t != null && t.contains(TAG, 10)) {
            CompoundTag c = t.getCompound(TAG);
            d.touched = true;
            d.origin = c.getString("Origin");
            if (c.contains("Base", 11)) d.base = c.getIntArray("Base");
            d.total = c.getInt("Total");
            d.cycle = c.getInt("Cycle");
            d.rebirths = c.getInt("Rebirths");
        } else {
            ResourceLocation key = ForgeRegistries.POTIONS.getKey(PotionUtils.getPotion(stack));
            d.origin = key == null ? "minecraft:empty" : key.toString();
        }
        return d;
    }

    /** Replaces the effects of the stack with {@code effects} (as custom effects, keeping the name) and saves this state. */
    public void store(ItemStack stack, Collection<MobEffectInstance> effects) {
        Component name = stack.getHoverName();
        boolean renamed = stack.hasCustomHoverName();
        stack.getOrCreateTag().remove("CustomPotionEffects");
        PotionUtils.setPotion(stack, Potions.EMPTY);
        PotionUtils.setCustomEffects(stack, effects);
        stack.getOrCreateTag().putInt("CustomPotionColor", PotionUtils.getColor(effects));
        if (!renamed) stack.setHoverName(name.copy().withStyle(s -> s.withItalic(false)));

        CompoundTag c = new CompoundTag();
        c.putString("Origin", origin);
        if (base != null) c.putIntArray("Base", base);
        c.putInt("Total", total);
        c.putInt("Cycle", cycle);
        c.putInt("Rebirths", rebirths);
        stack.getOrCreateTag().put(TAG, c);
    }
}
