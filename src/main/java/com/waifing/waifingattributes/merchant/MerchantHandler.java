package com.waifing.waifingattributes.merchant;

import com.waifing.waifingattributes.ModAttributes;
import com.waifing.waifingattributes.WaifingAttributesMod;
import com.waifing.waifingattributes.util.ChanceUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ai.gossip.GossipContainer;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.TradeWithVillagerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.UUID;

/**
 * Trading attributes. No mixins: vanilla and Forge events only.
 *  merchant_haggle      (PERCENT): discount. Right before the trading screen opens, the villager gets temporary good gossip about the
 *                       player (1.0 = the gossip of curing a zombie villager, the biggest vanilla discount). Vanilla turns it into prices
 *                       when the screen opens and keeps them while it is open; the gossip is removed on the next server task.
 *  merchant_xp          (PERCENT): extra experience orbs for the player on each trade that gives experience (1.0 = +100%).
 *  merchant_restock     (PERCENT): chance that a trade does not spend a use of the offer.
 *  merchant_villager_xp (PERCENT): the villager gains extra trading experience (1.0 = +100%), so it levels up faster.
 *  rogue_pickpocket     (PERCENT): chance of getting back what you paid ("you steal it back").
 */
@Mod.EventBusSubscriber(modid = WaifingAttributesMod.MODID)
public final class MerchantHandler {
    private static final int MAJOR_MAX = 20; // GossipType.MAJOR_POSITIVE max
    private static final int MINOR_MAX = 25; // GossipType.MINOR_POSITIVE max

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.isCanceled() || event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getEntity() instanceof ServerPlayer p) || p instanceof FakePlayer) return;
        if (!(event.getTarget() instanceof Villager v) || v.isBaby() || v.isTrading()) return;
        double h = ChanceUtil.chance(p.getAttributeValue(ModAttributes.MERCHANT_HAGGLE.get()));
        if (h <= 0.0D) return;

        UUID id = p.getUUID();
        GossipContainer g = v.getGossips();
        int major = add(g, id, GossipType.MAJOR_POSITIVE, (int) Math.round(MAJOR_MAX * h));
        int minor = add(g, id, GossipType.MINOR_POSITIVE, (int) Math.round(MINOR_MAX * h));
        if (major == 0 && minor == 0) return;
        p.server.tell(new TickTask(p.server.getTickCount(), () -> {
            if (major > 0) g.add(id, GossipType.MAJOR_POSITIVE, -major);
            if (minor > 0) g.add(id, GossipType.MINOR_POSITIVE, -minor);
        }));
    }

    /** Adds gossip and returns how much was really added (it is capped by the type's maximum). */
    private static int add(GossipContainer g, UUID id, GossipType type, int amount) {
        if (amount <= 0) return 0;
        int before = g.getReputation(id, t -> t == type);
        g.add(id, type, amount);
        return (g.getReputation(id, t -> t == type) - before) / Math.max(1, type.weight);
    }

    @SubscribeEvent
    public static void onTrade(TradeWithVillagerEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p instanceof FakePlayer) return;
        MerchantOffer offer = event.getMerchantOffer();
        AbstractVillager trader = event.getAbstractVillager();
        if (offer == null || trader == null) return;

        // rogue_pickpocket: get back what was paid
        if (ChanceUtil.roll(p.getAttributeValue(ModAttributes.ROGUE_PICKPOCKET.get()), p.getRandom())) {
            ItemStack a = offer.getCostA().copy();
            ItemStack b = offer.getCostB().copy();
            if (!a.isEmpty()) ItemHandlerHelper.giveItemToPlayer(p, a);
            if (!b.isEmpty()) ItemHandlerHelper.giveItemToPlayer(p, b);
        }
        // merchant_xp: extra orbs, same base as vanilla (3 to 6 per trade)
        double xp = ChanceUtil.chance(p.getAttributeValue(ModAttributes.MERCHANT_XP.get()));
        if (xp > 0.0D && offer.shouldRewardExp()) {
            int amount = ChanceUtil.scaleRandom(3 + p.getRandom().nextInt(4), xp, p.getRandom());
            if (amount > 0) trader.level().addFreshEntity(new ExperienceOrb(trader.level(), trader.getX(), trader.getY() + 0.5D, trader.getZ(), amount));
        }
        // merchant_villager_xp: extra trading experience for the villager
        double vxp = ChanceUtil.chance(p.getAttributeValue(ModAttributes.MERCHANT_VILLAGER_XP.get()));
        if (vxp > 0.0D && trader instanceof Villager v) {
            int extra = ChanceUtil.scaleRandom(offer.getXp(), vxp, p.getRandom());
            if (extra > 0) v.setVillagerXp(v.getVillagerXp() + extra);
        }
        // merchant_restock: give the use back on the next server task (after vanilla counted it)
        if (ChanceUtil.roll(p.getAttributeValue(ModAttributes.MERCHANT_RESTOCK.get()), p.getRandom())) {
            p.server.tell(new TickTask(p.server.getTickCount(), () -> restock(p, trader, offer)));
        }
    }

    /** MerchantOffer has no public way to lower its uses, so the offer is rebuilt from its own NBT with one use less. */
    private static void restock(ServerPlayer p, AbstractVillager trader, MerchantOffer offer) {
        MerchantOffers offers = trader.getOffers();
        int index = -1;
        for (int i = 0; i < offers.size(); i++) if (offers.get(i) == offer) { index = i; break; }
        if (index < 0 || offer.getUses() <= 0) return;
        CompoundTag tag = offer.createTag();
        tag.putInt("uses", offer.getUses() - 1);
        MerchantOffer fixed = new MerchantOffer(tag);
        fixed.setSpecialPriceDiff(offer.getSpecialPriceDiff());
        offers.set(index, fixed);
        if (p.containerMenu instanceof MerchantMenu menu && trader.getTradingPlayer() == p) {
            int level = trader instanceof Villager v ? v.getVillagerData().getLevel() : 1;
            p.sendMerchantOffers(menu.containerId, offers, level, trader.getVillagerXp(), trader.showProgressBar(), trader.canRestock());
        }
    }

    private MerchantHandler() {}
}
