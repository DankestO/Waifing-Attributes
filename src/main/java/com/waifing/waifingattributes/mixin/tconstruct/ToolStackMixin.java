package com.waifing.waifingattributes.mixin.tconstruct;

import com.waifing.waifingattributes.compat.TinkersStats;
import com.waifing.waifingattributes.upgrade.UpgradeData;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tinkers' Construct keeps its own durability system (the durability stat of ToolStack + the "Damage" NBT).
 * Instead of patching every place that reads the stat, the Waifing bonus (NBT WaifingUpgrades.Bonus) is added to the
 * durability stat that ToolStack#getStats returns. The bonus therefore raises the MAXIMUM durability everywhere:
 * the durability bar, the tooltip, the current/max values and the point where the tool breaks.
 *
 * Only applied when Tinkers' Construct is installed (see WaifingMixinPlugin); targets the class by name, so there is
 * no dependency on it. The injector is non-required: if a Tinkers update changes the method the mod keeps loading.
 */
@Pseudo
@Mixin(targets = "slimeknights.tconstruct.library.tools.nbt.ToolStack")
public abstract class ToolStackMixin {

    @Shadow(remap = false)
    private CompoundTag nbt;

    @Unique private Object waifingattributes$baseStats;
    @Unique private Object waifingattributes$boostedStats;
    @Unique private int waifingattributes$boostedBonus;

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(method = "getStats()Lslimeknights/tconstruct/library/tools/nbt/StatsNBT;",
            at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void waifingattributes$boostDurability(CallbackInfoReturnable cir) {
        Object base = cir.getReturnValue();
        if (base == null) return;
        int bonus = UpgradeData.bonusDurability(this.nbt);
        if (bonus <= 0) return;
        if (base == waifingattributes$baseStats && bonus == waifingattributes$boostedBonus) {
            cir.setReturnValue(waifingattributes$boostedStats);
            return;
        }
        Object boosted = TinkersStats.withDurabilityBonus(base, bonus);
        waifingattributes$baseStats = base;
        waifingattributes$boostedBonus = bonus;
        waifingattributes$boostedStats = boosted;
        cir.setReturnValue(boosted);
    }
}
