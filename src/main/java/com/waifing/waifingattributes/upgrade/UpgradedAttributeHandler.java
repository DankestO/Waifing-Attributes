package com.waifing.waifingattributes.upgrade;

import com.waifing.waifingattributes.WaifingAttributesMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Inyecta en cada objeto mejorado los modificadores guardados en su NBT. */
@Mod.EventBusSubscriber(modid = WaifingAttributesMod.MODID)
public final class UpgradedAttributeHandler {

    @SubscribeEvent
    public static void onModifiers(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        if (!UpgradeData.has(stack)) return;
        ListTag mods = UpgradeData.read(stack).getList("Mods", Tag.TAG_COMPOUND);
        String slotName = event.getSlotType().getName();
        for (int i = 0; i < mods.size(); i++) {
            CompoundTag c = mods.getCompound(i);
            if (!c.getString("Slot").equals(slotName)) continue;
            ResourceLocation rl = ResourceLocation.tryParse(c.getString("Attr"));
            if (rl == null) continue;
            Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(rl);
            if (attr == null) continue; // el mod dueno del atributo no esta instalado
            double amount = c.getDouble("Amount");
            if (amount == 0.0D) continue;
            // UUID estable y unico por (material, atributo, operacion, ranura): evita colisiones entre piezas de armadura
            UUID id = UUID.nameUUIDFromBytes((UpgradeData.ROOT + "|" + c.getString("Key") + "|" + rl + "|"
                    + c.getInt("Op") + "|" + slotName).getBytes(StandardCharsets.UTF_8));
            event.addModifier(attr, new AttributeModifier(id, "Waifing upgrade", amount,
                    AttributeModifier.Operation.fromValue(c.getInt("Op"))));
        }
    }

    private UpgradedAttributeHandler() {}
}
