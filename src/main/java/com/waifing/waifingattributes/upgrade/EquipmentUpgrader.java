package com.waifing.waifingattributes.upgrade;

import com.waifing.waifingattributes.ModAttributes;
import com.waifing.waifingattributes.config.CatalystDef;
import com.waifing.waifingattributes.config.WaifingConfig;
import com.waifing.waifingattributes.util.ChanceUtil;
import com.waifing.waifingattributes.util.Fx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Weapon/tool/armor/shield + catalyst ingot (on the cursor, right-clicked onto the item).
 * Up to N levels per cycle. Failure: the item is destroyed (configurable). With the attribute high enough and the
 * item complete there is a "rebirth" chance that resets the slots while keeping every bonus; failing it destroys the
 * item by default (configurable) and the catalyst can be restricted in the config.
 */
public final class EquipmentUpgrader {

    public static void run(UpgradeContext ctx) {
        ServerPlayer p = ctx.player();
        ItemStack main = ctx.target();
        ItemStack off = ctx.catalyst();
        UpgradeCategory cat = UpgradeCategory.detect(main);
        CatalystDef def = WaifingConfig.equipmentCatalyst(off, cat);
        if (cat == null || (def == null && !WaifingConfig.isEquipRebirthListed(off))) return;

        double attr = p.getAttributeValue(ModAttributes.BLACKSMITH_UPGRADE.get());
        int max = ModAttributes.MAX_UPGRADE_LEVELS;
        int level = UpgradeData.level(main);

        // ---------- Rebirth (complete equipment)
        if (level >= max) {
            rebirth(ctx, def != null);
            return;
        }
        if (def == null) {
            Fx.denied(p);
            return;
        }

        int target = level + 1;
        double chance = ChanceUtil.equipmentSuccess(attr, target);
        boolean ok = p.getRandom().nextDouble() < chance;

        if (ok || WaifingConfig.CONSUME_CATALYST_ON_FAIL.get()) ctx.consumeCatalyst(ModAttributes.BLACKSMITH_KEEP_CATALYST);

        if (ok) {
            UpgradeData.apply(main, def, cat, UpgradeCategory.slotsOf(main, cat));
            Fx.sound(p, WaifingConfig.EQUIP_OK_SOUND.get());
            Fx.particles(p, ParticleTypes.HAPPY_VILLAGER, 12);
        } else if (WaifingConfig.EQUIP_DESTROY_ON_FAIL.get()) {
            ctx.destroyTarget(); // the item explodes
            Fx.sound(p, WaifingConfig.EQUIP_FAIL_SOUND.get());
            Fx.particles(p, ParticleTypes.EXPLOSION, 3);
        } else {
            Fx.sound(p, WaifingConfig.EQUIP_FAIL_SOUND.get());
        }
    }

    private static void rebirth(UpgradeContext ctx, boolean validNormalCatalyst) {
        ServerPlayer p = ctx.player();
        ItemStack main = ctx.target();
        ItemStack off = ctx.catalyst();
        double chance = p.getAttributeValue(ModAttributes.BLACKSMITH_REBIRTH.get()); // PERCENT attribute
        if (chance <= 0.0D) {
            Fx.denied(p);
            return;
        }
        int maxRebirths = WaifingConfig.REBIRTH_MAX.get();
        if (maxRebirths > 0 && UpgradeData.rebirths(main) >= maxRebirths) {
            Fx.denied(p);
            return;
        }
        if (!WaifingConfig.equipRebirthAllowed(off, validNormalCatalyst)) {
            Fx.denied(p);
            return;
        }
        ctx.consumeCatalyst(ModAttributes.BLACKSMITH_KEEP_CATALYST);
        if (ChanceUtil.roll(chance, p.getRandom())) {
            UpgradeData.rebirth(main);
            Fx.rebirthSuccess(p);
            Fx.particles(p, ParticleTypes.TOTEM_OF_UNDYING, 40);
        } else if (WaifingConfig.REBIRTH_DESTROY_ON_FAIL.get()) {
            ctx.destroyTarget(); // the item is lost
            Fx.sound(p, WaifingConfig.EQUIP_FAIL_SOUND.get());
            Fx.particles(p, ParticleTypes.EXPLOSION, 3);
        } else {
            Fx.sound(p, WaifingConfig.EQUIP_FAIL_SOUND.get());
        }
    }

    private EquipmentUpgrader() {}
}
