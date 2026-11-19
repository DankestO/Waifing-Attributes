package com.waifing.waifingattributes.upgrade;

import com.waifing.waifingattributes.ModAttributes;
import com.waifing.waifingattributes.config.WaifingConfig;
import com.waifing.waifingattributes.util.ChanceUtil;
import com.waifing.waifingattributes.util.EnchantLevels;
import com.waifing.waifingattributes.util.Fx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Rebirth catalyst (on the cursor, right-clicked onto an enchanted book): one roll, with the chance of the PERCENT attribute
 * enchanter_book_rebirth, pushes every enchantment that is at its natural maximum above it (up to {@code maxLevelsAboveMax}
 * and {@code hardCap}). Enchantments below their maximum are left as they are.
 */
public final class BookUpgrader {

    public static void run(UpgradeContext ctx) {
        ServerPlayer p = ctx.player();
        ItemStack main = ctx.target();

        ListTag list = EnchantedBookItem.getEnchantments(main); // live list inside the NBT
        int hardCap = WaifingConfig.BOOK_HARD_CAP.get();
        int above = WaifingConfig.BOOK_REBIRTH_MAX_ABOVE.get();

        // Only enchantments that are already at their natural maximum can be pushed above it; the rest of the book is left alone.
        List<Integer> rebirth = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            ResourceLocation id = EnchantmentHelper.getEnchantmentId(c);
            if (id == null || WaifingConfig.isEnchantBlacklisted(id)) continue;
            Enchantment ench = ForgeRegistries.ENCHANTMENTS.getValue(id);
            if (ench == null) continue;
            int lvl = EnchantmentHelper.getEnchantmentLevel(c);
            int natural = EnchantLevels.naturalMax(ench);
            if (natural > 1 && lvl >= natural && lvl < Math.min(hardCap, natural + above)) rebirth.add(i); // maximum-I enchantments cannot be rebirthed
        }
        if (rebirth.isEmpty()) {
            Fx.denied(p);
            return;
        }
        rebirthUpgrade(ctx, list, rebirth);
    }

    private static void rebirthUpgrade(UpgradeContext ctx, ListTag list, List<Integer> idx) {
        ServerPlayer p = ctx.player();
        ItemStack off = ctx.catalyst();
        double chance = p.getAttributeValue(ModAttributes.ENCHANTER_BOOK_REBIRTH.get()); // PERCENT attribute
        if (chance <= 0.0D || !WaifingConfig.bookRebirthAllowed(off)) {
            Fx.denied(p);
            return;
        }
        ctx.consumeCatalyst();

        if (ChanceUtil.roll(chance, p.getRandom())) {
            for (int i : idx) raise(list.getCompound(i));
            Fx.rebirthSuccess(p);
            Fx.particles(p, ParticleTypes.TOTEM_OF_UNDYING, 40);
        } else if (curse(ctx)) {
            Fx.sound(p, WaifingConfig.BOOK_FAIL_SOUND.get());
            Fx.particles(p, ParticleTypes.SOUL, 20);
        } else if (WaifingConfig.BOOK_REBIRTH_DESTROY.get()) {
            ctx.destroyTarget();
            Fx.sound(p, WaifingConfig.BOOK_FAIL_SOUND.get());
            Fx.particles(p, ParticleTypes.SMOKE, 20);
        } else {
            Fx.sound(p, WaifingConfig.BOOK_FAIL_SOUND.get());
        }
    }

    /** A failed rebirth may turn the book into a cursed one. Returns true when it did. */
    private static boolean curse(UpgradeContext ctx) {
        List<ResourceLocation> curses = WaifingConfig.curses();
        if (curses.isEmpty() || ctx.player().getRandom().nextDouble() >= WaifingConfig.BOOK_REBIRTH_CURSE_CHANCE.get()) return false;
        Enchantment ench = ForgeRegistries.ENCHANTMENTS.getValue(curses.get(ctx.player().getRandom().nextInt(curses.size())));
        if (ench == null) return false;
        ctx.replaceTarget(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ench, 1)));
        return true;
    }

    private static void raise(CompoundTag c) {
        c.putShort("lvl", (short) (EnchantmentHelper.getEnchantmentLevel(c) + 1));
    }

    private BookUpgrader() {}
}
