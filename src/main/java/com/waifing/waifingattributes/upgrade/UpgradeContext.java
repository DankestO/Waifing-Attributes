package com.waifing.waifingattributes.upgrade;

import com.waifing.waifingattributes.util.ChanceUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * One upgrade attempt: the item being upgraded (it sits in {@link #slot}, the slot that was clicked) and the catalyst
 * the player holds on the cursor. Both are read straight from the open container menu, so it never depends on how
 * an event names its arguments. Every upgrader (potion, book, equipment) works only through this object.
 */
public final class UpgradeContext {
    private final ServerPlayer player;
    private final Slot slot;

    public UpgradeContext(ServerPlayer player, Slot slot) {
        this.player = player;
        this.slot = slot;
    }

    public ServerPlayer player() { return player; }

    /** The stack being upgraded (the clicked slot). Modify it in place. */
    public ItemStack target() { return slot.getItem(); }

    /** The catalyst stack held on the cursor. */
    public ItemStack catalyst() { return player.containerMenu.getCarried(); }

    /** Spends one catalyst (creative players keep it). */
    public void consumeCatalyst() {
        if (player.getAbilities().instabuild) return;
        AbstractContainerMenu menu = player.containerMenu;
        ItemStack c = menu.getCarried();
        c.shrink(1);
        if (c.isEmpty()) menu.setCarried(ItemStack.EMPTY);
    }

    /** Spends one catalyst unless the keep-catalyst attribute (PERCENT) saves it. */
    public void consumeCatalyst(RegistryObject<Attribute> keep) {
        if (ChanceUtil.roll(player.getAttributeValue(keep.get()), player.getRandom())) return;
        consumeCatalyst();
    }

    /** Replaces the clicked stack (brewing and other conversions). */
    public void replaceTarget(ItemStack stack) {
        slot.set(stack);
    }

    /** Hands an item to the player: onto the cursor if it is empty, otherwise into the inventory (or dropped). */
    public void give(ItemStack stack) {
        if (stack.isEmpty()) return;
        AbstractContainerMenu menu = player.containerMenu;
        if (menu.getCarried().isEmpty()) menu.setCarried(stack);
        else player.getInventory().placeItemBackInInventory(stack);
    }

    /** Removes the item being upgraded (failed attempt). */
    public void destroyTarget() {
        slot.set(ItemStack.EMPTY);
    }
}
