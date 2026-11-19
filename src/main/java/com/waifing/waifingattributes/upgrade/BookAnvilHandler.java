package com.waifing.waifingattributes.upgrade;

import com.waifing.waifingattributes.WaifingAttributesMod;
import com.waifing.waifingattributes.config.WaifingConfig;
import com.waifing.waifingattributes.util.EnchantLevels;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lets the anvil keep enchantment levels that are above the game's maximum (vanilla, or Apotheosis' when installed),
 * as Rebirth books have. Vanilla would cut them down to the maximum. Always on.
 *
 * It only acts when a level involved is above that maximum and no other mod has already set the anvil's result (it
 * runs late, at LOW priority). Everything else follows vanilla: the usual level rule (equal levels +1, otherwise the
 * higher one, never above the maximum), incompatible enchantments are skipped and add 1 to the cost (as vanilla
 * does), renaming, cost and the "Too Expensive!" limit of 40 levels (none with Apotheosis, which removes it).
 * The only difference: a level that is already above the maximum is kept as it is.
 */
@Mod.EventBusSubscriber(modid = WaifingAttributesMod.MODID)
public final class BookAnvilHandler {
    private BookAnvilHandler() {}

    /** Vanilla's "Too Expensive!" limit. Apotheosis removes it (mixin on AnvilMenu), so with Apotheosis there is no limit. */
    private static final int TOO_EXPENSIVE = 40;
    private static final boolean APOTHEOSIS = ModList.get().isLoaded("apotheosis");

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onAnvil(AnvilUpdateEvent e) {
        if (e.isCanceled() || !e.getOutput().isEmpty()) return; // another mod already handles this anvil

        ItemStack left = e.getLeft();
        ItemStack right = e.getRight();
        if (left.isEmpty() || right.isEmpty() || !right.is(Items.ENCHANTED_BOOK)) return;

        Map<Enchantment, Integer> book = EnchantmentHelper.getEnchantments(right);
        if (book.isEmpty()) return;
        Map<Enchantment, Integer> result = new LinkedHashMap<>(EnchantmentHelper.getEnchantments(left));

        if (!anyAboveMax(result) && !anyAboveMax(book)) return;

        ItemStack out = left.copy();
        boolean leftIsBook = left.is(Items.ENCHANTED_BOOK);
        boolean creative = e.getPlayer() != null && e.getPlayer().getAbilities().instabuild;
        int cost = 0;
        boolean anyOk = false;

        for (Map.Entry<Enchantment, Integer> en : book.entrySet()) {
            Enchantment ench = en.getKey();
            if (ench == null) continue;
            int bookLvl = en.getValue();
            int cur = result.getOrDefault(ench, 0);
            int lvl = (cur == bookLvl) ? cur + 1 : Math.max(cur, bookLvl);

            boolean ok = creative || leftIsBook || ench.canEnchant(left);
            for (Enchantment other : result.keySet()) {
                if (other != ench && !ench.isCompatibleWith(other)) { ok = false; cost++; }
            }
            if (!ok) continue;

            anyOk = true;
            int ceiling = Math.max(EnchantLevels.naturalMax(ench), Math.max(cur, bookLvl));
            lvl = Math.min(lvl, ceiling);
            result.put(ench, lvl);

            int mult = switch (ench.getRarity()) {
                case COMMON -> 1; case UNCOMMON -> 2; case RARE -> 4; case VERY_RARE -> 8;
            };
            cost += Math.max(1, mult / 2) * lvl;   // book: rarity / 2
            if (left.getCount() > 1) cost = TOO_EXPENSIVE;
        }
        if (!anyOk) return; // let vanilla handle the failure

        String name = e.getName();
        if (name == null || name.isBlank()) {
            if (left.hasCustomHoverName()) { out.resetHoverName(); cost++; }
        } else if (!name.equals(left.getHoverName().getString())) {
            out.setHoverName(Component.literal(name)); cost++;
        }

        int total = left.getBaseRepairCost() + right.getBaseRepairCost() + cost;
        out.setRepairCost(Math.max(left.getBaseRepairCost(), right.getBaseRepairCost()) * 2 + 1);
        EnchantmentHelper.setEnchantments(result, out);

        e.setOutput(out);
        if (total >= TOO_EXPENSIVE && !APOTHEOSIS && !creative) {
            e.setCanceled(true); // as vanilla: too expensive, no result
            return;
        }
        e.setCost(total);
        e.setMaterialCost(1);
    }

    private static boolean anyAboveMax(Map<Enchantment, Integer> map) {
        for (Map.Entry<Enchantment, Integer> en : map.entrySet())
            if (en.getKey() != null && en.getValue() > EnchantLevels.naturalMax(en.getKey())) return true;
        return false;
    }
}
