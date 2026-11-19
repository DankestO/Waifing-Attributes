package com.waifing.waifingattributes.upgrade;

import com.waifing.waifingattributes.config.WaifingConfig;
import com.waifing.waifingattributes.util.Fx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;

/**
 * Alchemy by click: the ingredient on the cursor is "brewed" onto the potion in the inventory with the recipes of
 * Forge's brewing registry (vanilla and every mod that registers one). One ingredient converts the whole stack.
 * Event driven: one registry lookup per click, nothing else.
 */
public final class PotionBrewer {

    /** The potion the recipe would give, or EMPTY. Side-effect free. */
    private static ItemStack output(ItemStack target, ItemStack ingredient) {
        if (!(target.getItem() instanceof PotionItem) || ingredient.isEmpty()) return ItemStack.EMPTY;
        ItemStack one = target.copy();
        one.setCount(1); // the registry only accepts single potions
        return BrewingRecipeRegistry.getOutput(one, ingredient);
    }

    public static boolean canBrew(ItemStack target, ItemStack ingredient) {
        return !output(target, ingredient).isEmpty();
    }

    /** Brews the clicked stack. Returns false when there is no recipe (the caller then tries the upgrades). */
    public static boolean brew(UpgradeContext ctx) {
        ItemStack target = ctx.target();
        ItemStack out = output(target, ctx.catalyst());
        if (out.isEmpty()) return false;

        // Upgraded potions are custom-effect potions: vanilla container recipes (gunpowder, dragon's breath) would drop their effects.
        if (target.hasTag() && PotionUtils.getPotion(target) == Potions.EMPTY && target.getTag().contains("CustomPotionEffects")
                && out.getItem() != target.getItem()) {
            out.setTag(target.getTag().copy());
        }
        out.setCount(target.getCount());

        ctx.consumeCatalyst();
        ctx.replaceTarget(out);
        Fx.sound(ctx.player(), WaifingConfig.POTION_OK_SOUND.get());
        Fx.particles(ctx.player(), ParticleTypes.EFFECT, 10);
        return true;
    }

    private PotionBrewer() {}
}
