package com.waifing.waifingattributes;

import com.mojang.logging.LogUtils;
import com.waifing.waifingattributes.compat.StardewCompat;
import com.waifing.waifingattributes.config.WaifingClientConfig;
import com.waifing.waifingattributes.config.WaifingConfig;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.slf4j.Logger;

@Mod(WaifingAttributesMod.MODID)
public class WaifingAttributesMod {
    public static final String MODID = "waifingattributes";
    public static final Logger LOGGER = LogUtils.getLogger();

    public WaifingAttributesMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModAttributes.ATTRIBUTES.register(modBus);
        modBus.addListener(ModAttributes::onAttributeModification);
        modBus.addListener((ModConfigEvent.Loading e) -> WaifingConfig.markDirty());
        modBus.addListener((ModConfigEvent.Reloading e) -> WaifingConfig.markDirty());
        // SERVER: lives in the world, synchronized to the clients. CLIENT: only the player's own display options.
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, WaifingConfig.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, WaifingClientConfig.SPEC);
        if (ModList.get().isLoaded("stardew_fishing")) StardewCompat.register();
    }
}
