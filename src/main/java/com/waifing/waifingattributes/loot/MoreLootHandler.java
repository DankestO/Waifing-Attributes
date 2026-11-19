package com.waifing.waifingattributes.loot;

import com.waifing.waifingattributes.ModAttributes;
import com.waifing.waifingattributes.WaifingAttributesMod;
import com.waifing.waifingattributes.compat.QualityBoost;
import com.waifing.waifingattributes.config.WaifingConfig;
import com.waifing.waifingattributes.util.ChanceUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * hunter: al matar un mob, tiradas de botin adicionales.
 * 1.0 = 1 tirada extra garantizada (equivale a matar 2 mobs); 0.5 = 50% de 1 tirada extra.
 */
@Mod.EventBusSubscriber(modid = WaifingAttributesMod.MODID)
public final class MoreLootHandler {
    private static final String QUALITY_MARK = "waifing_hq:";

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level)) return;
        if (victim instanceof Player) return;
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof ServerPlayer killer)) return;

        ResourceLocation typeId = ForgeRegistries.ENTITY_TYPES.getKey(victim.getType());
        if (typeId != null && WaifingConfig.isEntityBlacklisted(typeId)) return;

        double value = killer.getAttributeValue(ModAttributes.HUNTER_LOOT_AMOUNT.get());
        int rolls = ChanceUtil.rolls(value, killer.getRandom(), 5);
        if (rolls > 0) {
            try {
                if (WaifingConfig.LOOT_USE_LOOT_TABLE.get()) {
                    rollLootTable(event, victim, level, source, killer, rolls);
                } else {
                    cloneDrops(event, victim, level, rolls);
                }
            } catch (Exception ex) {
                WaifingAttributesMod.LOGGER.error("[hunter] Error generando botin extra para {}", typeId, ex);
            }
        }

        // Quality Food assigns its quality AFTER this handler (it runs last), so the drops are only marked here and
        // their quality is raised when they enter the world (see onItemJoin).
        if (ChanceUtil.levels(killer.getAttributeValue(ModAttributes.HUNTER_LOOT_QUALITY.get())) > 0 && QualityBoost.present()) {
            String mark = QUALITY_MARK + killer.getUUID();
            for (ItemEntity ie : event.getDrops()) ie.addTag(mark);
        }
    }

    /** The marked drops of a Hunter kill enter the world: now they already carry Quality Food's quality. */
    @SubscribeEvent
    public static void onItemJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || !(event.getEntity() instanceof ItemEntity drop) || drop.getTags().isEmpty()) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        String mark = null;
        for (String t : drop.getTags()) if (t.startsWith(QUALITY_MARK)) { mark = t; break; }
        if (mark == null) return;
        drop.removeTag(mark);
        UUID id;
        try {
            id = UUID.fromString(mark.substring(QUALITY_MARK.length()));
        } catch (IllegalArgumentException e) {
            return;
        }
        ServerPlayer killer = level.getServer().getPlayerList().getPlayer(id);
        if (killer == null) return;

        QualityBoost.raiseBy(drop.getItem(), ChanceUtil.levels(killer.getAttributeValue(ModAttributes.HUNTER_LOOT_QUALITY.get())));
    }

    private static void rollLootTable(LivingDropsEvent event, LivingEntity victim, ServerLevel level,
                                      DamageSource source, ServerPlayer killer, int rolls) {
        ResourceLocation tableId = victim.getLootTable();
        if (tableId == BuiltInLootTables.EMPTY) return;
        LootTable table = level.getServer().getLootData().getLootTable(tableId);

        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, victim)
                .withParameter(LootContextParams.ORIGIN, victim.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, source)
                .withOptionalParameter(LootContextParams.KILLER_ENTITY, source.getEntity())
                .withOptionalParameter(LootContextParams.DIRECT_KILLER_ENTITY, source.getDirectEntity())
                .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, killer)
                .withLuck(killer.getLuck())
                .create(LootContextParamSets.ENTITY);

        for (int i = 0; i < rolls; i++) {
            for (ItemStack stack : table.getRandomItems(params)) {
                event.getDrops().add(newDrop(level, victim, stack));
            }
        }
    }

    private static void cloneDrops(LivingDropsEvent event, LivingEntity victim, ServerLevel level, int rolls) {
        List<ItemStack> snapshot = new ArrayList<>();
        for (ItemEntity ie : event.getDrops()) snapshot.add(ie.getItem().copy());
        for (int i = 0; i < rolls; i++) {
            for (ItemStack s : snapshot) event.getDrops().add(newDrop(level, victim, s.copy()));
        }
    }

    private static ItemEntity newDrop(ServerLevel level, LivingEntity victim, ItemStack stack) {
        ItemEntity ie = new ItemEntity(level, victim.getX(), victim.getY(), victim.getZ(), stack);
        ie.setDefaultPickUpDelay();
        return ie;
    }

    private MoreLootHandler() {}
}
