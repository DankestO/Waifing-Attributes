package com.waifing.waifingattributes.compat;

import net.minecraft.world.entity.Entity;

/** Iron's Spellbooks summons implement io.redspace.ironsspellbooks.entity.mobs.IMagicSummon. Looked up by name: no dependency. */
public final class IronsSummons {
    private static final Class<?> SUMMON = find();

    private static Class<?> find() {
        try {
            return Class.forName("io.redspace.ironsspellbooks.entity.mobs.IMagicSummon");
        } catch (Throwable t) {
            return null;
        }
    }

    public static boolean isSummon(Entity e) {
        return SUMMON != null && SUMMON.isInstance(e);
    }

    private IronsSummons() {}
}
