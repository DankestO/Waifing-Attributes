package com.waifing.waifingattributes.upgrade;

import com.waifing.waifingattributes.config.CatalystDef;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Todo lo mejorado se guarda en el NBT del objeto (tag "WaifingUpgrades"), ya calculado, para que
 * el objeto funcione igual aunque la config cambie o el cliente tenga otra config.
 *
 * WaifingUpgrades: { Level, Total, Rebirths, Bonus, Counts:{clave:n}, Mods:[{Key,Attr,Op,Slot,Amount}] }
 */
public final class UpgradeData {
    public static final String ROOT = "WaifingUpgrades";

    public static CompoundTag read(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(ROOT, Tag.TAG_COMPOUND)) return new CompoundTag();
        return tag.getCompound(ROOT);
    }

    public static boolean has(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(ROOT, Tag.TAG_COMPOUND);
    }

    public static int level(ItemStack s) { return read(s).getInt("Level"); }
    public static int rebirths(ItemStack s) { return read(s).getInt("Rebirths"); }
    public static int bonusDurability(ItemStack s) { return has(s) ? read(s).getInt("Bonus") : 0; }

    /** Same as {@link #bonusDurability(ItemStack)} but straight from an item's root NBT (used by the Tinkers mixin). */
    public static int bonusDurability(@javax.annotation.Nullable CompoundTag stackTag) {
        if (stackTag == null || !stackTag.contains(ROOT, Tag.TAG_COMPOUND)) return 0;
        return stackTag.getCompound(ROOT).getInt("Bonus");
    }

    /** Aplica un exito de mejora: +1 nivel y acumula todos los efectos del catalizador para la categoria. */
    public static void apply(ItemStack stack, CatalystDef def, UpgradeCategory cat, EquipmentSlot... slots) {
        CompoundTag root = stack.getOrCreateTagElement(ROOT);
        root.putInt("Level", root.getInt("Level") + 1);
        root.putInt("Total", root.getInt("Total") + 1);

        CompoundTag counts = root.getCompound("Counts");
        counts.putInt(def.key(), counts.getInt(def.key()) + 1);
        root.put("Counts", counts);

        ListTag mods = root.getList("Mods", Tag.TAG_COMPOUND);
        for (CatalystDef.Effect e : def.effectsFor(cat)) {
            if (e.durability()) {
                root.putInt("Bonus", root.getInt("Bonus") + (int) Math.round(e.amount()));
                continue;
            }
            String attr = e.attribute().toString();
            int op = e.operation().toValue();
            for (EquipmentSlot slot : slots) {
                addModifier(mods, def.key(), attr, op, slot.getName(), e.amount());
            }
        }
        root.put("Mods", mods);
    }

    private static void addModifier(ListTag mods, String key, String attr, int op, String slotName, double amount) {
        CompoundTag found = null;
        for (int i = 0; i < mods.size(); i++) {
            CompoundTag c = mods.getCompound(i);
            if (c.getString("Key").equals(key) && c.getString("Attr").equals(attr)
                    && c.getInt("Op") == op && c.getString("Slot").equals(slotName)) {
                found = c;
                break;
            }
        }
        if (found == null) {
            found = new CompoundTag();
            found.putString("Key", key);
            found.putString("Attr", attr);
            found.putInt("Op", op);
            found.putString("Slot", slotName);
            found.putDouble("Amount", 0.0D);
            mods.add(found);
        }
        found.putDouble("Amount", found.getDouble("Amount") + amount);
    }

    /** Rebirth: reinicia los slots (Level=0) conservando todos los bonus acumulados. */
    public static void rebirth(ItemStack stack) {
        CompoundTag root = stack.getOrCreateTagElement(ROOT);
        root.putInt("Level", 0);
        root.putInt("Rebirths", root.getInt("Rebirths") + 1);
    }

    private UpgradeData() {}
}
