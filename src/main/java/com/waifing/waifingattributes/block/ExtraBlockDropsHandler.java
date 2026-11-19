package com.waifing.waifingattributes.block;

import com.waifing.waifingattributes.ModAttributes;
import com.waifing.waifingattributes.WaifingAttributesMod;
import com.waifing.waifingattributes.config.WaifingConfig;
import com.waifing.waifingattributes.util.ChanceUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Extra block drops when a player breaks a block with the right tool (no ticks: one event per broken block).
 *  miner_ore_amount      (AMOUNT, max 5): extra rolls of the ore's own drops (fortune applies). Not with Silk Touch (no ore duplication).
 *  lumberjack_log_amount (AMOUNT, max 5): extra rolls of the log's drops.
 *  lumberjack_sapling    (PERCENT): chance of one extra sapling when breaking leaves (the sapling the leaves themselves drop).
 * Each extra roll asks the block's loot table again, so modded ores and logs drop what they really drop.
 */
@Mod.EventBusSubscriber(modid = WaifingAttributesMod.MODID)
public final class ExtraBlockDropsHandler {
    private static final int SAPLING_TRIES = 32;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled()) return;
        if (!(event.getPlayer() instanceof ServerPlayer p) || p instanceof FakePlayer || p.isCreative()) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        BlockState state = event.getState();
        BlockPos pos = event.getPos();
        ItemStack tool = p.getMainHandItem();

        if (state.is(BlockTags.LEAVES)) {
            if (ChanceUtil.roll(p.getAttributeValue(ModAttributes.LUMBERJACK_SAPLING.get()), p.getRandom())) sapling(level, pos, state, p);
            return;
        }
        if (!p.hasCorrectToolForDrops(state)) return;
        double attr;
        if (matches(state, ores())) {
            if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) > 0) return;
            attr = p.getAttributeValue(ModAttributes.MINER_ORE_AMOUNT.get());
        } else if (matches(state, logs())) {
            attr = p.getAttributeValue(ModAttributes.LUMBERJACK_LOG_AMOUNT.get());
        } else {
            return;
        }
        int rolls = ChanceUtil.rolls(attr, p.getRandom(), 5);
        if (rolls <= 0) return;
        BlockEntity be = level.getBlockEntity(pos);
        for (int i = 0; i < rolls; i++) {
            for (ItemStack s : Block.getDrops(state, level, pos, be, p, tool)) Block.popResource(level, pos, s);
        }
    }

    private static void sapling(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer p) {
        for (int i = 0; i < SAPLING_TRIES; i++) {
            for (ItemStack s : Block.getDrops(state, level, pos, null, p, ItemStack.EMPTY)) {
                if (s.is(ItemTags.SAPLINGS)) {
                    s.setCount(1);
                    Block.popResource(level, pos, s);
                    return;
                }
            }
        }
    }

    // ------------------------------------------------------------------ block lists (ids or #tags), parsed once per config change
    private record Matcher(Set<ResourceLocation> ids, List<TagKey<Block>> tags) {}

    private static List<? extends String> oreRaw, logRaw;
    private static Matcher ores, logs;

    private static Matcher ores() {
        List<? extends String> raw = WaifingConfig.MINER_BLOCKS.get();
        if (raw != oreRaw) { ores = parse(raw); oreRaw = raw; }
        return ores;
    }

    private static Matcher logs() {
        List<? extends String> raw = WaifingConfig.LUMBER_BLOCKS.get();
        if (raw != logRaw) { logs = parse(raw); logRaw = raw; }
        return logs;
    }

    private static Matcher parse(List<? extends String> raw) {
        Set<ResourceLocation> ids = new HashSet<>();
        List<TagKey<Block>> tags = new ArrayList<>();
        for (String s : raw) {
            String t = s.trim();
            boolean tag = t.startsWith("#");
            ResourceLocation rl = ResourceLocation.tryParse(tag ? t.substring(1) : t);
            if (rl == null) continue;
            if (tag) tags.add(TagKey.create(Registries.BLOCK, rl));
            else ids.add(rl);
        }
        return new Matcher(ids, tags);
    }

    private static boolean matches(BlockState state, Matcher m) {
        for (TagKey<Block> t : m.tags()) if (state.is(t)) return true;
        return !m.ids().isEmpty() && m.ids().contains(ForgeRegistries.BLOCKS.getKey(state.getBlock()));
    }

    private ExtraBlockDropsHandler() {}
}
