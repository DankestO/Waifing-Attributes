package com.waifing.waifingattributes.config;

import com.waifing.waifingattributes.WaifingAttributesMod;
import com.waifing.waifingattributes.upgrade.UpgradeCategory;
import com.waifing.waifingattributes.util.ChanceUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeConfigSpec;

import javax.annotation.Nullable;
import java.util.*;

/**
 * SERVER configuration (serverconfig/waifingattributes-server.toml, inside each world; for a dedicated server it is the one
 * in the world folder). Forge synchronizes it to every client that joins, so players never need the same file as the server
 * (the client only uses it for tooltips, the guide book and to cancel the vanilla item swap when a click is an upgrade).
 * Client-only options (tooltips) live in {@link WaifingClientConfig}.
 * NEVER read these values before a world is loaded (main menu, mod constructors): Forge has not loaded them yet.
 *
 * Every item field accepts "modid:item" or "#modid:tag". Mods that are not installed are simply ignored,
 * so every entry is optional: nothing here makes another mod mandatory.
 */
public final class WaifingConfig {
    public static final ForgeConfigSpec SPEC;

    // ---- general
    public static final ForgeConfigSpec.DoubleValue SOUND_VOLUME;
    public static final ForgeConfigSpec.ConfigValue<String> REBIRTH_OK_SOUND;
    public static final ForgeConfigSpec.ConfigValue<String> DENIED_SOUND;
    public static final ForgeConfigSpec.BooleanValue CONSUME_CATALYST_ON_FAIL;

    // ---- hunter
    public static final ForgeConfigSpec.BooleanValue LOOT_USE_LOOT_TABLE;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> LOOT_ENTITY_BLACKLIST;

    // ---- rogue (nightkin 2.0)
    public static final ForgeConfigSpec.BooleanValue NIGHTKIN_SUN_BURN;
    public static final ForgeConfigSpec.IntValue NIGHTKIN_SUN_WEAKNESS;
    public static final ForgeConfigSpec.IntValue NIGHTKIN_ALLY_RADIUS;
    public static final ForgeConfigSpec.IntValue NIGHTKIN_ALLY_MAX;
    public static final ForgeConfigSpec.IntValue NIGHTKIN_ALLY_COOLDOWN;
    public static final ForgeConfigSpec.BooleanValue NIGHTKIN_NO_HEAL_VULNERABLE;
    public static final ForgeConfigSpec.IntValue NIGHTKIN_FIRE_VULNERABLE;

    // ---- miner / lumberjack
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> MINER_BLOCKS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> LUMBER_BLOCKS;

    // ---- fisherman
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> FISH_ITEMS;

    // ---- farmer / chef
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CROPS_ITEMS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CROPS_BLOCK_BLACKLIST;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> GASTRO_CRAFTED_FOODS;

    // ---- alchemist
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> POTION_CATALYSTS;
    public static final ForgeConfigSpec.DoubleValue POTION_DURATION_STEP;
    public static final ForgeConfigSpec.BooleanValue POTION_REQUIRE_VANILLA_MAX;
    public static final ForgeConfigSpec.DoubleValue POTION_TIER_FALLOFF;
    public static final ForgeConfigSpec.IntValue POTION_MAX_AMPLIFIER;
    public static final ForgeConfigSpec.IntValue POTION_MAX_DURATION;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> POTION_NO_LEVEL_EFFECTS;
    public static final ForgeConfigSpec.BooleanValue POTION_DESTROY_ON_FAIL;
    public static final ForgeConfigSpec.ConfigValue<String> POTION_OK_SOUND;
    public static final ForgeConfigSpec.ConfigValue<String> POTION_FAIL_SOUND;
    public static final ForgeConfigSpec.BooleanValue POTION_REBIRTH_DESTROY;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> POTION_REBIRTH_CATALYSTS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> IMBUE_TARGETS;
    public static final ForgeConfigSpec.BooleanValue IMBUE_AUTO_MELEE;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> IMBUE_BLACKLIST;
    public static final ForgeConfigSpec.IntValue IMBUE_HITS;
    public static final ForgeConfigSpec.BooleanValue IMBUE_RETURN_BOTTLE;

    // ---- blacksmith
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> EQUIP_CATALYSTS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> EQUIP_MELEE_ITEMS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> EQUIP_RANGED_ITEMS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> EQUIP_ARMOR_ITEMS;
    public static final ForgeConfigSpec.DoubleValue EQUIP_TIER_FALLOFF;
    public static final ForgeConfigSpec.BooleanValue EQUIP_DESTROY_ON_FAIL;
    public static final ForgeConfigSpec.ConfigValue<String> EQUIP_OK_SOUND;
    public static final ForgeConfigSpec.ConfigValue<String> EQUIP_FAIL_SOUND;
    public static final ForgeConfigSpec.IntValue REBIRTH_MAX;
    public static final ForgeConfigSpec.BooleanValue REBIRTH_DESTROY_ON_FAIL;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> REBIRTH_CATALYSTS;

    // ---- enchanter
    public static final ForgeConfigSpec.IntValue BOOK_HARD_CAP;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BOOK_BLACKLIST;
    public static final ForgeConfigSpec.ConfigValue<String> BOOK_OK_SOUND;
    public static final ForgeConfigSpec.ConfigValue<String> BOOK_FAIL_SOUND;
    public static final ForgeConfigSpec.IntValue BOOK_REBIRTH_MAX_ABOVE;
    public static final ForgeConfigSpec.BooleanValue BOOK_REBIRTH_DESTROY;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BOOK_REBIRTH_CATALYSTS;
    public static final ForgeConfigSpec.DoubleValue BOOK_REBIRTH_CURSE_CHANCE;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BOOK_CURSE_BLACKLIST;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SPLIT_ITEMS;

    // ---- mage (Iron's Spellbooks)
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SCROLL_SPLIT_ITEMS;
    public static final ForgeConfigSpec.BooleanValue ARCANE_REPLACE_FULL;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("Global options of the upgrade mechanic.",
                        "HOW IT WORKS: every ability is its own attribute (waifingattributes:<name>), so a skill tree can hand them out one by one.",
                        "Four kinds of attribute exist:",
                        "  SWITCH      any value above 0 turns the ability on (use 1.0 by convention).",
                        "  AMOUNT      the value is a quantity (1.0 = one extra roll, one extra copy, one quality level...).",
                        "  PERCENT     0.01 = 1% ... 1.0 = 100% chance (values above 1.0 count as 100%).",
                        "  TIER CHANCE the upgrade chances of the 'tier model': attribute 1.0 = level 1 at 100%, level 2 at 50%, level 3 at 25%...",
                        "HOW TO USE: pick the catalyst up with the cursor in your inventory and RIGHT-click the potion, enchanted book,",
                        "weapon, tool, armor piece or shield (shields count as armor). Without the matching attribute the click is ignored.")
                .push("general");
        SOUND_VOLUME = b.comment("Volume of this mod's sounds relative to the player's own Minecraft sound settings (0.5 = half).")
                .defineInRange("soundVolume", 0.5D, 0.0D, 1.0D);
        REBIRTH_OK_SOUND = b.comment("Sound played when a Rebirth succeeds (potions, books and equipment).")
                .define("rebirthSuccessSound", "minecraft:ui.toast.challenge_complete");
        DENIED_SOUND = b.comment("Short sound when the click cannot upgrade anything (already at the maximum, wrong catalyst for this step...).")
                .define("deniedSound", "minecraft:entity.villager.no");
        CONSUME_CATALYST_ON_FAIL = b.comment("Also consume the catalyst when the attempt fails.")
                .define("consumeCatalystOnFail", true);
        b.pop();

        b.comment("HUNTER. Attributes: hunter_loot_amount (AMOUNT: extra loot rolls) and hunter_loot_quality (AMOUNT: Quality Food levels, 1.0 = +1 level, never above diamond).")
                .push("hunter");
        LOOT_USE_LOOT_TABLE = b.comment("true = real extra rolls on the mob's loot table. false = duplicate the drops that already dropped (works with modded mobs that drop items from code).")
                .define("useLootTable", true);
        LOOT_ENTITY_BLACKLIST = b.comment("Entities that do NOT get extra loot or quality. Example: [\"minecraft:ender_dragon\"]")
                .defineListAllowEmpty("entityBlacklist", List.of(), o -> o instanceof String);
        b.pop();

        b.comment("FISHERMAN. Attributes: fisherman_catch_amount (AMOUNT: extra copies of each fish) and fisherman_catch_quality (AMOUNT: Quality Food",
                        "levels ADDED to the quality the catch already has, with or without Stardew Fishing; never above diamond).")
                .push("fisherman");
        FISH_ITEMS = b.comment("Items/#tags that count as 'fish' for the extra copies and the quality (the treasure chest of Stardew Fishing and junk are never touched).")
                .defineListAllowEmpty("fishItems", List.of("#minecraft:fishes", "#forge:raw_fishes", "#forge:fishes"), o -> o instanceof String);
        b.pop();

        b.comment("FARMER. Attributes:",
                        "  farmer_harvest_amount   AMOUNT: extra harvest units on every harvest-item drop that appears while a player breaks a crop OR right-clicks to",
                        "                          harvest (berry bushes, Kaleidoscope Cookery tomatoes/chili, Tinkers' Construct scythes, Kaleidoscope Cookery sickles...).",
                        "  farmer_bonemeal_keep    PERCENT: chance of NOT spending the bone meal when a player uses it on a plant that accepts it.")
                .push("farmer");
        CROPS_ITEMS = b.comment("Item TAGS (#modid:tag) that count as harvest; single items also work. Only drops matching this list are multiplied.")
                .defineListAllowEmpty("harvestItems", List.of("#forge:crops", "#forge:berries", "#forge:fruits", "#forge:vegetables"),
                        o -> o instanceof String);
        CROPS_BLOCK_BLACKLIST = b.comment("Blocks that do NOT give extra harvest.")
                .defineListAllowEmpty("blockBlacklist", List.of(), o -> o instanceof String);
        b.pop();

        b.comment("CHEF. Attributes:",
                        "  chef_cooking_quality AMOUNT: Quality Food levels for food you COOK WITH FIRE: when you take it out of a furnace, smoker or blast furnace, or",
                        "                       interact with a finished dish of Kaleidoscope Cookery's pot, stockpot, steamer or shawarma spit. Never when crafting or cutting.",
                        "  chef_imbue_food      PERCENT: potion on ONE food item of a stack (1:1); the eater gets that fraction of the duration (0.01 = 1% ... 1.0 = 100%).")
                .push("chef");
        GASTRO_CRAFTED_FOODS = b.comment("Foods that are FINISHED the moment they are crafted (sushi...): they get the same cooking quality when you take them from the crafting grid.",
                        "Items or #tags. Empty by default: only list ready-to-eat foods, so multi-step dishes cannot climb by crafting.")
                .defineListAllowEmpty("craftedFoods", List.of(), o -> o instanceof String);
        b.pop();

        b.comment("ALCHEMIST. Attributes:",
                        "  alchemist_brewing        SWITCH: ingredient + RIGHT-click a potion brews it like a brewing stand (any recipe of vanilla or other mods, one ingredient converts the whole stack).",
                        "  alchemist_potion_upgrade TIER CHANCE: catalyst upgrades (attribute capped at 10.0). They start where vanilla ends: +10% of the base duration per level (10 levels), or potency in instant potions (1 level, then the Rebirth).",
                        "  alchemist_potion_rebirth PERCENT: Rebirth (resets the counter and raises the potency one level, duration kept; instant potions too).",
                        "  alchemist_keep_catalyst  PERCENT: chance of NOT spending the catalyst of an upgrade or Rebirth.")
                .push("alchemist");
        POTION_CATALYSTS = b.comment("Items/#tags used as catalyst to upgrade potions (duration, or potency in instant potions).")
                .defineListAllowEmpty("catalysts", List.of("minecraft:lapis_lazuli"), o -> o instanceof String);
        POTION_DURATION_STEP = b.comment("Each duration level adds this fraction of the BASE duration (0.10 = +10%; 10 levels = twice the duration).")
                .defineInRange("durationStep", 0.10D, 0.0D, 10.0D);
        POTION_REQUIRE_VANILLA_MAX = b.comment("The catalyst only works once vanilla cannot improve the potion any more (it already is the long_ or strong_ version, or it has none).")
                .define("requireVanillaMax", true);
        POTION_TIER_FALLOFF = b.comment("Chance factor per level above the mastered one (attribute 1.0 = level 1 at 100%, level 2 at 50%...; attribute 10.0 = every level at 100%).")
                .defineInRange("tierFalloff", 0.5D, 0.0D, 1.0D);
        POTION_MAX_AMPLIFIER = b.comment("Highest amplifier allowed (127 = level 128; vanilla stores it in a byte).")
                .defineInRange("maxAmplifier", 127, 0, 127);
        POTION_MAX_DURATION = b.comment("Maximum duration in ticks (72000 = 1 hour).")
                .defineInRange("maxDurationTicks", 72000, 20, Integer.MAX_VALUE);
        POTION_NO_LEVEL_EFFECTS = b.comment("Effects that do not scale with the amplifier: a Rebirth does not raise them (a potion made only of these just resets its counter).")
                .defineListAllowEmpty("noLevelEffects", List.of("minecraft:fire_resistance", "minecraft:night_vision", "minecraft:water_breathing",
                        "minecraft:invisibility", "minecraft:slow_falling", "minecraft:glowing"), o -> o instanceof String);
        POTION_DESTROY_ON_FAIL = b.comment("Destroy the potion (the whole stack) when an upgrade fails. If false only the catalyst is spent.")
                .define("destroyOnFail", true);
        POTION_OK_SOUND = b.comment("Sound of a brewed or upgraded potion.").define("successSound", "minecraft:block.brewing_stand.brew");
        POTION_FAIL_SOUND = b.define("failSound", "minecraft:entity.item.break");
        b.comment("Rebirth: the chance is the value of alchemist_potion_rebirth (0.01 = 1% ... 1.0 = 100%).").push("rebirth");
        POTION_REBIRTH_DESTROY = b.comment("Destroy (explode) the potion when the rebirth fails. If false only the catalyst is spent.")
                .define("destroyOnFail", true);
        POTION_REBIRTH_CATALYSTS = b.comment("Items/#tags that are the Rebirth catalyst. REQUIRED: the Rebirth only works with these (empty = no Rebirth).")
                .defineListAllowEmpty("catalysts", List.of("minecraft:gold_ingot"), o -> o instanceof String);
        b.pop();
        b.pop();

        b.comment("BLACKSMITH. Attributes:",
                        "  blacksmith_upgrade        TIER CHANCE: upgrade weapons, tools and armor, levels 1 to 10. The attribute is capped at 10.0 (every level at 100%).",
                        "  blacksmith_rebirth        PERCENT: at max level, resets the upgrade slots and keeps every bonus.",
                        "  blacksmith_gem_socket    AMOUNT (max 5.0): Apotheosis gem on a piece of equipment; the item may end with up to that many sockets even if it has none.",
                        "  blacksmith_gem_quality    PERCENT (max 1.0): the attribute bonuses of the gems you socket are multiplied by 1 + value (0.5 = +50%). Stored on the item per gem, the gem is not changed.",
                        "  blacksmith_keep_catalyst  PERCENT: chance of NOT spending the catalyst of an upgrade or Rebirth.",
                        "  blacksmith_imbue_weapon_duration PERCENT: potion on a weapon; fraction of the potion's duration each hit applies (0.01 = 1% ... 1.0 = 100%).",
                        "  blacksmith_imbue_weapon_hits     PERCENT: hits the weapon keeps (0.01 = 1 hit ... 1.0 = 100 hits, see hitsAtMax).")
                .push("blacksmith");
        EQUIP_CATALYSTS = b.comment("Upgrade: item_or_#tag;MELEE|RANGED|ARMOR|ALL;attribute;amount_per_level;ADDITION|MULTIPLY_BASE|MULTIPLY_TOTAL ('durability' = extra max durability).",
                        "Repair: item_or_#tag;REPAIR;fraction (0.05 = 5% of the max durability). Same chance as the item's upgrade level; failing only spends the catalyst.")
                .defineListAllowEmpty("catalysts", List.of(
                        "minecraft:iron_ingot;MELEE;minecraft:generic.attack_damage;0.5;ADDITION",
                        "minecraft:iron_ingot;ARMOR;minecraft:generic.armor;0.2;ADDITION",
                        "minecraft:copper_ingot;ALL;durability;100;ADDITION",
                        "minecraft:experience_bottle;REPAIR;0.05"),
                        o -> o instanceof String);
        EQUIP_MELEE_ITEMS = b.comment("Items/#tags forced into the MELEE category.")
                .defineListAllowEmpty("forceMelee", List.of(), o -> o instanceof String);
        EQUIP_RANGED_ITEMS = b.comment("Items/#tags forced into the RANGED category.")
                .defineListAllowEmpty("forceRanged", List.of(), o -> o instanceof String);
        EQUIP_ARMOR_ITEMS = b.comment("Items/#tags forced into the ARMOR category.")
                .defineListAllowEmpty("forceArmor", List.of(), o -> o instanceof String);
        EQUIP_TIER_FALLOFF = b.comment("Chance factor per level above the mastered one (0.5 = half per level).")
                .defineInRange("tierFalloff", 0.5D, 0.0D, 1.0D);
        EQUIP_DESTROY_ON_FAIL = b.comment("Destroy the equipment when a normal upgrade fails.")
                .define("destroyOnFail", true);
        EQUIP_OK_SOUND = b.define("successSound", "minecraft:block.anvil.use");
        EQUIP_FAIL_SOUND = b.define("failSound", "minecraft:block.anvil.destroy");
        b.comment("Imbue: pick a potion up with the cursor and RIGHT-click a melee weapon (its next hits apply the potion's effects) or a stack of arrows",
                        "(the whole stack becomes arrows with the potion's effects, potency kept, duration = potion duration x blacksmith_imbue_weapon_duration).").push("imbue");
        IMBUE_AUTO_MELEE = b.comment("Any item that deals melee damage (attack damage in the main hand) can be imbued: swords, axes, tools, Tinkers, modded weapons.")
                .define("anyMeleeWeapon", true);
        IMBUE_TARGETS = b.comment("Extra items/#tags that can be imbued even without melee damage.")
                .defineListAllowEmpty("targets", List.of("#minecraft:swords"), o -> o instanceof String);
        IMBUE_BLACKLIST = b.comment("Items/#tags that can never be imbued (weapons or arrows).")
                .defineListAllowEmpty("blacklist", List.of(), o -> o instanceof String);
        IMBUE_HITS = b.comment("Hits of an imbued weapon at blacksmith_imbue_weapon_hits = 1.0 (0.01 = 1 hit when this is 100; hits = attribute x this value, at least 1).",
                        "The duration of each hit is the potion's duration times blacksmith_imbue_weapon_duration (0.01 = 1% ... 1.0 = 100%). Both attributes are capped at 1.0 and both are needed.")
                .defineInRange("hitsAtMax", 100, 1, 10000);
        IMBUE_RETURN_BOTTLE = b.comment("Give back an empty glass bottle when the potion is spent (weapons and food).")
                .define("returnBottle", true);
        b.pop();
        b.comment("Rebirth: the chance is the value of blacksmith_rebirth (0.01 = 1% ... 1.0 = 100%).").push("rebirth");
        REBIRTH_MAX = b.comment("Maximum rebirths per item (0 = unlimited).")
                .defineInRange("maxPerItem", 0, 0, 1000);
        REBIRTH_DESTROY_ON_FAIL = b.comment("Destroy the equipment when the rebirth fails. If false only the catalyst is spent.")
                .define("destroyOnFail", true);
        REBIRTH_CATALYSTS = b.comment("Items/#tags that are the Rebirth catalyst. REQUIRED: the Rebirth only works with these (empty = no Rebirth).",
                        "Change it to whatever fits your server.")
                .defineListAllowEmpty("catalysts", List.of("minecraft:diamond"), o -> o instanceof String);
        b.pop();
        b.pop();

        b.comment("ENCHANTER. Attributes:",
                        "  enchanter_book_rebirth  PERCENT: raises an enchantment that is at its natural maximum above it (Rebirth catalyst needed).",
                        "  enchanter_apply_book    SWITCH: enchanted book + RIGHT-click on a tool, weapon, armor piece or shield enchants it.",
                        "  enchanter_merge_books   SWITCH: enchanted book onto another book. Different enchantments merge, higher levels win, two equal levels give +1 (up to the natural max). Always succeeds.",
                        "  enchanter_split_books   SWITCH: a normal book onto an enchanted book moves its LAST added enchantment to a new book.",
                        "  enchanter_keep_book     PERCENT: chance of NOT spending the book when it is applied to equipment or merged into another book.",
                        "  enchanter_table_xp      PERCENT: experience discount at the enchanting table (0.25 = 25% cheaper).",
                        "  enchanter_table_lapis   PERCENT: chance of NOT spending the lapis lazuli at the enchanting table.")
                .push("enchanter");
        BOOK_HARD_CAP = b.comment("Absolute maximum level of any enchantment, Rebirth included.")
                .defineInRange("hardCap", 10, 1, 255);
        BOOK_BLACKLIST = b.comment("Enchantments that cannot be touched (rebirth, merge, apply). Example: [\"minecraft:mending\"]")
                .defineListAllowEmpty("enchantBlacklist", List.of(), o -> o instanceof String);
        BOOK_OK_SOUND = b.comment("Sound of a successful book rebirth, merge, split or enchant.").define("successSound", "minecraft:block.enchantment_table.use");
        BOOK_FAIL_SOUND = b.define("failSound", "minecraft:entity.item.break");
        b.comment("Rebirth: raises enchantments that are already at their natural maximum above it (books whose enchantment has a maximum level of I cannot be rebirthed).",
                        "The chance is the value of enchanter_book_rebirth (0.01 = 1% ... 1.0 = 100%).").push("rebirth");
        BOOK_REBIRTH_MAX_ABOVE = b.comment("How many levels above its natural maximum an enchantment can be pushed (still limited by hardCap).")
                .defineInRange("maxLevelsAboveMax", 1, 1, 50);
        BOOK_REBIRTH_DESTROY = b.comment("Destroy the book when the rebirth fails. If false only the catalyst is spent.")
                .define("destroyOnFail", true);
        BOOK_REBIRTH_CATALYSTS = b.comment("Items/#tags that are the Rebirth catalyst. REQUIRED: the Rebirth only works with these (empty = no Rebirth).",
                        "Change it to whatever fits your server.")
                .defineListAllowEmpty("catalysts", List.of("minecraft:diamond"), o -> o instanceof String);
        BOOK_REBIRTH_CURSE_CHANCE = b.comment("When a Rebirth fails, chance that the book turns into a CURSED book (one random curse, from any mod) instead of just being lost/kept.")
                .defineInRange("curseChance", 0.5D, 0.0D, 1.0D);
        BOOK_CURSE_BLACKLIST = b.comment("Curses that never appear on a cursed book. Every other curse of the game (vanilla or modded) can.")
                .defineListAllowEmpty("curseBlacklist", List.of(), o -> o instanceof String);
        b.pop();

        b.comment("Split a book: items accepted as the 'normal book' that is put on the enchanted book.").push("splitBooks");
        SPLIT_ITEMS = b.defineListAllowEmpty("items", List.of("minecraft:book"), o -> o instanceof String);
        b.pop();

        b.pop(); // enchanter

        b.comment("MAGE (Iron's Spellbooks, optional mod). Attributes:",
                        "  mage_scroll_merge SWITCH: cursor scroll onto another of the same spell and level: level +1, always succeeds.",
                        "  mage_keep_scroll  PERCENT: chance of NOT spending the scroll when merging or using the Arcane Anvil.",
                        "  mage_scroll_split SWITCH: split a scroll of level L into levels L-1 and 1 with the items below.",
                        "  mage_arcane_anvil SWITCH: cursor scroll right-clicked onto an item adds its spell to the item (what Iron's Arcane Anvil does).")
                .push("mage");
        b.comment("Scrolls").push("scrolls");
        SCROLL_SPLIT_ITEMS = b.defineListAllowEmpty("splitItems", List.of("minecraft:paper"), o -> o instanceof String);
        b.pop();
        b.comment("Arcane Anvil: only items Iron's itself accepts for imbuing (its own whitelist/blacklist applies). The scroll is spent unless mage_keep_scroll saves it.").push("arcaneAnvil");
        ARCANE_REPLACE_FULL = b.comment("When the item has no free spell slot, replace its first spell. If false the click is denied.")
                .define("replaceWhenFull", false);
        b.pop();
        b.pop(); // mage

        b.comment("ROGUE. rogue_nightkin (AMOUNT, max 2.0): 1.0 = undead mobs ignore you (bosses and Iron's summons excluded; one you hurt can target you).",
                        "2.0 = you ARE undead (vanilla and Consecration treat you as one), undead help you, and the sun hurts you.")
                .push("rogue");
        NIGHTKIN_SUN_BURN = b.comment("Nightkin 2.0 in daylight (open sky, no rain): set on fire unless wearing a helmet. The helmet wears down through the normal",
                        "durability path, so mods that stop durability loss (e.g. Iron's RPG Tweaks) also protect it.")
                .define("sunBurn", true);
        NIGHTKIN_SUN_WEAKNESS = b.comment("Weakness level applied in daylight, helmet or not. 0 = none.")
                .defineInRange("sunWeakness", 1, 0, 5);
        NIGHTKIN_ALLY_RADIUS = b.comment("Nightkin 2.0: when you hit a player or a non-undead monster, or one of them hits you, idle undead within this radius attack it.")
                .defineInRange("allyRadius", 16, 0, 64);
        NIGHTKIN_ALLY_MAX = b.comment("Most undead that join in each time.")
                .defineInRange("allyMax", 8, 0, 64);
        NIGHTKIN_ALLY_COOLDOWN = b.comment("Ticks between calls for help of the same player (20 = 1 second).")
                .defineInRange("allyCooldownTicks", 40, 0, 1200);
        NIGHTKIN_NO_HEAL_VULNERABLE = b.comment("Nightkin 2.0: no healing at all (food, potions, spells, Consecration) while burning or vulnerable.",
                        "With Consecration installed, vulnerable = its fire/holy vulnerability (fireVulnerableDuration / holyVulnerableDuration).")
                .define("noHealWhileVulnerable", true);
        NIGHTKIN_FIRE_VULNERABLE = b.comment("Without Consecration: seconds without healing after taking fire damage.")
                .defineInRange("fireVulnerableSeconds", 10, 0, 120);
        b.pop();

        b.comment("MINER. miner_ore_amount (AMOUNT, max 5): extra rolls of the drops of the ores you break with the right tool. Not with Silk Touch.")
                .push("miner");
        MINER_BLOCKS = b.comment("Blocks or #tags that count as ores.")
                .defineListAllowEmpty("oreBlocks", List.of("#forge:ores"), o -> o instanceof String);
        b.pop();

        b.comment("LUMBERJACK. lumberjack_log_amount (AMOUNT, max 5): extra rolls of the drops of logs.",
                        "lumberjack_sapling (PERCENT): chance of one extra sapling when you break leaves.")
                .push("lumberjack");
        LUMBER_BLOCKS = b.comment("Blocks or #tags that count as logs.")
                .defineListAllowEmpty("logBlocks", List.of("#minecraft:logs"), o -> o instanceof String);
        b.pop();

        SPEC = b.build();
    }

    // ------------------------------------------------------------------ parsed cache
    private static volatile boolean dirty = true;
    private static List<ItemMatcher> potionCatalysts = List.of();
    private static List<CatalystDef> equipCatalysts = List.of();
    private static List<ItemMatcher> cropItems = List.of();
    private static List<ItemMatcher> craftedFoods = List.of();
    private static List<ItemMatcher> forceMelee = List.of();
    private static List<ItemMatcher> forceRanged = List.of();
    private static List<ItemMatcher> forceArmor = List.of();
    private static List<ItemMatcher> potionRebirth = List.of();
    private static List<ItemMatcher> equipRebirth = List.of();
    private static List<ItemMatcher> bookRebirth = List.of();
    private static Set<ResourceLocation> entityBlacklist = Set.of();
    private static Set<ResourceLocation> blockBlacklist = Set.of();
    private static Set<ResourceLocation> enchantBlacklist = Set.of();
    private static List<ItemMatcher> imbueTargets = List.of();
    private static List<ItemMatcher> imbueBlacklist = List.of();
    private static List<ItemMatcher> splitItems = List.of();
    private static List<ItemMatcher> scrollSplitItems = List.of();
    private static Set<ResourceLocation> noLevelEffects = Set.of();
    private static List<ResourceLocation> curses = List.of();
    private static List<ItemMatcher> fishItems = List.of();

    public static void markDirty() { dirty = true; }

    private static void ensure() {
        if (dirty) rebuild();
    }

    private static synchronized void rebuild() {
        potionCatalysts = parseMatchers(POTION_CATALYSTS.get());
        equipCatalysts = parseEquipCatalysts(EQUIP_CATALYSTS.get());
        cropItems = parseMatchers(CROPS_ITEMS.get());
        craftedFoods = parseMatchers(GASTRO_CRAFTED_FOODS.get());
        forceMelee = parseMatchers(EQUIP_MELEE_ITEMS.get());
        forceRanged = parseMatchers(EQUIP_RANGED_ITEMS.get());
        forceArmor = parseMatchers(EQUIP_ARMOR_ITEMS.get());
        potionRebirth = parseMatchers(POTION_REBIRTH_CATALYSTS.get());
        equipRebirth = parseMatchers(REBIRTH_CATALYSTS.get());
        bookRebirth = parseMatchers(BOOK_REBIRTH_CATALYSTS.get());
        entityBlacklist = parseIds(LOOT_ENTITY_BLACKLIST.get());
        blockBlacklist = parseIds(CROPS_BLOCK_BLACKLIST.get());
        enchantBlacklist = parseIds(BOOK_BLACKLIST.get());
        imbueTargets = parseMatchers(IMBUE_TARGETS.get());
        splitItems = parseMatchers(SPLIT_ITEMS.get());
        scrollSplitItems = parseMatchers(SCROLL_SPLIT_ITEMS.get());
        noLevelEffects = parseIds(POTION_NO_LEVEL_EFFECTS.get());
        Set<ResourceLocation> curseBlacklist = parseIds(BOOK_CURSE_BLACKLIST.get());
        List<ResourceLocation> allCurses = new ArrayList<>();
        for (Map.Entry<net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment>, net.minecraft.world.item.enchantment.Enchantment> e
                : net.minecraftforge.registries.ForgeRegistries.ENCHANTMENTS.getEntries()) {
            ResourceLocation id = e.getKey().location();
            if (e.getValue().isCurse() && !curseBlacklist.contains(id)) allCurses.add(id);
        }
        curses = allCurses;
        imbueBlacklist = parseMatchers(IMBUE_BLACKLIST.get());
        fishItems = parseMatchers(FISH_ITEMS.get());
        dirty = false;
    }

    private static List<ItemMatcher> parseMatchers(List<? extends String> raw) {
        List<ItemMatcher> out = new ArrayList<>();
        for (String s : raw) {
            ItemMatcher m = ItemMatcher.parse(s);
            if (m == null) WaifingAttributesMod.LOGGER.warn("[config] Invalid item entry: '{}'", s);
            else out.add(m);
        }
        return out;
    }

    public static boolean isFishItem(ItemStack stack) { ensure(); return matchesAny(fishItems, stack); }

    private static Set<ResourceLocation> parseIds(List<? extends String> raw) {
        Set<ResourceLocation> out = new HashSet<>();
        for (String s : raw) {
            ResourceLocation rl = ResourceLocation.tryParse(s.trim());
            if (rl != null) out.add(rl);
        }
        return out;
    }

    private record RepairDef(ItemMatcher matcher, double fraction) {}
    private static List<RepairDef> repairCatalysts = List.of();

    /** Fraction of the maximum durability this item repairs (blacksmith.catalysts REPAIR entries), 0 if none. */
    public static double repairFraction(ItemStack stack) {
        ensure();
        for (RepairDef r : repairCatalysts) if (r.matcher().matches(stack)) return r.fraction();
        return 0.0D;
    }

    private static List<CatalystDef> parseEquipCatalysts(List<? extends String> raw) {
        Map<String, CatalystDef> map = new LinkedHashMap<>();
        List<RepairDef> repair = new ArrayList<>();
        for (String line : raw) {
            try {
                String[] p = line.split(";");
                ItemMatcher m = ItemMatcher.parse(p[0]);
                if (m == null) throw new IllegalArgumentException("invalid item");
                if (p[1].trim().equalsIgnoreCase("REPAIR")) {
                    double f = Double.parseDouble(p[2].trim());
                    if (f <= 0.0D || f > 1.0D) throw new IllegalArgumentException("repair fraction must be > 0 and <= 1");
                    repair.add(new RepairDef(m, f));
                    continue;
                }
                UpgradeCategory cat = UpgradeCategory.valueOf(p[1].trim().toUpperCase(Locale.ROOT));
                String attr = p[2].trim();
                double amount = Double.parseDouble(p[3].trim());
                AttributeModifier.Operation op = p.length > 4
                        ? AttributeModifier.Operation.valueOf(p[4].trim().toUpperCase(Locale.ROOT))
                        : AttributeModifier.Operation.ADDITION;
                CatalystDef def = map.computeIfAbsent(m.key(), k -> new CatalystDef(m));
                if (attr.equalsIgnoreCase("durability")) {
                    def.add(new CatalystDef.Effect(cat, true, null, amount, op));
                } else {
                    ResourceLocation rl = ResourceLocation.tryParse(attr);
                    if (rl == null) throw new IllegalArgumentException("invalid attribute");
                    def.add(new CatalystDef.Effect(cat, false, rl, amount, op));
                }
            } catch (Exception e) {
                WaifingAttributesMod.LOGGER.warn("[config] Invalid blacksmith.catalysts entry '{}': {}", line, e.toString());
            }
        }
        repairCatalysts = repair;
        return new ArrayList<>(map.values());
    }

    // ------------------------------------------------------------------ queries
    public static boolean isPotionCatalyst(ItemStack stack) { ensure(); return matchesAny(potionCatalysts, stack); }
    public static boolean isHarvestItem(ItemStack stack) { ensure(); return matchesAny(cropItems, stack); }
    public static boolean isCraftedFood(ItemStack stack) { ensure(); return matchesAny(craftedFoods, stack); }
    public static List<ItemMatcher> forceMelee() { ensure(); return forceMelee; }
    public static List<ItemMatcher> forceRanged() { ensure(); return forceRanged; }
    public static List<ItemMatcher> forceArmor() { ensure(); return forceArmor; }
    public static boolean isEntityBlacklisted(ResourceLocation id) { ensure(); return entityBlacklist.contains(id); }
    public static boolean isBlockBlacklisted(ResourceLocation id) { ensure(); return blockBlacklist.contains(id); }
    public static boolean isEnchantBlacklisted(ResourceLocation id) { ensure(); return enchantBlacklist.contains(id); }
    /** Melee weapon that can be imbued: in 'targets' or dealing melee damage (anyMeleeWeapon), and not blacklisted. */
    public static boolean isImbueTarget(ItemStack stack) {
        ensure();
        if (matchesAny(imbueBlacklist, stack)) return false;
        if (matchesAny(imbueTargets, stack)) return true;
        return IMBUE_AUTO_MELEE.get() && stack.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND)
                .containsKey(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
    }

    /** Plain arrows that a potion turns into arrows with effects. */
    public static boolean isImbuableArrow(ItemStack stack) {
        ensure();
        return stack.is(net.minecraft.world.item.Items.ARROW) && !matchesAny(imbueBlacklist, stack);
    }
    public static boolean isSplitItem(ItemStack stack) { ensure(); return matchesAny(splitItems, stack); }
    public static boolean isScrollSplitItem(ItemStack stack) { ensure(); return matchesAny(scrollSplitItems, stack); }
    public static boolean isNoLevelEffect(ResourceLocation id) { ensure(); return noLevelEffects.contains(id); }
    public static List<ResourceLocation> curses() { ensure(); return curses; }

    /** True when the stack is explicitly listed as a Rebirth catalyst. */
    public static boolean isPotionRebirthListed(ItemStack s) { ensure(); return matchesAny(potionRebirth, s); }
    public static boolean isEquipRebirthListed(ItemStack s) { ensure(); return matchesAny(equipRebirth, s); }
    public static boolean isBookRebirthListed(ItemStack s) { ensure(); return matchesAny(bookRebirth, s); }

    public static boolean equipRebirthAllowed(ItemStack s, boolean validNormal) {
        ensure();
        return matchesAny(equipRebirth, s);
    }

    public static boolean bookRebirthAllowed(ItemStack s) {
        ensure();
        return matchesAny(bookRebirth, s);
    }

    @Nullable
    public static CatalystDef equipmentCatalyst(ItemStack stack, @Nullable UpgradeCategory cat) {
        if (cat == null) return null;
        ensure();
        for (CatalystDef d : equipCatalysts) if (d.matcher().matches(stack) && d.appliesTo(cat)) return d;
        return null;
    }

    public static boolean matchesAny(List<ItemMatcher> list, ItemStack stack) {
        for (ItemMatcher m : list) if (m.matches(stack)) return true;
        return false;
    }

    private WaifingConfig() {}
}
