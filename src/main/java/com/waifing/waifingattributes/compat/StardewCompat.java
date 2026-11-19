package com.waifing.waifingattributes.compat;

import com.waifing.waifingattributes.WaifingAttributesMod;
import com.waifing.waifingattributes.fish.FishermanHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Consumer;

/**
 * Stardew Fishing (OPTIONAL, reached by reflection; nothing is compiled against it and its minigame is never touched).
 *
 * Flow of that mod: minigame ends -> a success gets its quality from the accuracy -> (StardewMinigameModifyRewardsEvent) -> rewards are
 * dropped. The Fisherman acts on the rewards event only: fisherman_catch_quality raises the quality the minigame gave and the fish are
 * duplicated like the Hunter's drops. A failed minigame is left alone.
 */
public final class StardewCompat {
    /** The hook whose rewards this class has just handled: the ItemFishedEvent that follows must not be handled twice. */
    public static volatile FishingHook lastHook;

    private static Method getPlayer, getHook, getRewards;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void register() {
        try {
            String base = "com.bonker.stardewfishing.server.event.";
                        Class<?> rewards = Class.forName(base + "StardewMinigameModifyRewardsEvent");
            Class<?> any = Class.forName(base + "StardewMinigameEvent");
            getPlayer = any.getMethod("getPlayer");
            getHook = any.getMethod("getHook");
            getRewards = rewards.getMethod("getRewards");
            MinecraftForge.EVENT_BUS.addListener(EventPriority.LOW, false, (Class) rewards, (Consumer) (Consumer<Event>) StardewCompat::onRewards);
            WaifingAttributesMod.LOGGER.info("Stardew Fishing found: the Fisherman quality bonus is active.");
        } catch (Throwable t) {
            WaifingAttributesMod.LOGGER.warn("Could not hook Stardew Fishing, the Fisherman works with vanilla fishing only: {}", t.toString());
        }
    }

    @SuppressWarnings("unchecked")
    private static void onRewards(Event e) {
        try {
            if (!(getPlayer.invoke(e) instanceof ServerPlayer p)) return;
            List<ItemStack> rewards = (List<ItemStack>) getRewards.invoke(e);
            lastHook = (FishingHook) getHook.invoke(e);
            FishermanHandler.apply(p, rewards);
        } catch (Throwable t) {
            WaifingAttributesMod.LOGGER.debug("Fisherman bonus (rewards) skipped: {}", t.toString());
        }
    }

    private StardewCompat() {}
}
