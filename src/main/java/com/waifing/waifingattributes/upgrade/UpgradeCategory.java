package com.waifing.waifingattributes.upgrade;

import com.waifing.waifingattributes.config.WaifingConfig;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TridentItem;
import net.minecraftforge.common.ToolActions;

import javax.annotation.Nullable;

public enum UpgradeCategory {
    MELEE, RANGED, ARMOR,
    /** Solo valido dentro de la config: aplica a cualquier categoria. */
    ALL;

    /** Categoria real de un objeto, o null si no es mejorable. */
    @Nullable
    public static UpgradeCategory detect(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (WaifingConfig.matchesAny(WaifingConfig.forceArmor(), stack)) return ARMOR;
        if (WaifingConfig.matchesAny(WaifingConfig.forceRanged(), stack)) return RANGED;
        if (WaifingConfig.matchesAny(WaifingConfig.forceMelee(), stack)) return MELEE;
        if (stack.getItem() instanceof ArmorItem || isShield(stack)) return ARMOR; // shields behave like armor
        if (stack.getItem() instanceof ProjectileWeaponItem || stack.getItem() instanceof TridentItem) return RANGED;
        // Item.getAttributeModifiers(slot, stack) NO dispara el evento de atributos (no hay recursion).
        if (stack.getItem().getAttributeModifiers(EquipmentSlot.MAINHAND, stack).containsKey(Attributes.ATTACK_DAMAGE)) {
            return MELEE; // espadas, hachas, herramientas, bastones con dano de ataque...
        }
        return null;
    }

    private static final EquipmentSlot[] MAIN = {EquipmentSlot.MAINHAND};
    private static final EquipmentSlot[] HANDS = {EquipmentSlot.OFFHAND, EquipmentSlot.MAINHAND};

    public static boolean isShield(ItemStack stack) {
        return stack.getItem() instanceof ShieldItem || stack.canPerformAction(ToolActions.SHIELD_BLOCK);
    }

    /** Ranuras donde actuan los modificadores del objeto (un escudo sirve tanto en la mano secundaria como en la principal). */
    public static EquipmentSlot[] slotsOf(ItemStack stack, UpgradeCategory cat) {
        if (cat != ARMOR) return MAIN;
        if (stack.getItem() instanceof ArmorItem armor) return new EquipmentSlot[]{armor.getEquipmentSlot()};
        if (isShield(stack)) return HANDS;
        EquipmentSlot s = stack.getEquipmentSlot();
        return new EquipmentSlot[]{s != null ? s : EquipmentSlot.CHEST};
    }
}
