package com.waifing.waifingattributes.util;

import com.waifing.waifingattributes.config.WaifingConfig;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.registries.ForgeRegistries;

public final class Fx {
    public static void sound(ServerPlayer p, String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return;
        SoundEvent ev = ForgeRegistries.SOUND_EVENTS.getValue(rl);
        if (ev == null) return;
        float volume = WaifingConfig.SOUND_VOLUME.get().floatValue(); // multiplied client-side by the player's own sound settings
        if (volume <= 0.0F) return;
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), ev, SoundSource.PLAYERS, volume, 1.0F);
    }

    /** Sound of a successful Rebirth (the lucky moment). */
    public static void rebirthSuccess(ServerPlayer p) {
        sound(p, WaifingConfig.REBIRTH_OK_SOUND.get());
    }

    /** The click could not upgrade anything: short "no" sound. */
    public static void denied(ServerPlayer p) {
        sound(p, WaifingConfig.DENIED_SOUND.get());
    }

    public static void particles(ServerPlayer p, ParticleOptions type, int count) {
        if (p.level() instanceof ServerLevel sl) {
            sl.sendParticles(type, p.getX(), p.getY() + 1.0D, p.getZ(), count, 0.4D, 0.4D, 0.4D, 0.05D);
        }
    }

    private Fx() {}
}
