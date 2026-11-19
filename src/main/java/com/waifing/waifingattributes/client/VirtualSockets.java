package com.waifing.waifingattributes.client;

import com.waifing.waifingattributes.ModAttributes;
import com.waifing.waifingattributes.WaifingAttributesMod;
import com.waifing.waifingattributes.compat.ApotheosisSockets;
import com.waifing.waifingattributes.upgrade.UpgradeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Virtual sockets: a Blacksmith (blacksmith_gem_socket) sees the extra empty sockets he could fill drawn by Apotheosis itself, as if the
 * item had them. Only on the CLIENT RENDER THREAD of the player who has the attribute: the server, the integrated server of single
 * player and every other player still see the real sockets. A virtual socket becomes real when the Blacksmith socketes a gem with the
 * right click (ApotheosisSockets), so the gem stays on the item if this mod is removed.
 *
 * Apotheosis asks for the socket count through its public GetItemSocketsEvent. It is reached by reflection (no compile dependency).
 */
@Mod.EventBusSubscriber(modid = WaifingAttributesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class VirtualSockets {
    private static final String EVENT = "dev.shadowsoffire.apotheosis.adventure.event.GetItemSocketsEvent";
    private static MethodHandle getStack, getSockets, setSockets;

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        if (ApotheosisSockets.present()) event.enqueueWork(VirtualSockets::register);
    }

    @SuppressWarnings("unchecked")
    private static void register() {
        try {
            Class<? extends Event> cls = (Class<? extends Event>) Class.forName(EVENT);
            MethodHandles.Lookup l = MethodHandles.publicLookup();
            getStack = l.findVirtual(cls, "getStack", MethodType.methodType(ItemStack.class));
            getSockets = l.findVirtual(cls, "getSockets", MethodType.methodType(int.class));
            setSockets = l.findVirtual(cls, "setSockets", MethodType.methodType(void.class, int.class));
            MinecraftForge.EVENT_BUS.addListener(EventPriority.LOW, false, (Class<Event>) cls, VirtualSockets::onGetSockets);
        } catch (Throwable t) {
            WaifingAttributesMod.LOGGER.warn("[waifingattributes] Apotheosis sockets event not found, virtual sockets disabled: {}", t.toString());
        }
    }

    private static void onGetSockets(Event e) {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.isSameThread()) return; // never on the (integrated) server
        Player p = mc.player;
        if (p == null) return;
        int cap = (int) Math.floor(p.getAttributeValue(ModAttributes.BLACKSMITH_GEM_SOCKET.get()) + 1.0E-6);
        if (cap <= 0) return;
        try {
            if ((int) getSockets.invoke(e) >= cap) return;
            ItemStack stack = (ItemStack) getStack.invoke(e);
            if (UpgradeCategory.detect(stack) == null) return;
            setSockets.invoke(e, cap);
        } catch (Throwable ignored) {
        }
    }

    private VirtualSockets() {}
}
