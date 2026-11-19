package com.waifing.waifingattributes.config;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

/** Coincide con un item ("modid:item") o con una etiqueta ("#forge:ingots/steel"). */
public final class ItemMatcher {
    private final String key;
    @Nullable private final ResourceLocation id;
    @Nullable private final TagKey<Item> tag;

    private ItemMatcher(String key, @Nullable ResourceLocation id, @Nullable TagKey<Item> tag) {
        this.key = key;
        this.id = id;
        this.tag = tag;
    }

    @Nullable
    public static ItemMatcher parse(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;
        if (s.startsWith("#")) {
            ResourceLocation rl = ResourceLocation.tryParse(s.substring(1));
            return rl == null ? null : new ItemMatcher(s, null, TagKey.create(Registries.ITEM, rl));
        }
        ResourceLocation rl = ResourceLocation.tryParse(s);
        return rl == null ? null : new ItemMatcher(s, rl, null);
    }

    public boolean matches(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (tag != null) return stack.is(tag);
        return id != null && id.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }

    /** The items this matcher covers right now (a tag is expanded to its members). Client/JEI use only. */
    public java.util.List<Item> items() {
        java.util.List<Item> out = new java.util.ArrayList<>();
        if (id != null) {
            Item it = ForgeRegistries.ITEMS.getValue(id);
            if (it != null && ForgeRegistries.ITEMS.containsKey(id)) out.add(it);
        } else if (tag != null) {
            var tags = ForgeRegistries.ITEMS.tags();
            if (tags != null) tags.getTag(tag).forEach(out::add);
        }
        return out;
    }

    /** Clave estable usada para guardar contadores en el NBT del objeto mejorado. */
    public String key() {
        return key;
    }
}
