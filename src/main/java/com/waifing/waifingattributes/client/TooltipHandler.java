package com.waifing.waifingattributes.client;

import com.waifing.waifingattributes.WaifingAttributesMod;
import com.waifing.waifingattributes.config.WaifingClientConfig;
import com.waifing.waifingattributes.ModAttributes;
import com.waifing.waifingattributes.config.CatalystDef;
import com.waifing.waifingattributes.config.WaifingConfig;
import com.waifing.waifingattributes.upgrade.UpgradeCategory;
import net.minecraft.network.chat.MutableComponent;
import com.waifing.waifingattributes.upgrade.PotionData;
import com.waifing.waifingattributes.upgrade.PotionImbue;
import com.waifing.waifingattributes.upgrade.PotionUpgrader;
import com.waifing.waifingattributes.upgrade.UpgradeData;
import com.waifing.waifingattributes.compat.ApotheosisSockets;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = WaifingAttributesMod.MODID, value = Dist.CLIENT)
public final class TooltipHandler {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> tip = event.getToolTip();
        try {
            if (WaifingClientConfig.SHOW_CATALYST_HINTS.get()) addCatalystHint(stack, tip);
            if (!stack.hasTag() || !WaifingClientConfig.SHOW_UPGRADE_TOOLTIPS.get()) return;
        } catch (IllegalStateException notLoaded) {
            return; // the SERVER config is only available inside a world
        }

        if (UpgradeData.has(stack)) {
            CompoundTag root = UpgradeData.read(stack);
            nameSuffix(tip, root.getInt("Level"), root.getInt("Rebirths"), false);
            int bonus = root.getInt("Bonus");
            if (bonus > 0) {
                tip.add(Component.translatable("tooltip.waifingattributes.durability", bonus).withStyle(ChatFormatting.GRAY));
            }
        }

        if (PotionData.has(stack)) {
            PotionData d = PotionData.read(stack);
            nameSuffix(tip, d.cycle, d.rebirths, !PotionUpgrader.isTimed(stack));
            if (d.total > 0) {
                long pct = Math.round(d.total * WaifingConfig.POTION_DURATION_STEP.get() * 100.0D);
                tip.add(Component.translatable("tooltip.waifingattributes.potion_duration", pct).withStyle(ChatFormatting.GRAY));
            }
        }

        // Arrows imbued by the Blacksmith carry only custom effects (no vanilla potion), so vanilla would call them "Uncraftable".
        if (stack.is(net.minecraft.world.item.Items.TIPPED_ARROW) && !tip.isEmpty()
                && net.minecraft.world.item.alchemy.PotionUtils.getPotion(stack) == net.minecraft.world.item.alchemy.Potions.EMPTY) {
            List<MobEffectInstance> fx = net.minecraft.world.item.alchemy.PotionUtils.getCustomEffects(stack);
            if (!fx.isEmpty()) tip.set(0, Component.translatable("tooltip.waifingattributes.imbued_arrow", fx.get(0).getEffect().getDisplayName())
                    .withStyle(tip.get(0).getStyle()));
        }

        if (PotionImbue.hasFood(stack)) {
            tip.add(Component.translatable("tooltip.waifingattributes.imbue_food").withStyle(ChatFormatting.DARK_AQUA));
            for (MobEffectInstance e : PotionImbue.foodEffects(stack)) {
                tip.add(Component.literal(" ").append(e.getEffect().getDisplayName()).withStyle(ChatFormatting.DARK_GRAY));
            }
        }

        if (PotionImbue.has(stack)) {
            tip.add(Component.translatable("tooltip.waifingattributes.imbue", PotionImbue.hits(stack)).withStyle(ChatFormatting.DARK_AQUA));
            for (MobEffectInstance e : PotionImbue.effects(stack)) {
                tip.add(Component.literal(" ").append(e.getEffect().getDisplayName()).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }

    // ------------------------------------------------------------------ upgrade level (RPG style)
    /**
     * "Diamond Sword +4 ✦✦": the upgrade level in gold after the name and one purple star per Rebirth (more than 5 rebirths:
     * "✦×7"). Nothing is added while the item has neither. The name is the first tooltip line.
     * Instant potions have no "+N": their only upgrade is one of potency, shown as a red "▲" so it is not mistaken for the "+N"
     * duration upgrades of the other potions.
     */
    private static void nameSuffix(List<Component> tip, int level, int rebirths, boolean potency) {
        if (tip.isEmpty() || (level <= 0 && rebirths <= 0)) return;
        MutableComponent name = tip.get(0).copy();
        if (level > 0 && potency) name.append(Component.literal(" " + "\u25B2".repeat(level)).withStyle(ChatFormatting.RED));
        else if (level > 0) name.append(Component.literal(" +" + level).withStyle(ChatFormatting.GOLD));
        if (rebirths > 0) {
            String stars = rebirths <= 5 ? "\u2726".repeat(rebirths) : "\u2726\u00D7" + rebirths;
            name.append(Component.literal(" " + stars).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        tip.set(0, name);
    }

    // ------------------------------------------------------------------ catalyst hints
    /** "Catalyst: <masteries>" with each mastery in its colour, then one line per thing the item does. */
    private static void addCatalystHint(ItemStack stack, List<Component> tip) {
        if (stack.isEmpty()) return;
        List<String> who = new ArrayList<>();
        List<Component> details = new ArrayList<>();

        boolean potion = WaifingConfig.isPotionCatalyst(stack);
        boolean potionRebirth = WaifingConfig.isPotionRebirthListed(stack);
        if (potion || potionRebirth) who.add("alchemist");
        if (potion) details.add(Component.translatable("tooltip.waifingattributes.catalyst.potion",
                number(WaifingConfig.POTION_DURATION_STEP.get() * 100.0D)));
        if (potionRebirth) details.add(Component.translatable("tooltip.waifingattributes.catalyst.potion_rebirth"));

        CatalystDef def = null;
        for (UpgradeCategory c : UpgradeCategory.values()) {
            if (def == null && c != UpgradeCategory.ALL) def = WaifingConfig.equipmentCatalyst(stack, c);
        }
        boolean equipRebirth = WaifingConfig.isEquipRebirthListed(stack);
        double repair = WaifingConfig.repairFraction(stack);
        if (def != null || equipRebirth || repair > 0.0D) who.add("blacksmith");
        if (def != null) {
            for (CatalystDef.Effect e : def.effects()) {
                Component what = e.durability()
                        ? Component.translatable("tooltip.waifingattributes.catalyst.durability", number(e.amount()))
                        : effect(e);
                if (what == null) continue;
                details.add(Component.translatable("tooltip.waifingattributes.catalyst.equip",
                        Component.translatable("tooltip.waifingattributes.category." + e.category().name().toLowerCase(java.util.Locale.ROOT)), what));
            }
        }
        if (repair > 0.0D) details.add(Component.translatable("tooltip.waifingattributes.catalyst.repair", number(repair * 100.0D)));
        if (equipRebirth) details.add(Component.translatable("tooltip.waifingattributes.catalyst.equip_rebirth"));

        boolean bookRebirth = WaifingConfig.isBookRebirthListed(stack);
        boolean split = WaifingConfig.isSplitItem(stack);
        if (bookRebirth || split) who.add("enchanter");
        if (bookRebirth) details.add(Component.translatable("tooltip.waifingattributes.catalyst.book_rebirth"));
        if (split) details.add(Component.translatable("tooltip.waifingattributes.catalyst.split_book"));

        if (WaifingConfig.isScrollSplitItem(stack)) {
            who.add("mage");
            details.add(Component.translatable("tooltip.waifingattributes.catalyst.scroll_split"));
        }
        if (who.isEmpty()) return;

        MutableComponent names = Component.empty();
        for (int i = 0; i < who.size(); i++) {
            if (i > 0) names.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
            names.append(Component.translatable("tooltip.waifingattributes.group." + who.get(i)).withStyle(groupColor(who.get(i))));
        }
        tip.add(Component.translatable("tooltip.waifingattributes.hint", names).withStyle(ChatFormatting.GRAY));
        for (Component d : details) tip.add(Component.literal("  ").append(d).withStyle(ChatFormatting.DARK_GRAY));
    }

    private static ChatFormatting groupColor(String group) {
        return switch (group) {
            case "alchemist" -> ChatFormatting.LIGHT_PURPLE;
            case "blacksmith" -> ChatFormatting.GOLD;
            case "enchanter" -> ChatFormatting.AQUA;
            case "mage" -> ChatFormatting.BLUE;
            default -> ChatFormatting.GRAY;
        };
    }

    /** "+0.5 Attack Damage" or "+5% Attack Speed", or null if the attribute does not exist. */
    private static Component effect(CatalystDef.Effect e) {
        if (e.attribute() == null) return null;
        net.minecraft.world.entity.ai.attributes.Attribute a = net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES.getValue(e.attribute());
        if (a == null) return null;
        String amount = e.operation() == net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION
                ? number(e.amount()) : number(e.amount() * 100.0D) + "%";
        return Component.literal((e.amount() >= 0 ? "+" : "") + amount + " ").append(Component.translatable(a.getDescriptionId()));
    }

    private static String number(double v) {
        if (Math.abs(v - Math.rint(v)) < 1.0E-9) return String.valueOf((long) Math.rint(v));
        return java.math.BigDecimal.valueOf(v).setScale(3, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private TooltipHandler() {}
}
