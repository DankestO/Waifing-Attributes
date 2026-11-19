package com.waifing.waifingattributes.upgrade;

import com.waifing.waifingattributes.ModAttributes;
import com.waifing.waifingattributes.WaifingAttributesMod;
import com.waifing.waifingattributes.compat.ApotheosisSockets;
import com.waifing.waifingattributes.compat.IronsScrolls;
import com.waifing.waifingattributes.config.WaifingConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraftforge.event.ItemStackedOnOtherEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;

/**
 * Mechanic: pick an item up with the cursor and RIGHT-click another one in the inventory (any slot, armor and shield
 * slots included). What happens depends on the pair: a brewing ingredient on a potion, a catalyst on a potion, book or
 * piece of equipment, a potion on a weapon (imbue), a book on a book or on equipment, a scroll on a scroll...
 *
 * Purely event-driven: it only runs when the player clicks with an item on the cursor, never on a tick. The server
 * decides; the client only cancels the click so the vanilla item swap is not predicted.
 */
@Mod.EventBusSubscriber(modid = WaifingAttributesMod.MODID)
public final class UpgradeClickHandler {
    public enum Kind {
        POTION, IMBUE, IMBUE_FOOD, BOOK_REBIRTH, MERGE_BOOK, SPLIT_BOOK, APPLY_BOOK, EXTRACT_BOOK, COPY_BOOK, SCROLL_MERGE, SCROLL_SPLIT, ARCANE, EQUIPMENT, REPAIR, SOCKET, IMBUE_ARROW
    }

    /** What a click would do and the attribute that unlocks exactly this action (each ability has its own attribute). */
    public record Plan(Kind kind, Attribute attribute) {
        void run(UpgradeContext ctx) {
            switch (kind) {
                case POTION -> PotionUpgrader.run(ctx);
                case IMBUE -> PotionImbue.run(ctx);
                case IMBUE_FOOD -> PotionImbue.runFood(ctx);
                case BOOK_REBIRTH -> BookUpgrader.run(ctx);
                case MERGE_BOOK -> BookTools.merge(ctx);
                case SPLIT_BOOK -> BookTools.split(ctx);
                case APPLY_BOOK -> BookTools.apply(ctx);
                case EXTRACT_BOOK -> BookTools.extract(ctx);
                case COPY_BOOK -> BookTools.copyWritten(ctx);
                case SCROLL_MERGE -> IronsScrolls.merge(ctx);
                case SCROLL_SPLIT -> IronsScrolls.split(ctx);
                case ARCANE -> IronsScrolls.arcane(ctx);
                case EQUIPMENT -> EquipmentUpgrader.run(ctx);
                case REPAIR -> EquipmentRepair.run(ctx);
                case IMBUE_ARROW -> PotionImbue.runArrows(ctx);
                case SOCKET -> ApotheosisSockets.run(ctx);
            }
        }
    }

    private static Plan plan(Kind k, RegistryObject<Attribute> attribute) {
        return new Plan(k, attribute.get());
    }

    /** What would happen if {@code catalyst} were used on {@code target}. Cheap and side-effect free; null = nothing. */
    @Nullable
    public static Plan resolve(ItemStack target, ItemStack catalyst) {
        if (target.isEmpty() || catalyst.isEmpty()) return null;

        // blank book and quill onto a book with text (signed or not): copy it
        if (BookTools.canCopyWritten(target, catalyst)) return plan(Kind.COPY_BOOK, ModAttributes.ENCHANTER_EXTRACT_BOOK);

        if (target.getItem() instanceof PotionItem) { // potions may be stacked: one ingredient, one roll for the whole stack
            if (PotionBrewer.canBrew(target, catalyst)) return plan(Kind.POTION, ModAttributes.ALCHEMIST_BREWING);
            boolean cat = WaifingConfig.isPotionCatalyst(catalyst);
            boolean reb = WaifingConfig.isPotionRebirthListed(catalyst);
            if (!cat && !reb) return null;
            boolean complete = PotionData.read(target).cycle >= PotionUpgrader.steps(PotionUpgrader.isTimed(target));
            return plan(Kind.POTION, reb && (complete || !cat) ? ModAttributes.ALCHEMIST_POTION_REBIRTH : ModAttributes.ALCHEMIST_POTION_UPGRADE);
        }
        // Food may be stacked: only ONE item is imbued per potion (1:1).
        if (catalyst.getItem() instanceof PotionItem && PotionImbue.isImbuableFood(target)) {
            return plan(Kind.IMBUE_FOOD, ModAttributes.CHEF_IMBUE_FOOD);
        }
        // Arrows may be stacked: the whole stack becomes arrows with the potion's effects.
        if (catalyst.getItem() instanceof PotionItem && WaifingConfig.isImbuableArrow(target)) {
            return plan(Kind.IMBUE_ARROW, ModAttributes.BLACKSMITH_IMBUE_WEAPON_HITS);
        }
        if (target.getCount() != 1) return null;

        if (catalyst.getItem() instanceof PotionItem && WaifingConfig.isImbueTarget(target)) {
            return plan(Kind.IMBUE, ModAttributes.BLACKSMITH_IMBUE_WEAPON_HITS);
        }

        // Apotheosis gem on a piece of equipment: the Blacksmith can socket it even without sockets (blacksmith_gem_socket)
        if (ApotheosisSockets.isGem(catalyst) && UpgradeCategory.detect(target) != null) {
            return plan(Kind.SOCKET, ModAttributes.BLACKSMITH_GEM_SOCKET);
        }

        boolean catalystIsBook = catalyst.is(Items.ENCHANTED_BOOK);
        if (target.is(Items.ENCHANTED_BOOK)) {
            if (WaifingConfig.isBookRebirthListed(catalyst)) return plan(Kind.BOOK_REBIRTH, ModAttributes.ENCHANTER_BOOK_REBIRTH);
            if (catalystIsBook) return plan(Kind.MERGE_BOOK, ModAttributes.ENCHANTER_MERGE_BOOKS);
            if (WaifingConfig.isSplitItem(catalyst)) return plan(Kind.SPLIT_BOOK, ModAttributes.ENCHANTER_SPLIT_BOOKS);
            return null;
        }
        if (catalystIsBook) {
            return BookTools.canReceive(target) ? plan(Kind.APPLY_BOOK, ModAttributes.ENCHANTER_APPLY_BOOK) : null;
        }
        // normal book onto an enchanted item: extract one enchantment into a book
        if (target.isEnchanted() && WaifingConfig.isSplitItem(catalyst)) return plan(Kind.EXTRACT_BOOK, ModAttributes.ENCHANTER_EXTRACT_BOOK);
        if (IronsScrolls.isScroll(target)) {
            if (IronsScrolls.isScroll(catalyst)) return plan(Kind.SCROLL_MERGE, ModAttributes.MAGE_SCROLL_MERGE);
            if (WaifingConfig.isScrollSplitItem(catalyst)) return plan(Kind.SCROLL_SPLIT, ModAttributes.MAGE_SCROLL_SPLIT);
            return null;
        }
        if (IronsScrolls.isScroll(catalyst)) return plan(Kind.ARCANE, ModAttributes.MAGE_ARCANE_ANVIL); // scroll onto any item: Arcane Anvil

        UpgradeCategory cat = UpgradeCategory.detect(target);
        // repair catalyst on damaged equipment (an undamaged item falls through: the same item may also be an upgrade catalyst)
        if (cat != null && target.isDamageableItem() && target.getDamageValue() > 0 && WaifingConfig.repairFraction(catalyst) > 0.0D) {
            return plan(Kind.REPAIR, ModAttributes.BLACKSMITH_UPGRADE);
        }
        if (cat != null && (WaifingConfig.equipmentCatalyst(catalyst, cat) != null
                || WaifingConfig.isEquipRebirthListed(catalyst))) {
            boolean complete = UpgradeData.level(target) >= ModAttributes.MAX_UPGRADE_LEVELS;
            return plan(Kind.EQUIPMENT, complete ? ModAttributes.BLACKSMITH_REBIRTH : ModAttributes.BLACKSMITH_UPGRADE);
        }
        return null;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onStackedOn(ItemStackedOnOtherEvent e) {
        if (e.getClickAction() != ClickAction.SECONDARY) return; // right click only: left click keeps swapping items
        Player player = e.getPlayer();
        boolean server = player instanceof ServerPlayer;
        // Creative's inventory screen is handled client-side only, so the server would never see the click.
        if (!server && player.getAbilities().instabuild) return;

        // Read the real state from the open menu instead of trusting the event's argument names:
        // the clicked slot holds the item to upgrade, the cursor holds the catalyst.
        Slot slot = e.getSlot();
        ItemStack target = slot.getItem();
        ItemStack catalyst = player.containerMenu.getCarried();
        // Result/output slots (crafting, furnace, anvil...) must never be upgraded.
        if (!slot.mayPlace(target) || !slot.mayPickup(player)) return;

        Plan plan = resolve(target, catalyst);
        if (plan == null) return;

        // Without the matching mastery the mod ignores the click completely (vanilla item swap, no sound, no text).
        if (player.getAttributeValue(plan.attribute()) <= 0.0D) return;

        e.setCanceled(true);
        if (!server) return;

        plan.run(new UpgradeContext((ServerPlayer) player, slot));
        // The client predicted nothing, but make sure slots and cursor match after the attempt.
        player.containerMenu.sendAllDataToRemote();
    }

    private UpgradeClickHandler() {}
}
