package com.waifing.waifingattributes.compat;

import com.waifing.waifingattributes.util.ChanceUtil;
import com.waifing.waifingattributes.ModAttributes;
import com.waifing.waifingattributes.WaifingAttributesMod;
import com.waifing.waifingattributes.config.WaifingConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Chef x Kaleidoscope Cookery (OPTIONAL, nothing is compiled against it: reflection only).
 *
 * Kaleidoscope's pot, stockpot, steamer and shawarma spit cook over time, with nobody to credit, so Quality Food gives them no
 * player. The bonus is applied when a player interacts with a FINISHED dish: right before Kaleidoscope hands it over,
 * the finished result is raised by the Chef tiers, once per dish (a flag in the block's persistent data, cleared
 * as soon as the block is clicked while it is not finished). Event driven: it only runs on a right click, and only
 * does anything on those three blocks. Crafting, the chopping board and the millstone are never touched.
 */
@Mod.EventBusSubscriber(modid = WaifingAttributesMod.MODID)
public final class CookeryQuality {
    private static final String PKG = "com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.";
    private static final String FLAG = "waifingattributes_cooked";

    /** Ready-to-eat foods (config craftedFoods): Quality Food has already rolled at the crafting grid, the bonus goes on top. */
    @SubscribeEvent
    public static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !QualityBoost.present()) return;
        ItemStack stack = event.getCrafting();
        if (stack.isEmpty() || !WaifingConfig.isCraftedFood(stack)) return;
        QualityBoost.raiseBy(stack, ChanceUtil.levels(player.getAttributeValue(ModAttributes.CHEF_COOKING_QUALITY.get())));
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !QualityBoost.present()) return;
        int levels = ChanceUtil.levels(player.getAttributeValue(ModAttributes.CHEF_COOKING_QUALITY.get()));
        if (levels <= 0) return;
        BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
        if (be == null) return;
        String name = be.getClass().getName();
        if (!name.startsWith(PKG)) return;

        try {
            ItemStack dish = null;
            switch (name.substring(PKG.length())) {
                case "PotBlockEntity" -> dish = finished(be, 2);       // IPot.FINISHED
                case "StockpotBlockEntity" -> dish = finished(be, 3);  // IStockpot.FINISHED
                case "ShawarmaSpitBlockEntity" -> dish = shawarma(be);
                case "SteamerBlockEntity" -> {
                    steamer(be, levels);
                    return;
                }
                default -> { return; } // steamer, chopping board, millstone...: not covered
            }
            CompoundTag data = be.getPersistentData();
            if (dish == null || dish.isEmpty()) {
                if (data.contains(FLAG)) data.remove(FLAG); // a new dish will be cooked: it may be raised again
                return;
            }
            if (data.getBoolean(FLAG)) return; // this dish was already raised
            QualityBoost.raiseBy(dish, levels);
            data.putBoolean(FLAG, true);
            be.setChanged();
        } catch (Throwable t) {
            WaifingAttributesMod.LOGGER.warn("Could not apply the Chef bonus to {}: {}", name, t.toString());
        }
    }

    /** Pot and stockpot: {@code getResult()} is the finished dish when {@code getStatus()} equals the FINISHED constant. */
    private static ItemStack finished(BlockEntity be, int finishedStatus) throws ReflectiveOperationException {
        Method status = be.getClass().getMethod("getStatus");
        if ((Integer) status.invoke(be) != finishedStatus) return null;
        return (ItemStack) be.getClass().getMethod("getResult").invoke(be);
    }

    /**
     * Steamer: every slot holds either the raw food or, once cooked, the result; Kaleidoscope marks a cooked slot with
     * {@code cookingTime == -1}. Only those slots are raised (a bit per slot remembers it), so raw food that is taken out
     * early is never improved.
     */
    @SuppressWarnings("unchecked")
    private static void steamer(BlockEntity be, int levels) throws ReflectiveOperationException {
        java.util.List<ItemStack> items = (java.util.List<ItemStack>) be.getClass().getMethod("getItems").invoke(be);
        int[] time = (int[]) be.getClass().getMethod("getCookingTime").invoke(be);
        CompoundTag data = be.getPersistentData();
        int mask = data.getInt(FLAG + "_mask");
        int old = mask;
        for (int i = 0; i < items.size() && i < time.length && i < 31; i++) {
            int bit = 1 << i;
            ItemStack s = items.get(i);
            if (!s.isEmpty() && time[i] == -1) {
                if ((mask & bit) == 0) {
                    QualityBoost.raiseBy(s, levels);
                    mask |= bit;
                }
            } else {
                mask &= ~bit; // empty, or still raw: a new dish may come
            }
        }
        if (mask != old) {
            if (mask == 0) data.remove(FLAG + "_mask");
            else data.putInt(FLAG + "_mask", mask);
            be.setChanged();
        }
    }

    /** Shawarma spit: the cooked item is ready when the cook time has run out. */
    private static ItemStack shawarma(BlockEntity be) throws ReflectiveOperationException {
        Field cookTime = be.getClass().getField("cookTime");
        if (cookTime.getInt(be) > 0) return null;
        return (ItemStack) be.getClass().getField("cookedItem").get(be);
    }

    private CookeryQuality() {}
}
