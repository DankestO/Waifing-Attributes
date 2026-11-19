# Waifing Attributes

Waifing Attributes adds 47 player attributes, I created it for my skill tree, I separated it into two mods in case someone wants to use my attributes for some reason.
The mod is event-driven and lightweight. It does not run any per-tick logic, with one exception: a once-per-second daylight check for undead players (see `rogue_nightkin`). Every integration with other mods is optional.

---

## How it works

Many abilities use one simple interaction: pick up an item with the cursor and **right-click** another item in your inventory. Examples:

- An ingredient on a potion brews the potion.
- A catalyst on a sword upgrades the sword.
- An enchanted book on a pickaxe applies the enchantment.

If you lack the attribute for that action, the click behaves as it does in vanilla. This interaction is not available in the creative inventory.

## Attribute types

| Type | Meaning |
|---|---|
| Switch | Any value above 0 enables the ability. Capped at 1.0. |
| Percent | 0.01 to 1.0 means 1% to 100%. Capped at 1.0. |
| Amount | A direct quantity: 1.0 = +1 (or +100% for damage bonuses). Capped at the listed maximum. |
| Tier chance | Every level up to the attribute value succeeds at 100%. Each level above it halves the chance (configurable). Capped at 10.0. |

All attribute IDs use the `waifingattributes:` namespace.

---

## Hunter

| Attribute | Type | Max | Effect |
|---|---|---|---|
| `hunter_loot_amount` | Amount | 5 | Extra loot rolls when killing a mob. |
| `hunter_loot_quality` | Amount | 3 | Food dropped by mobs is raised by this many quality levels (Quality Food). |
| `hunter_stealth` | Percent | 1.0 | Mobs detect you from a shorter distance (0.5 = half the distance). |
| `hunter_ambush` | Amount | 4.0 | Bonus damage on the first hit against a mob that has not noticed anyone (4.0 = +400%). Applies once per encounter, so it does not stack in multiplayer. |

## Fisherman

| Attribute | Type | Max | Effect |
|---|---|---|---|
| `fisherman_catch_amount` | Amount | 5 | Extra copies of each catch. |
| `fisherman_catch_quality` | Amount | 3 | Catches gain this many quality levels on top of their own (Quality Food, works with or without Stardew Fishing). |

## Farmer

| Attribute | Type | Max | Effect |
|---|---|---|---|
| `farmer_harvest_amount` | Amount | 5 | Extra crops when harvesting. |
| `farmer_bonemeal_keep` | Percent | 1.0 | Chance not to consume bone meal. |
| `farmer_breed_twins` | Percent | 1.0 | Chance of a second baby when breeding animals. |
| `farmer_feed_keep` | Percent | 1.0 | Chance not to consume the food used for breeding. |

## Chef

| Attribute | Type | Max | Effect |
|---|---|---|---|
| `chef_cooking_quality` | Amount | 3 | Food cooked over fire gains this many quality levels (Quality Food). |
| `chef_imbue_food` | Percent | 1.0 | A potion on a food item stores its effects in one piece of food. The eater receives that fraction of the original duration. |

## Alchemist

| Attribute | Type | Max | Effect |
|---|---|---|---|
| `alchemist_brewing` | Switch | 1.0 | Brew potions by right-clicking an ingredient onto a potion. Uses every brewing recipe registered in the game. |
| `alchemist_potion_upgrade` | Tier chance | 10.0 | Upgrade potions with a catalyst (lapis lazuli by default). Potions with a duration have 10 levels of extra duration. Instant potions have 1 level of extra potency. |
| `alchemist_potion_rebirth` | Percent | 1.0 | A fully upgraded potion gains +1 potency and starts a new upgrade cycle (gold ingot by default). |
| `alchemist_keep_catalyst` | Percent | 1.0 | Chance not to consume the catalyst on upgrades and Rebirths. |

## Blacksmith

| Attribute | Type | Max | Effect |
|---|---|---|---|
| `blacksmith_upgrade` | Tier chance | 10.0 | Upgrade weapons, tools and armor up to level 10 with configurable catalysts. Defaults: iron ingot for attack damage and armor, copper ingot for extra durability. Also unlocks **repair** with repair catalysts (experience bottle by default, 5% per use), using the success chance of the item's upgrade level. |
| `blacksmith_rebirth` | Percent | 1.0 | A level 10 item resets its upgrade levels and keeps every bonus (diamond by default). |
| `blacksmith_keep_catalyst` | Percent | 1.0 | Chance not to consume the catalyst on upgrades and Rebirths. |
| `blacksmith_gem_socket` | Amount | 5 | Socket Apotheosis gems into equipment even if it has no sockets, up to this many sockets in total. Only you see the extra empty sockets. A socket becomes real once a gem is placed in it. |
| `blacksmith_gem_quality` | Percent | 1.0 | The attribute bonuses of the gems you socket are multiplied by 1 + value (up to +100%). The value is stored on the item, not on the gem. |
| `blacksmith_imbue_weapon_hits` | Percent | 1.0 | A potion on a melee weapon stores its effects for a number of hits (0.01 = 1 hit, 1.0 = 100 hits). Any item that deals melee damage qualifies. Also unlocks **imbued arrows**: a potion turns a whole stack of arrows into arrows that carry its full potency. |
| `blacksmith_imbue_weapon_duration` | Percent | 1.0 | Duration applied by each imbued hit or arrow, as a fraction of the potion's duration. |

## Enchanter

| Attribute | Type | Max | Effect |
|---|---|---|---|
| `enchanter_apply_book` | Switch | 1.0 | Apply an enchanted book directly to an item by right-clicking. |
| `enchanter_merge_books` | Switch | 1.0 | Merge two enchanted books. |
| `enchanter_split_books` | Switch | 1.0 | Split a book with several enchantments into separate books. |
| `enchanter_extract_book` | Switch | 1.0 | A normal book on an enchanted item extracts one enchantment into a book (curses cannot be extracted). A blank Book and Quill on a written book makes a copy of it, signed or unsigned. |
| `enchanter_book_rebirth` | Percent | 1.0 | Raise a book's enchantment above its natural maximum (diamond by default). A failed attempt may turn the book into a random curse. |
| `enchanter_keep_book` | Percent | 1.0 | Chance not to consume the book when applying or merging. |
| `enchanter_table_xp` | Percent | 1.0 | Experience level discount at the enchanting table. |
| `enchanter_table_lapis` | Percent | 1.0 | Lapis lazuli refund at the enchanting table. |

The anvil keeps enchantment levels that are above their maximum, so books raised with Rebirth also work there.

## Mage (Iron's Spells 'n Spellbooks)

| Attribute | Type | Max | Effect |
|---|---|---|---|
| `mage_scroll_merge` | Switch | 1.0 | Merge two scrolls of the same spell into a higher level scroll. |
| `mage_scroll_split` | Switch | 1.0 | Split a scroll into lower level scrolls using paper. |
| `mage_arcane_anvil` | Switch | 1.0 | Put a scroll's spell into an item without using the Arcane Anvil. |
| `mage_keep_scroll` | Percent | 1.0 | Chance not to consume the scroll when merging or imbuing. |

## Rogue

| Attribute | Type | Max | Effect |
|---|---|---|---|
| `rogue_backstab` | Amount | 1.0 | Bonus damage when hitting from behind (1.0 = +100%). Works with melee, projectiles and spells, and stacks with `hunter_ambush`. |
| `rogue_nightkin` | Amount | 2.0 | **1.0:** undead mobs ignore you unless you attack them (bosses and summons excluded). **2.0:** you count as undead in vanilla and in Consecration. Nearby undead fight for you, but sunlight burns you without a helmet and weakens you. |
| `rogue_pickpocket` | Percent | 1.0 | Chance to get your payment back when trading with a villager. |

## Miner

| Attribute | Type | Max | Effect |
|---|---|---|---|
| `miner_ore_amount` | Amount | 5 | Extra drop rolls when mining ores (not with Silk Touch). |

## Lumberjack

| Attribute | Type | Max | Effect |
|---|---|---|---|
| `lumberjack_log_amount` | Amount | 5 | Extra drop rolls when cutting logs. |
| `lumberjack_sapling` | Percent | 1.0 | Chance of an extra sapling when breaking leaves. |

## Tamer

| Attribute | Type | Max | Effect |
|---|---|---|---|
| `tamer_pet_damage` | Amount | 3.0 | Bonus damage dealt by your tamed pets (1.0 = +100%). Spell summons are not affected. |

## Merchant

| Attribute | Type | Max | Effect |
|---|---|---|---|
| `merchant_haggle` | Percent | 1.0 | Discount when trading with villagers (1.0 = the largest vanilla reputation discount). |
| `merchant_xp` | Percent | 1.0 | Extra experience for you on each trade (1.0 = +100%). |
| `merchant_restock` | Percent | 1.0 | Chance that a trade does not use up a use of the offer. |
| `merchant_villager_xp` | Percent | 1.0 | The villager gains extra trading experience and levels up faster (1.0 = +100%). |

## Explorer

| Attribute | Type | Max | Effect |
|---|---|---|---|
| `explorer_fall` | Percent | 1.0 | Reduces fall damage (1.0 = no fall damage). |

---

## Tooltips

Upgraded items show their level next to their name (for example `Diamond Sword +4`), followed by one star per Rebirth. Catalysts list what they do for each profession. With Obscure Tooltips installed, the frame of an upgraded item matches its level: copper, iron, gold, diamond, and netherite after a Rebirth.

## Optional integrations

All of the following are optional. None is needed to run the mod.

- **Apotheosis:** gem sockets and gem quality.
- **Iron's Spells 'n Spellbooks:** Mage attributes.
- **Tinkers' Construct:** upgrades and extra durability on Tinkers tools.
- **Quality Food:** quality bonuses for food.
- **Kaleidoscope Cookery:** quality bonuses for its food.
- **Stardew Fishing:** quality bonuses for its catches.
- **Consecration:** undead behaviour for `rogue_nightkin` 2.0.
- **Obscure Tooltips:** frames for upgraded items.

## Configuration

Server configuration in `serverconfig/waifingattributes-server.toml`, synced to clients:

- Catalysts and their bonuses.
- Upgrade chances.
- Rebirth costs.
- Item lists for each feature.

Client configuration in `config/waifingattributes-client.toml`: tooltip options.
