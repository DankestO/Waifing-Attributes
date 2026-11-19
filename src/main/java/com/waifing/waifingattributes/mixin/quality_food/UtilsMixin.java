package com.waifing.waifingattributes.mixin.quality_food;

import com.waifing.waifingattributes.util.ChanceUtil;
import com.waifing.waifingattributes.ModAttributes;
import com.waifing.waifingattributes.compat.QualityBoost;
import com.waifing.waifingattributes.config.WaifingConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Chef: Quality Food stores the quality of what a furnace cooks inside the block and applies it when the PLAYER
 * takes the result (clicking the slot or shift-clicking). This hook runs right after Quality Food did that and raises
 * the quality of the taken food by the Chef tiers. Only furnaces (furnace, smoker, blast furnace and anything that
 * extends them) and only a server player taking the item: crafting and cutting boards are never touched.
 *
 * Only applied when Quality Food is installed (see WaifingMixinPlugin); it targets the class by name, so there is no
 * dependency on it. The injector is non-required: if a Quality Food update changes the method the mod keeps loading.
 */
@Pseudo
@Mixin(targets = "de.cadentem.quality_food.util.Utils")
public abstract class UtilsMixin {

    /** Shift-clicking calls Quality Food twice for the same stack object: only the first one may add levels. */
    @Unique private static ItemStack waifingattributes$last;
    @Unique private static long waifingattributes$lastTick = Long.MIN_VALUE;

    @Inject(method = "useQuality(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;)V",
            at = @At("TAIL"), remap = false, require = 0)
    private static void waifingattributes$cookingBonus(BlockEntity blockEntity, ItemStack stack, Player player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayer sp) || !(blockEntity instanceof AbstractFurnaceBlockEntity) || stack.isEmpty()) return;
        long now = sp.level().getGameTime();
        if (stack == waifingattributes$last && now == waifingattributes$lastTick) return;
        int levels = ChanceUtil.levels(sp.getAttributeValue(ModAttributes.CHEF_COOKING_QUALITY.get()));
        if (levels <= 0) return;
        waifingattributes$last = stack;
        waifingattributes$lastTick = now;
        QualityBoost.raiseBy(stack, levels);
    }
}
