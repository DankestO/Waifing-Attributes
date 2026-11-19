package com.waifing.waifingattributes.compat;

import com.waifing.waifingattributes.WaifingAttributesMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * blacksmith_gem_quality (PERCENT, max 1.0): the attribute bonuses of a gem the Blacksmith socketed are multiplied by (1 + quality).
 *
 * The gem itself is never touched. The quality lives on the ITEM, tied to the gem's own id:
 *   WaifingGemQuality: [ { gem: [I; ...first uuid of the gem...], q: 0.6f }, ... ]
 * so a gem taken out and socketed somewhere else carries nothing, and the value is SET when socketing (never added up), so socketing
 * again cannot stack it.
 *
 * Apotheosis adds the attribute bonuses of the gems through the public ItemAttributeModifierEvent, each modifier using one of the uuids
 * stored in the gem. This listener runs after Apotheosis (lowest priority), finds the modifiers whose uuid belongs to a boosted gem and
 * replaces them with the scaled amount. No mixin. Items without the tag leave after a single tag lookup.
 * Only attribute bonuses are scaled: enchantment, potion, durability and damage reduction bonuses of gems are not.
 */
@Mod.EventBusSubscriber(modid = WaifingAttributesMod.MODID)
public final class GemQuality {
    public static final String KEY = "WaifingGemQuality";
    private static final String AFFIX_DATA = "affix_data";
    private static final String GEMS = "gems";
    private static final String UUIDS = "uuids";

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onModifiers(ItemAttributeModifierEvent event) {
        CompoundTag root = event.getItemStack().getTag();
        if (root == null || !root.contains(KEY, Tag.TAG_LIST)) return;
        ListTag qualities = root.getList(KEY, Tag.TAG_COMPOUND);
        if (qualities.isEmpty()) return;

        Map<UUID, Float> byGem = new HashMap<>();
        for (int i = 0; i < qualities.size(); i++) {
            CompoundTag e = qualities.getCompound(i);
            if (e.contains("gem", Tag.TAG_INT_ARRAY)) byGem.put(NbtUtils.loadUUID(e.get("gem")), e.getFloat("q"));
        }
        // uuid of every modifier of a boosted gem -> its quality
        Map<UUID, Float> byModifier = new HashMap<>();
        ListTag gems = root.getCompound(AFFIX_DATA).getList(GEMS, Tag.TAG_COMPOUND);
        for (int i = 0; i < gems.size(); i++) {
            ListTag ids = gems.getCompound(i).getCompound("tag").getList(UUIDS, Tag.TAG_INT_ARRAY);
            if (ids.isEmpty()) continue;
            Float q = byGem.get(NbtUtils.loadUUID(ids.get(0)));
            if (q == null || q <= 0.0F) continue;
            for (Tag id : ids) byModifier.put(NbtUtils.loadUUID(id), q);
        }
        if (byModifier.isEmpty()) return;

        for (Map.Entry<Attribute, AttributeModifier> me : new ArrayList<>(event.getModifiers().entries())) {
            AttributeModifier m = me.getValue();
            Float q = byModifier.get(m.getId());
            if (q == null) continue;
            event.removeModifier(me.getKey(), m);
            event.addModifier(me.getKey(), new AttributeModifier(m.getId(), m.getName(), m.getAmount() * (1.0D + q), m.getOperation()));
        }
    }

    /** Sets the quality of a gem on the item (replacing any previous value for that gem) and forgets gems that are no longer socketed. */
    public static void record(ItemStack item, UUID gemId, float quality) {
        CompoundTag root = item.getOrCreateTag();
        Set<UUID> alive = new HashSet<>();
        ListTag gems = root.getCompound(AFFIX_DATA).getList(GEMS, Tag.TAG_COMPOUND);
        for (int i = 0; i < gems.size(); i++) {
            ListTag ids = gems.getCompound(i).getCompound("tag").getList(UUIDS, Tag.TAG_INT_ARRAY);
            if (!ids.isEmpty()) alive.add(NbtUtils.loadUUID(ids.get(0)));
        }
        ListTag next = new ListTag();
        ListTag old = root.getList(KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < old.size(); i++) {
            CompoundTag e = old.getCompound(i);
            if (!e.contains("gem", Tag.TAG_INT_ARRAY)) continue;
            UUID id = NbtUtils.loadUUID(e.get("gem"));
            if (alive.contains(id) && !id.equals(gemId)) next.add(e);
        }
        if (quality > 0.0F) {
            CompoundTag add = new CompoundTag();
            add.put("gem", NbtUtils.createUUID(gemId));
            add.putFloat("q", quality);
            next.add(add);
        }
        if (next.isEmpty()) root.remove(KEY);
        else root.put(KEY, next);
    }

    private GemQuality() {}
}
