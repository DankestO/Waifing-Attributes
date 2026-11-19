package com.waifing.waifingattributes;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * One attribute per ability (47), grouped by mastery in the name: hunter_, fisherman_, farmer_, chef_, alchemist_,
 * blacksmith_, enchanter_, mage_, rogue_, miner_, lumberjack_, tamer_, merchant_, explorer_. Base value 0.0: a skill mod or /attribute raises them.
 * Kinds: SWITCH (above 0 = on), AMOUNT (quantity/levels), PERCENT (0.01 to 1.0 = 1% to 100%), TIER CHANCE (see ChanceUtil.tier).
 */
public final class ModAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(ForgeRegistries.ATTRIBUTES, WaifingAttributesMod.MODID);

    // ---- Hunter
    public static final RegistryObject<Attribute> HUNTER_LOOT_AMOUNT = reg("hunter_loot_amount", 5.0D);
    public static final RegistryObject<Attribute> HUNTER_LOOT_QUALITY = reg("hunter_loot_quality", 3.0D);
    public static final RegistryObject<Attribute> HUNTER_STEALTH = reg("hunter_stealth", 1.0D);
    public static final RegistryObject<Attribute> HUNTER_AMBUSH = reg("hunter_ambush", 4.0D);
    // ---- Fisherman
    public static final RegistryObject<Attribute> FISHERMAN_CATCH_AMOUNT = reg("fisherman_catch_amount", 5.0D);
    public static final RegistryObject<Attribute> FISHERMAN_CATCH_QUALITY = reg("fisherman_catch_quality", 3.0D);
    // ---- Farmer
    public static final RegistryObject<Attribute> FARMER_HARVEST_AMOUNT = reg("farmer_harvest_amount", 5.0D);
    public static final RegistryObject<Attribute> FARMER_BONEMEAL_KEEP = reg("farmer_bonemeal_keep", 1.0D);
    public static final RegistryObject<Attribute> FARMER_BREED_TWINS = reg("farmer_breed_twins", 1.0D);
    public static final RegistryObject<Attribute> FARMER_FEED_KEEP = reg("farmer_feed_keep", 1.0D);
    // ---- Chef
    public static final RegistryObject<Attribute> CHEF_COOKING_QUALITY = reg("chef_cooking_quality", 3.0D);
    public static final RegistryObject<Attribute> CHEF_IMBUE_FOOD = reg("chef_imbue_food", 1.0D);
    // ---- Alchemist
    public static final RegistryObject<Attribute> ALCHEMIST_BREWING = reg("alchemist_brewing", 1.0D);
    public static final RegistryObject<Attribute> ALCHEMIST_POTION_UPGRADE = reg("alchemist_potion_upgrade", 10.0D);
    public static final RegistryObject<Attribute> ALCHEMIST_POTION_REBIRTH = reg("alchemist_potion_rebirth", 1.0D);
    public static final RegistryObject<Attribute> ALCHEMIST_KEEP_CATALYST = reg("alchemist_keep_catalyst", 1.0D);
    // ---- Blacksmith
    public static final RegistryObject<Attribute> BLACKSMITH_UPGRADE = reg("blacksmith_upgrade", 10.0D);
    public static final RegistryObject<Attribute> BLACKSMITH_REBIRTH = reg("blacksmith_rebirth", 1.0D);
    public static final RegistryObject<Attribute> BLACKSMITH_KEEP_CATALYST = reg("blacksmith_keep_catalyst", 1.0D);
    public static final RegistryObject<Attribute> BLACKSMITH_GEM_SOCKET = reg("blacksmith_gem_socket", 5.0D);
    public static final RegistryObject<Attribute> BLACKSMITH_GEM_QUALITY = reg("blacksmith_gem_quality", 1.0D);
    public static final RegistryObject<Attribute> BLACKSMITH_IMBUE_WEAPON_DURATION = reg("blacksmith_imbue_weapon_duration", 1.0D);
    public static final RegistryObject<Attribute> BLACKSMITH_IMBUE_WEAPON_HITS = reg("blacksmith_imbue_weapon_hits", 1.0D);
    // ---- Enchanter
    public static final RegistryObject<Attribute> ENCHANTER_BOOK_REBIRTH = reg("enchanter_book_rebirth", 1.0D);
    public static final RegistryObject<Attribute> ENCHANTER_APPLY_BOOK = reg("enchanter_apply_book", 1.0D);
    public static final RegistryObject<Attribute> ENCHANTER_MERGE_BOOKS = reg("enchanter_merge_books", 1.0D);
    public static final RegistryObject<Attribute> ENCHANTER_SPLIT_BOOKS = reg("enchanter_split_books", 1.0D);
    public static final RegistryObject<Attribute> ENCHANTER_EXTRACT_BOOK = reg("enchanter_extract_book", 1.0D);
    public static final RegistryObject<Attribute> ENCHANTER_KEEP_BOOK = reg("enchanter_keep_book", 1.0D);
    public static final RegistryObject<Attribute> ENCHANTER_TABLE_XP = reg("enchanter_table_xp", 1.0D);
    public static final RegistryObject<Attribute> ENCHANTER_TABLE_LAPIS = reg("enchanter_table_lapis", 1.0D);
    // ---- Mage (Iron's Spellbooks)
    public static final RegistryObject<Attribute> MAGE_SCROLL_MERGE = reg("mage_scroll_merge", 1.0D);
    public static final RegistryObject<Attribute> MAGE_SCROLL_SPLIT = reg("mage_scroll_split", 1.0D);
    public static final RegistryObject<Attribute> MAGE_KEEP_SCROLL = reg("mage_keep_scroll", 1.0D);
    public static final RegistryObject<Attribute> MAGE_ARCANE_ANVIL = reg("mage_arcane_anvil", 1.0D);

    // ---- Rogue
    public static final RegistryObject<Attribute> ROGUE_BACKSTAB = reg("rogue_backstab", 1.0D);
    public static final RegistryObject<Attribute> ROGUE_NIGHTKIN = reg("rogue_nightkin", 2.0D);
    public static final RegistryObject<Attribute> ROGUE_PICKPOCKET = reg("rogue_pickpocket", 1.0D);
    // ---- Miner
    public static final RegistryObject<Attribute> MINER_ORE_AMOUNT = reg("miner_ore_amount", 5.0D);
    // ---- Lumberjack
    public static final RegistryObject<Attribute> LUMBERJACK_LOG_AMOUNT = reg("lumberjack_log_amount", 5.0D);
    public static final RegistryObject<Attribute> LUMBERJACK_SAPLING = reg("lumberjack_sapling", 1.0D);
    // ---- Tamer
    public static final RegistryObject<Attribute> TAMER_PET_DAMAGE = reg("tamer_pet_damage", 3.0D);
    // ---- Merchant
    public static final RegistryObject<Attribute> MERCHANT_HAGGLE = reg("merchant_haggle", 1.0D);
    public static final RegistryObject<Attribute> MERCHANT_XP = reg("merchant_xp", 1.0D);
    public static final RegistryObject<Attribute> MERCHANT_RESTOCK = reg("merchant_restock", 1.0D);
    public static final RegistryObject<Attribute> MERCHANT_VILLAGER_XP = reg("merchant_villager_xp", 1.0D);
    // ---- Explorer
    public static final RegistryObject<Attribute> EXPLORER_FALL = reg("explorer_fall", 1.0D);

    /** Upgrade levels of equipment and of potions with duration. alchemist_potion_upgrade and blacksmith_upgrade are capped at this value. */
    public static final int MAX_UPGRADE_LEVELS = 10;
    /** Upgrade levels of potions WITHOUT duration (healing, harming...); the Rebirth is the step after the last one. */
    public static final int INSTANT_POTION_LEVELS = 1;

    private static RegistryObject<Attribute> reg(String name) {
        return reg(name, 1024.0D);
    }

    private static RegistryObject<Attribute> reg(String name, double max) {
        return ATTRIBUTES.register(name, () ->
                new RangedAttribute("attribute.name." + WaifingAttributesMod.MODID + "." + name, 0.0D, 0.0D, max)
                        .setSyncable(true));
    }

    public static void onAttributeModification(EntityAttributeModificationEvent event) {
        for (RegistryObject<Attribute> a : ATTRIBUTES.getEntries()) {
            event.add(EntityType.PLAYER, a.get());
        }
    }

    private ModAttributes() {}
}
