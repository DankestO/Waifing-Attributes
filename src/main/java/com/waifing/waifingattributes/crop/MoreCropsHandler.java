package com.waifing.waifingattributes.crop;

import com.waifing.waifingattributes.ModAttributes;
import com.waifing.waifingattributes.WaifingAttributesMod;
import com.waifing.waifingattributes.config.WaifingConfig;
import com.waifing.waifingattributes.util.ChanceUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * farmer: when a player harvests a plant, every harvest-item drop gets extra units.
 * Each whole attribute point = 1 extra unit of the dropped item, the decimal part is the chance of one more.
 *
 * How it works (works with ANY mod's crops and tools, no compile-time dependency on them):
 *  1. Breaking a ripe plant, or right-clicking a block, opens a short-lived "harvest zone" for that player
 *     (only for the current game tick).
 *  2. Any harvest item (see config harvestItems) that spawns as an item entity inside the zone during that same tick
 *     receives the bonus, once per item type and block position.
 *
 * This covers breaking, right-click harvesting (berries, Kaleidoscope Cookery tomatoes, chili...), Tinkers' Construct
 * scythes, Kaleidoscope Cookery sickles and anything else that drops the crop as an item.
 */
@Mod.EventBusSubscriber(modid = WaifingAttributesMod.MODID)
public final class MoreCropsHandler {

    private static final double BREAK_RADIUS = 1.8D;
    /**
     * Right-click harvests (berries, tomatoes, Tinkers scythes, sickles...) can cover an area decided by the tool itself,
     * so the zone is generous. It is harmless: only harvest items that spawn during that same tick count.
     */
    private static final double CLICK_RADIUS = 16.0D;

    private static final class Zone {
        final long tick;
        final ServerLevel level;
        final List<double[]> areas = new ArrayList<>(); // x, y, z, radius^2
        final Set<String> done = new HashSet<>();

        Zone(long tick, ServerLevel level) {
            this.tick = tick;
            this.level = level;
        }

        boolean contains(double x, double y, double z) {
            for (double[] a : areas) {
                double dx = x - a[0], dy = y - a[1], dz = z - a[2];
                if (dx * dx + dy * dy + dz * dz <= a[3]) return true;
            }
            return false;
        }
    }

    private static final Map<UUID, Zone> ACTIVE = new HashMap<>();
    private static boolean spawning;

    private static void open(ServerPlayer player, ServerLevel level, BlockPos pos, double radius) {
        long now = level.getGameTime();
        Zone z = ACTIVE.get(player.getUUID());
        if (z == null || z.tick != now || z.level != level) {
            z = new Zone(now, level);
            ACTIVE.put(player.getUUID(), z);
        }
        z.areas.add(new double[]{pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, radius * radius});
    }

    /** Breaking a ripe plant. Runs first so the zone exists before the block drops its items. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || player.isCreative()) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        BlockState state = event.getState();
        if (isBlacklisted(state)) return;
        if (!isHarvestable(state)) return;
        open(player, level, event.getPos(), BREAK_RADIUS);
    }

    /** Right-click harvest (berries, tomatoes, scythes, sickles...). Nothing happens unless something actually drops. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (event.getUseBlock() == Event.Result.DENY && event.getUseItem() == Event.Result.DENY) return;
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isCreative()) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (isBlacklisted(level.getBlockState(event.getPos()))) return;
        open(player, level, event.getPos(), CLICK_RADIUS);
    }

    @SubscribeEvent
    public static void onItemJoin(EntityJoinLevelEvent event) {
        if (spawning || ACTIVE.isEmpty() || event.loadedFromDisk()) return;
        if (!(event.getEntity() instanceof ItemEntity drop) || !(event.getLevel() instanceof ServerLevel level)) return;
        ItemStack stack = drop.getItem();
        if (stack.isEmpty() || !WaifingConfig.isHarvestItem(stack)) return;

        long now = level.getGameTime();
        Iterator<Map.Entry<UUID, Zone>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Zone> en = it.next();
            Zone z = en.getValue();
            if (z.tick < now) { it.remove(); continue; } // stale
            if (z.tick != now || z.level != level || !z.contains(drop.getX(), drop.getY(), drop.getZ())) continue;

            ServerPlayer player = level.getServer().getPlayerList().getPlayer(en.getKey());
            if (player == null) continue;
            String key = drop.blockPosition().asLong() + "|" + ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (!z.done.add(key)) return;

            int extra = ChanceUtil.rolls(player.getAttributeValue(ModAttributes.FARMER_HARVEST_AMOUNT.get()),
                    player.getRandom(), 5);
            if (extra > 0) spawn(level, drop.getX(), drop.getY(), drop.getZ(), stack, extra);
            return;
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ACTIVE.remove(event.getEntity().getUUID());
    }

    private static boolean isBlacklisted(BlockState state) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return id != null && WaifingConfig.isBlockBlacklisted(id);
    }

    private static boolean isHarvestable(BlockState state) {
        if (state.getBlock() instanceof CropBlock crop) return crop.isMaxAge(state);
        for (Property<?> prop : state.getProperties()) {
            if (prop instanceof IntegerProperty ip && ip.getName().equals("age")) {
                return state.getValue(ip) >= Collections.max(ip.getPossibleValues());
            }
        }
        return false; // only fully grown plants count: no seed duplication from young crops
    }

    /** Extra units are exact copies of the dropped stack, so NBT added by other mods (e.g. Quality Food's quality) is kept. */
    private static void spawn(ServerLevel level, double x, double y, double z, ItemStack template, int count) {
        int left = count;
        int stackSize = template.getMaxStackSize();
        spawning = true; // the extra item entities must not trigger the bonus again
        try {
            while (left > 0) {
                int n = Math.min(left, stackSize);
                ItemStack copy = template.copy();
                copy.setCount(n);
                ItemEntity extra = new ItemEntity(level, x, y, z, copy);
                extra.setDefaultPickUpDelay();
                level.addFreshEntity(extra);
                left -= n;
            }
        } finally {
            spawning = false;
        }
    }

    private MoreCropsHandler() {}
}
