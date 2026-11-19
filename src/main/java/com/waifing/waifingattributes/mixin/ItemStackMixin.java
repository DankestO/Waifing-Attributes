package com.waifing.waifingattributes.mixin;

import com.waifing.waifingattributes.compat.TinkersStats;
import com.waifing.waifingattributes.upgrade.UpgradeData;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Extra maximum durability (Bronze): Forge has no event for this, hence the mixin.
 * Tinkers' Construct tools are skipped: their max durability already includes the bonus through ToolStackMixin.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Inject(method = "getMaxDamage", at = @At("RETURN"), cancellable = true)
    private void waifingattributes$bonusDurability(CallbackInfoReturnable<Integer> cir) {
        int base = cir.getReturnValue();
        if (base <= 0) return;
        if (TinkersStats.isTinkersItem(((ItemStack) (Object) this).getItem())) return;
        int bonus = UpgradeData.bonusDurability((ItemStack) (Object) this);
        if (bonus > 0) cir.setReturnValue(base + bonus);
    }
}
