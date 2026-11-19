package com.waifing.waifingattributes.compat;

import com.waifing.waifingattributes.ModAttributes;
import com.waifing.waifingattributes.config.WaifingConfig;
import com.waifing.waifingattributes.upgrade.UpgradeContext;
import com.waifing.waifingattributes.util.ChanceUtil;
import com.waifing.waifingattributes.util.Fx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.fml.ModList;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * blacksmith_gem_socket (AMOUNT, max 5.0): the Blacksmith can socket an Apotheosis gem into a piece of equipment even if it has no
 * sockets. Gem on the cursor, right-click the item:
 *  - the item has a free socket: the gem goes in (same result as the vanilla smithing table);
 *  - it has none free: a socket is added first, as long as the item would end with at most floor(attribute) sockets.
 *
 * Apotheosis keeps sockets in the item's "affix_data" tag ("sockets" = amount, "gems" = the gems). Instead of writing that format here,
 * the Apotheosis SocketingRecipe is asked (matches/assemble through the vanilla Recipe interface, found by class name): it decides if
 * the gem fits the item and builds the result, so every rule of Apotheosis (gem class, unique gems, other mods' events) still applies.
 * No compile-time dependency; only runs on a click.
 */
public final class ApotheosisSockets {
    private static final String MOD = "apotheosis";
    private static final ResourceLocation GEM = new ResourceLocation(MOD, "gem");
    private static final String RECIPE_CLASS = "dev.shadowsoffire.apotheosis.adventure.socket.SocketingRecipe";
    private static final String AFFIX_DATA = "affix_data";
    private static final String SOCKETS = "sockets";

    private static Boolean loaded;

    public static boolean present() {
        if (loaded == null) {
            ModList list = ModList.get();
            if (list == null) return false;
            loaded = list.isLoaded(MOD);
        }
        return loaded;
    }

    public static boolean isGem(ItemStack s) {
        return present() && !s.isEmpty() && GEM.equals(ForgeRegistries.ITEMS.getKey(s.getItem()));
    }

    public static void run(UpgradeContext ctx) {
        ServerPlayer p = ctx.player();
        ServerLevel level = p.serverLevel();
        ItemStack gem = ctx.catalyst();
        Recipe<Container> recipe = socketing(level);
        if (recipe == null) {
            Fx.denied(p);
            return;
        }
        int cap = ChanceUtil.levels(p.getAttributeValue(ModAttributes.BLACKSMITH_GEM_SOCKET.get()));
        ItemStack base = ctx.target().copy();
        base.setCount(1);
        ItemStack one = gem.copy();
        one.setCount(1);
        // blacksmith_gem_quality: the gem needs its own ids before it goes in, so the quality can be tied to it (see GemQuality)
        float quality = (float) ChanceUtil.chance(p.getAttributeValue(ModAttributes.BLACKSMITH_GEM_QUALITY.get()));
        UUID gemId = quality > 0.0F ? gemId(one) : null;

        if (!recipe.matches(new SimpleContainer(ItemStack.EMPTY, base, one), level)) {
            // Is it only a missing socket? Ask again with one more (on a copy: the real item is untouched if the gem does not fit).
            int sockets = sockets(base);
            ItemStack probe = base.copy();
            probe.getOrCreateTagElement(AFFIX_DATA).putInt(SOCKETS, sockets + 1);
            if (!recipe.matches(new SimpleContainer(ItemStack.EMPTY, probe, one), level)) {
                // Apotheosis refuses the gem itself: no bonus for this kind of item at the gem's rarity, or a unique gem already socketed
                Fx.denied(p);
                return;
            }
            if (sockets >= cap) {
                Fx.denied(p); // no free socket and the attribute allows no more
                return;
            }
            base = probe;
        }
        ItemStack out = recipe.assemble(new SimpleContainer(ItemStack.EMPTY, base, one), level.registryAccess());
        if (out.isEmpty()) {
            Fx.denied(p);
            return;
        }
        if (gemId != null) GemQuality.record(out, gemId, quality);
        ctx.replaceTarget(out);
        ctx.consumeCatalyst();
        Fx.sound(p, WaifingConfig.EQUIP_OK_SOUND.get());
        Fx.particles(p, ParticleTypes.ENCHANT, 30);
    }

    private static Method getUuids;

    /** First uuid of the gem (Apotheosis creates and stores them in the stack on first use); null if unavailable. */
    private static UUID gemId(ItemStack gem) {
        try {
            if (getUuids == null) {
                getUuids = Class.forName("dev.shadowsoffire.apotheosis.adventure.socket.gem.GemItem").getMethod("getUUIDs", ItemStack.class);
            }
            List<?> ids = (List<?>) getUuids.invoke(null, gem);
            return ids.isEmpty() ? null : (UUID) ids.get(0);
        } catch (Throwable t) {
            return null;
        }
    }

    private static int sockets(ItemStack s) {
        CompoundTag t = s.getTagElement(AFFIX_DATA);
        return t == null ? 0 : t.getInt(SOCKETS);
    }

    @SuppressWarnings("unchecked")
    private static Recipe<Container> socketing(ServerLevel level) {
        // Looked up by class name and walked as a plain Recipe, so nothing here assumes the exact type Apotheosis extends.
        for (Recipe<?> r : level.getRecipeManager().getRecipes()) {
            if (RECIPE_CLASS.equals(r.getClass().getName())) return (Recipe<Container>) r;
        }
        return null;
    }

    private ApotheosisSockets() {}
}
