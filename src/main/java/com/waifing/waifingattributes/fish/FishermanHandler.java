package com.waifing.waifingattributes.fish;

import com.waifing.waifingattributes.ModAttributes;
import com.waifing.waifingattributes.WaifingAttributesMod;
import com.waifing.waifingattributes.compat.QualityBoost;
import com.waifing.waifingattributes.compat.StardewCompat;
import com.waifing.waifingattributes.config.WaifingConfig;
import com.waifing.waifingattributes.util.ChanceUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Fisherman, the Hunter of the water: extra copies of the fished items and quality for them.
 * Quality: fisherman_catch_quality adds that many levels on top of whatever quality the catch already has (vanilla: none; Stardew Fishing: the
 * minigame quality), never above diamond. Works the same with or without Stardew Fishing.
 */
@Mod.EventBusSubscriber(modid = WaifingAttributesMod.MODID)
public final class FishermanHandler {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onFished(ItemFishedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        if (StardewCompat.lastHook != null && StardewCompat.lastHook == event.getHookEntity()) {
            StardewCompat.lastHook = null; // already handled through Stardew's own rewards list
            return;
        }
        apply(p, event.getDrops());
    }

    public static void apply(ServerPlayer p, List<ItemStack> drops) {
        double amount = p.getAttributeValue(ModAttributes.FISHERMAN_CATCH_AMOUNT.get());
        double quality = p.getAttributeValue(ModAttributes.FISHERMAN_CATCH_QUALITY.get());
        if (drops.isEmpty() || (amount <= 0.0D && quality <= 0.0D)) return;

        List<ItemStack> fish = new ArrayList<>();
        for (ItemStack s : drops) if (WaifingConfig.isFishItem(s)) fish.add(s);
        if (fish.isEmpty()) return;

        if (QualityBoost.present()) {
            int levels = ChanceUtil.levels(quality); // QualityBoost never goes above diamond
            if (levels > 0) for (ItemStack s : fish) QualityBoost.raiseBy(s, levels);
        }

        int rolls = ChanceUtil.rolls(amount, p.getRandom(), 5);
        for (int i = 0; i < rolls; i++) for (ItemStack s : fish) drops.add(s.copy());
    }

    private FishermanHandler() {}
}
