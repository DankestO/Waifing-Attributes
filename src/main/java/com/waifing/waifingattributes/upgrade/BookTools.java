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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.Set;

/**
 * The "mage" tools of the enchantment mastery, all done by clicking:
 *  - APPLY: an enchanted book onto equipment enchants it (enchanter_keep_book: chance of keeping the book).
 *  - MERGE: an enchanted book onto another one; equal levels try +1 with the tier chances of enchanter_merge_books (the donor book can be kept too).
 *  - SPLIT: a normal book onto an enchanted book moves its last added enchantment to a new book.
 */
public final class BookTools {

    // ------------------------------------------------------------------ apply a book to equipment
    public static void apply(UpgradeContext ctx) {
        ServerPlayer p = ctx.player();
        ItemStack target = ctx.target();
        ItemStack book = ctx.catalyst();
        int hardCap = WaifingConfig.BOOK_HARD_CAP.get();

        Map<Enchantment, Integer> have = EnchantmentHelper.getEnchantments(target);
        Map<Enchantment, Integer> add = stored(book);
        int applied = 0;
        for (Map.Entry<Enchantment, Integer> en : add.entrySet()) {
            Enchantment ench = en.getKey();
            ResourceLocation id = ForgeRegistries.ENCHANTMENTS.getKey(ench);
            if (id == null || WaifingConfig.isEnchantBlacklisted(id) || !ench.canEnchant(target)) continue;
            int lvl = Math.min(en.getValue(), hardCap);
            Integer cur = have.get(ench);
            if (cur == null) {
                if (!compatible(ench, have.keySet())) continue;
                have.put(ench, lvl);
                applied++;
            } else if (lvl > cur) {
                have.put(ench, lvl);
                applied++;
            }
        }
        if (applied == 0) {
            Fx.denied(p);
            return;
        }
        EnchantmentHelper.setEnchantments(have, target);

        if (!keepBook(p)) ctx.consumeCatalyst();
        Fx.sound(p, WaifingConfig.BOOK_OK_SOUND.get());
        Fx.particles(p, ParticleTypes.ENCHANT, 30);
    }

    /** enchanter_keep_book is a PERCENT attribute: that chance of NOT spending the book (applying or merging). */
    private static boolean keepBook(ServerPlayer p) {
        return p.getAbilities().instabuild || ChanceUtil.roll(p.getAttributeValue(ModAttributes.ENCHANTER_KEEP_BOOK.get()), p.getRandom());
    }

    /** Equipment that may receive a book by click. */
    public static boolean canReceive(ItemStack target) {
        if (target.is(Items.ENCHANTED_BOOK)) return false;
        return target.isEnchantable() || target.isEnchanted();
    }

    // ------------------------------------------------------------------ merge two books
    public static void merge(UpgradeContext ctx) {
        ServerPlayer p = ctx.player();
        ItemStack target = ctx.target();
        ItemStack donor = ctx.catalyst();
        int hardCap = WaifingConfig.BOOK_HARD_CAP.get();

        ListTag list = EnchantedBookItem.getEnchantments(target); // live list
        Map<Enchantment, Integer> add = stored(donor);
        Map<Enchantment, Integer> have = stored(target);

        int attempted = 0;
        boolean changed = false;
        for (Map.Entry<Enchantment, Integer> en : add.entrySet()) {
            Enchantment ench = en.getKey();
            ResourceLocation id = ForgeRegistries.ENCHANTMENTS.getKey(ench);
            if (id == null || WaifingConfig.isEnchantBlacklisted(id)) continue;
            int lvl = Math.min(en.getValue(), hardCap);
            Integer cur = have.get(ench);
            if (cur == null) {
                if (!compatible(ench, have.keySet())) continue;
                attempted++;
                setLevel(list, id, lvl);
                have.put(ench, lvl);
                changed = true;
            } else if (lvl > cur) {
                attempted++;
                setLevel(list, id, lvl);
                have.put(ench, lvl);
                changed = true;
            } else if (lvl == cur) {
                int natural = EnchantLevels.naturalMax(ench);
                if (cur >= natural || cur >= hardCap) continue;
                attempted++;
                setLevel(list, id, cur + 1);
                have.put(ench, cur + 1);
                changed = true;
            }
        }
        if (attempted == 0) {
            Fx.denied(p);
            return;
        }
        if (!keepBook(p)) ctx.consumeCatalyst(); // the donor book is spent unless enchanter_keep_book saves it
        if (changed) {
            Fx.sound(p, WaifingConfig.BOOK_OK_SOUND.get());
            Fx.particles(p, ParticleTypes.ENCHANT, 30);
        } else {
            Fx.sound(p, WaifingConfig.BOOK_FAIL_SOUND.get());
            Fx.particles(p, ParticleTypes.SMOKE, 12);
        }
    }

    // ------------------------------------------------------------------ split a book
    public static void split(UpgradeContext ctx) {
        ServerPlayer p = ctx.player();
        ItemStack target = ctx.target();
        ListTag list = EnchantedBookItem.getEnchantments(target); // live list
        if (list.size() < 2) {
            Fx.denied(p);
            return;
        }
        CompoundTag last = list.getCompound(list.size() - 1).copy(); // the last one that was added
        list.remove(list.size() - 1);

        ItemStack out = new ItemStack(Items.ENCHANTED_BOOK);
        ListTag one = new ListTag();
        one.add(last);
        out.getOrCreateTag().put("StoredEnchantments", one);

        ctx.consumeCatalyst();
        ctx.give(out);
        Fx.sound(p, WaifingConfig.BOOK_OK_SOUND.get());
        Fx.particles(p, ParticleTypes.ENCHANT, 20);
    }

    // ------------------------------------------------------------------ copying written books
    private static boolean hasPages(ItemStack s) {
        return s.hasTag() && !s.getTag().getList("pages", net.minecraft.nbt.Tag.TAG_STRING).isEmpty();
    }

    /** Target: a Written Book, or a Book and Quill with text. Catalyst: a blank Book and Quill. */
    public static boolean canCopyWritten(ItemStack target, ItemStack catalyst) {
        if (!catalyst.is(Items.WRITABLE_BOOK) || hasPages(catalyst)) return false;
        return target.is(Items.WRITTEN_BOOK) || (target.is(Items.WRITABLE_BOOK) && hasPages(target));
    }

    /**
     * enchanter_extract_book also copies books with text: a blank Book and Quill RIGHT-clicked onto a Written Book or onto a Book and
     * Quill with text becomes a copy of it (the blank one is spent, the source is untouched). An unsigned copy stays editable. A signed
     * copy is marked like vanilla copies ("Copy of original", then "Copy of a copy"), but unlike vanilla any generation can be copied.
     */
    public static void copyWritten(UpgradeContext ctx) {
        ServerPlayer p = ctx.player();
        ItemStack copy = ctx.target().copyWithCount(1);
        if (copy.is(Items.WRITTEN_BOOK)) {
            int generation = copy.getOrCreateTag().getInt("generation");
            if (generation < 2) copy.getOrCreateTag().putInt("generation", generation + 1); // 0 original, 1 copy, 2 copy of a copy, 3 tattered
        }
        ctx.consumeCatalyst();
        ctx.give(copy);
        Fx.sound(p, WaifingConfig.BOOK_OK_SOUND.get());
        Fx.particles(p, ParticleTypes.ENCHANT, 12);
    }

    /**
     * enchanter_extract_book: a normal book (splitBooks.items) RIGHT-clicked onto an enchanted item takes the last added enchantment off
     * the item and turns it into an enchanted book. Curses are never extracted (that would make curses removable).
     */
    public static void extract(UpgradeContext ctx) {
        ServerPlayer p = ctx.player();
        ItemStack target = ctx.target();
        Map<Enchantment, Integer> have = EnchantmentHelper.getEnchantments(target); // keeps the item's order
        Enchantment pick = null;
        for (Enchantment e : have.keySet()) if (!e.isCurse()) pick = e; // the last non-curse one
        if (pick == null) {
            Fx.denied(p);
            return;
        }
        int lvl = have.remove(pick);
        EnchantmentHelper.setEnchantments(have, target);
        ItemStack out = EnchantedBookItem.createForEnchantment(new net.minecraft.world.item.enchantment.EnchantmentInstance(pick, lvl));

        ctx.consumeCatalyst();
        ctx.give(out);
        Fx.sound(p, WaifingConfig.BOOK_OK_SOUND.get());
        Fx.particles(p, ParticleTypes.ENCHANT, 20);
    }

    // ------------------------------------------------------------------ helpers
    /** The enchantments stored in an enchanted book (a copy, safe to modify). */
    private static Map<Enchantment, Integer> stored(ItemStack book) {
        return EnchantmentHelper.deserializeEnchantments(EnchantedBookItem.getEnchantments(book));
    }

    private static boolean compatible(Enchantment ench, Set<Enchantment> existing) {
        for (Enchantment o : existing) if (o != ench && !ench.isCompatibleWith(o)) return false;
        return true;
    }

    /** Sets the level of {@code id} in a book's StoredEnchantments list, adding it at the end when missing. */
    private static void setLevel(ListTag list, ResourceLocation id, int level) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            if (id.equals(EnchantmentHelper.getEnchantmentId(c))) {
                c.putShort("lvl", (short) level);
                return;
            }
        }
        list.add(EnchantmentHelper.storeEnchantment(id, level));
    }

    private BookTools() {}
}
