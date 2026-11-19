package com.waifing.waifingattributes.compat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Optional;

/** Reads Consecration's "vulnerable" state (after fire or holy damage) through its public API, by reflection: no dependency. */
public final class ConsecrationCompat {
    private static boolean tried;
    private static Object api;
    private static MethodHandle getUndying, isVulnerable;

    private static boolean init() {
        if (tried) return api != null;
        tried = true;
        if (!ModList.get().isLoaded("consecration")) return false;
        try {
            Class<?> apiClass = Class.forName("com.illusivesoulworks.consecration.api.ConsecrationApi");
            Class<?> undying = Class.forName("com.illusivesoulworks.consecration.api.IUndying");
            MethodHandles.Lookup l = MethodHandles.publicLookup();
            Object instance = l.findStatic(apiClass, "getInstance", MethodType.methodType(apiClass)).invoke();
            getUndying = l.findVirtual(apiClass, "getUndying", MethodType.methodType(Optional.class, LivingEntity.class));
            isVulnerable = l.findVirtual(undying, "isVulnerable", MethodType.methodType(boolean.class));
            api = instance;
        } catch (Throwable t) {
            api = null;
        }
        return api != null;
    }

    public static boolean present() {
        return init();
    }

    /** True while Consecration keeps the entity vulnerable (fire / holy damage, 10 s by default). */
    public static boolean isVulnerable(LivingEntity e) {
        if (!init()) return false;
        try {
            Optional<?> u = (Optional<?>) getUndying.invoke(api, e);
            return u.isPresent() && (boolean) isVulnerable.invoke(u.get());
        } catch (Throwable t) {
            return false;
        }
    }

    private ConsecrationCompat() {}
}
