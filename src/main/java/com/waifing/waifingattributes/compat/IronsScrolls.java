package com.waifing.waifingattributes.compat;

import com.waifing.waifingattributes.ModAttributes;
import com.waifing.waifingattributes.WaifingAttributesMod;
import com.waifing.waifingattributes.config.WaifingConfig;
import com.waifing.waifingattributes.upgrade.UpgradeContext;
import com.waifing.waifingattributes.util.ChanceUtil;
import com.waifing.waifingattributes.util.Fx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

/**
 * Iron's Spellbooks scrolls (OPTIONAL dependency). Nothing here is compiled against Iron's: scrolls are edited
 * through their NBT ({@code irons_spellbooks:spell_container -> data[0] {id, level}}) and the few API calls
 * (spell maximum level, the cast event) are made by reflection, so the mod builds and loads without it.
 *
 *  - merge: scroll onto a scroll of the same spell and level -> level + 1 with the enchanted-book chances.
 *  - split: paper onto a scroll of level L -> a scroll of level L-1 and another of level 1.
 *  - arcane anvil: scroll onto an item adds its spell to it (see {@link #arcane}).
 */
public final class IronsScrolls {
    private static final String MOD = "irons_spellbooks";
    private static final ResourceLocation SCROLL = new ResourceLocation(MOD, "scroll");
    private static final String NBT = "irons_spellbooks:spell_container";
    private static final String LEGACY_NBT = "ISB_Spells";

    private static Boolean loaded;

    public static boolean present() {
        if (loaded == null) {
            ModList list = ModList.get();
            if (list == null) return false; // too early, ask again later
            loaded = list.isLoaded(MOD);
        }
        return loaded;
    }

    public static boolean isScroll(ItemStack s) {
        return present() && !s.isEmpty() && SCROLL.equals(ForgeRegistries.ITEMS.getKey(s.getItem()));
    }

    // ------------------------------------------------------------------ merge / split
    public static void merge(UpgradeContext ctx) {
        ServerPlayer p = ctx.player();
        ItemStack target = ctx.target();
        CompoundTag t = slot(target);
        CompoundTag d = slot(ctx.catalyst());
        if (t == null || d == null || !t.getString("id").equals(d.getString("id")) || t.getInt("level") != d.getInt("level")) {
            Fx.denied(p); // different spell or level
            return;
        }
        int level = t.getInt("level");
        if (level >= maxLevel(t.getString("id"))) {
            Fx.denied(p);
            return;
        }
        if (!keepScroll(p)) ctx.consumeCatalyst(); // the second scroll is spent unless mage_keep_scroll saves it
        t.putInt("level", level + 1);
        Fx.sound(p, WaifingConfig.BOOK_OK_SOUND.get());
        Fx.particles(p, ParticleTypes.ENCHANT, 30);
    }

    public static void split(UpgradeContext ctx) {
        ServerPlayer p = ctx.player();
        ItemStack target = ctx.target();
        CompoundTag t = slot(target);
        if (t == null || t.getInt("level") < 2) {
            Fx.denied(p);
            return;
        }
        int level = t.getInt("level");
        ItemStack extra = target.copy();
        extra.setCount(1);
        CompoundTag e = slot(extra);
        if (e == null) {
            Fx.denied(p);
            return;
        }
        e.putInt("level", 1);
        t.putInt("level", level - 1);
        ctx.consumeCatalyst();
        ctx.give(extra);
        Fx.sound(p, WaifingConfig.BOOK_OK_SOUND.get());
        Fx.particles(p, ParticleTypes.ENCHANT, 20);
    }

    // ------------------------------------------------------------------ arcane anvil
    /**
     * "Arcane Anvil" by click: the cursor scroll's spell is added to the clicked item, with the same steps Iron's own
     * anvil takes (its container API, called by reflection). Only items Iron's accepts for imbuing ({@code Utils.canImbue}:
     * swords, its whitelist, items that already hold spells; never its blacklist or unique items).
     */
    public static void arcane(UpgradeContext ctx) {
        ServerPlayer p = ctx.player();
        ItemStack target = ctx.target();
        CompoundTag s = slot(ctx.catalyst());
        if (s == null) {
            Fx.denied(p);
            return;
        }
        try {
            String pkg = "io.redspace.ironsspellbooks.api.";
            Class<?> utils = Class.forName(pkg + "util.Utils");
            if (!(Boolean) utils.getMethod("canImbue", ItemStack.class).invoke(null, target)) {
                Fx.denied(p);
                return;
            }
            Class<?> registry = Class.forName(pkg + "registry.SpellRegistry");
            Object spell = registry.getMethod("getSpell", String.class).invoke(null, s.getString("id"));
            if (spell == null) {
                Fx.denied(p);
                return;
            }
            int level = s.getInt("level");
            Class<?> spellCls = Class.forName(pkg + "spells.AbstractSpell");
            Class<?> container = Class.forName(pkg + "spells.ISpellContainer");
            Class<?> mutableCls = Class.forName(pkg + "spells.ISpellContainerMutable");

            Object current = container.getMethod("getOrCreate", ItemStack.class).invoke(null, target);
            Object mutable = container.getMethod("mutableCopy").invoke(current);

            int index = (Integer) mutableCls.getMethod("getIndexForSpell", spellCls).invoke(mutable, spell);
            if (index >= 0) { // the item already has this spell: only a higher level improves it
                Object data = mutableCls.getMethod("getSpellAtIndex", int.class).invoke(mutable, index);
                int have = (Integer) data.getClass().getMethod("getLevel").invoke(data);
                if (level <= have) {
                    Fx.denied(p);
                    return;
                }
                mutableCls.getMethod("removeSpellAtIndex", int.class).invoke(mutable, index);
            } else {
                index = (Integer) mutableCls.getMethod("getNextAvailableIndex").invoke(mutable);
                if (index < 0) {
                    if (!WaifingConfig.ARCANE_REPLACE_FULL.get()) {
                        Fx.denied(p);
                        return;
                    }
                    index = 0;
                    mutableCls.getMethod("removeSpellAtIndex", int.class).invoke(mutable, index);
                }
            }
            boolean added = (Boolean) mutableCls.getMethod("addSpellAtIndex", spellCls, int.class, int.class, boolean.class)
                    .invoke(mutable, spell, level, index, false);
            if (!added) {
                Fx.denied(p);
                return;
            }
            Object immutable = mutableCls.getMethod("toImmutable").invoke(mutable);
            container.getMethod("set", ItemStack.class, container).invoke(null, target, immutable);
        } catch (Throwable t) {
            WaifingAttributesMod.LOGGER.warn("Arcane anvil click failed: {}", t.toString());
            Fx.denied(p);
            return;
        }
        if (!keepScroll(p)) ctx.consumeCatalyst();
        Fx.sound(p, WaifingConfig.BOOK_OK_SOUND.get());
        Fx.particles(p, ParticleTypes.ENCHANT, 30);
    }

    /** mage_keep_scroll is a PERCENT attribute: that chance of NOT spending the scroll (merging or Arcane Anvil). */
    private static boolean keepScroll(ServerPlayer p) {
        return p.getAbilities().instabuild || ChanceUtil.roll(p.getAttributeValue(ModAttributes.MAGE_KEEP_SCROLL.get()), p.getRandom());
    }

    // ------------------------------------------------------------------ NBT helpers
    /** The single spell entry of a scroll (live reference into the stack's NBT), or null. */
    @Nullable
    private static CompoundTag slot(ItemStack scroll) {
        CompoundTag root = scroll.getTag();
        if (root == null) return null;
        CompoundTag container = root.contains(NBT, 10) ? root.getCompound(NBT)
                : root.contains(LEGACY_NBT, 10) ? root.getCompound(LEGACY_NBT) : null;
        if (container == null) return null;
        ListTag data = container.getList("data", 10);
        if (data.isEmpty()) return null;
        CompoundTag first = data.getCompound(0);
        return first.contains("id") && first.contains("level") ? first : null;
    }

    public static int level(ItemStack scroll) {
        CompoundTag t = slot(scroll);
        return t == null ? 0 : t.getInt("level");
    }

    private static int maxLevel(String spellId) {
        try {
            Class<?> registry = Class.forName("io.redspace.ironsspellbooks.api.registry.SpellRegistry");
            Object spell = registry.getMethod("getSpell", String.class).invoke(null, spellId);
            if (spell == null) return 10;
            return (int) spell.getClass().getMethod("getMaxLevel").invoke(spell);
        } catch (Throwable t) {
            return 10;
        }
    }

    private IronsScrolls() {}
}
