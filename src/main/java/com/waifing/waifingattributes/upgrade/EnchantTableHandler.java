package com.waifing.waifingattributes.upgrade;

import com.waifing.waifingattributes.ModAttributes;
import com.waifing.waifingattributes.WaifingAttributesMod;
import com.waifing.waifingattributes.config.WaifingConfig;
import com.waifing.waifingattributes.util.ChanceUtil;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The enchanting table gets cheaper with enchanter. No mixins, no ticks:
 *  - Experience: the table pays through Forge's experience events (levels in vanilla, points in Apotheosis via Placebo).
 *    While an enchanting menu is open and the player is SPENDING experience, the amount is reduced.
 *  - Lapis: the same enchant action rolls a chance of not spending it; the lapis is refunded right after the click.
 * Two PERCENT attributes: enchanter_table_xp (experience discount) and enchanter_table_lapis (chance of keeping the lapis).
 */
@Mod.EventBusSubscriber(modid = WaifingAttributesMod.MODID)
public final class EnchantTableHandler {
    /** Last game tick in which an enchant action was already processed for a player (both events can fire in one click). */
    private static final Map<UUID, Long> LAST = new HashMap<>();

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onXp(PlayerXpEvent.XpChange e) {
        if (e.getAmount() >= 0 || !(e.getEntity() instanceof ServerPlayer p) || !(p.containerMenu instanceof EnchantmentMenu menu)) return;
        double d = discount(p, ModAttributes.ENCHANTER_TABLE_XP.get());
        if (d > 0.0D) e.setAmount(-ChanceUtil.scaleRandom(-e.getAmount(), 1.0D - d, p.getRandom()));
        lapis(p, menu);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLevels(PlayerXpEvent.LevelChange e) {
        if (e.getLevels() > 0 || !(e.getEntity() instanceof ServerPlayer p) || !(p.containerMenu instanceof EnchantmentMenu menu)) return;
        double d = discount(p, ModAttributes.ENCHANTER_TABLE_XP.get());
        if (d > 0.0D && e.getLevels() < 0) e.setLevels(-ChanceUtil.scaleRandom(-e.getLevels(), 1.0D - d, p.getRandom()));
        lapis(p, menu);
    }

    /** Rolls the lapis refund once per enchant action and gives the lapis back after the click has finished. */
    private static void lapis(ServerPlayer p, EnchantmentMenu menu) {
        long now = p.level().getGameTime();
        Long last = LAST.put(p.getUUID(), now);
        if (last != null && last == now) return; // already handled in this click
        double chance = discount(p, ModAttributes.ENCHANTER_TABLE_LAPIS.get());
        if (chance <= 0.0D || p.getAbilities().instabuild || p.getRandom().nextDouble() >= chance) return;

        Slot slot = menu.getSlot(1); // lapis slot
        ItemStack before = slot.getItem();
        if (before.isEmpty()) return;
        ItemStack kind = before.copy();
        int count = before.getCount();
        // tell() always queues the task (execute() may run it immediately while a packet is being handled, before the lapis is taken).
        p.server.tell(new TickTask(p.server.getTickCount(), () -> {
            if (p.containerMenu != menu) return;
            ItemStack now2 = slot.getItem();
            int missing = count - (now2.isEmpty() ? 0 : now2.getCount());
            if (missing <= 0) return;
            if (now2.isEmpty()) {
                ItemStack back = kind.copy();
                back.setCount(missing);
                slot.set(back);
            } else if (ItemStack.isSameItemSameTags(now2, kind)) {
                now2.grow(missing);
                slot.setChanged();
            }
            menu.broadcastChanges();
        }));
    }

    /** PERCENT attribute (0.01 = 1% ... 1.0 = 100%) capped by the configured maximum. */
    private static double discount(ServerPlayer p, net.minecraft.world.entity.ai.attributes.Attribute attribute) {
        return ChanceUtil.chance(p.getAttributeValue(attribute)); // the attribute is capped at 1.0
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        LAST.remove(e.getEntity().getUUID());
    }

    private EnchantTableHandler() {}
}
