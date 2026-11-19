package com.waifing.waifingattributes.upgrade;

import com.waifing.waifingattributes.ModAttributes;
import com.waifing.waifingattributes.config.WaifingConfig;
import com.waifing.waifingattributes.util.ChanceUtil;
import com.waifing.waifingattributes.util.Fx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Blacksmith repair: a repair catalyst (blacksmith.catalysts entry "item;REPAIR;fraction", none by default) RIGHT-clicked
 * onto damaged equipment restores that fraction of its maximum durability. No attribute of its own: it uses
 * blacksmith_upgrade, with the chance of the item's current upgrade tier (an item without upgrades or at level 1 uses the
 * level 1 chance; a level 7 item the level 7 chance). Failing only spends the catalyst: the item is never harmed and no
 * upgrade level is used. blacksmith_keep_catalyst applies as usual.
 */
public final class EquipmentRepair {

    public static void run(UpgradeContext ctx) {
        ServerPlayer p = ctx.player();
        ItemStack item = ctx.target();
        double fraction = WaifingConfig.repairFraction(ctx.catalyst());
        int damage = item.getDamageValue();
        if (fraction <= 0.0D || !item.isDamageableItem() || damage <= 0) {
            Fx.denied(p);
            return;
        }
        int tier = Math.max(1, UpgradeData.level(item));
        double chance = ChanceUtil.equipmentSuccess(p.getAttributeValue(ModAttributes.BLACKSMITH_UPGRADE.get()), tier);
        boolean ok = p.getRandom().nextDouble() < chance;
        ctx.consumeCatalyst(ModAttributes.BLACKSMITH_KEEP_CATALYST);
        if (ok) {
            int amount = Math.max(1, (int) Math.ceil(item.getMaxDamage() * fraction));
            item.setDamageValue(Math.max(0, damage - amount));
            Fx.sound(p, WaifingConfig.EQUIP_OK_SOUND.get());
            Fx.particles(p, ParticleTypes.HAPPY_VILLAGER, 8);
        } else {
            Fx.denied(p);
            Fx.particles(p, ParticleTypes.SMOKE, 8);
        }
    }

    private EquipmentRepair() {}
}
