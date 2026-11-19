package com.waifing.waifingattributes.mixin;

import com.waifing.waifingattributes.ModAttributes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

/**
 * rogue_nightkin 2.0: the player IS undead. Player does not override getMobType (LivingEntity returns UNDEFINED), so this adds the
 * override to Player only; other entities are untouched. Everything that asks "is it undead?" then sees an undead player: vanilla
 * (inverted healing/harming, immune to regeneration and poison, Smite, the Wither ignores it) and Consecration (undying: regeneration,
 * damage reduction, speed, weak to fire and holy damage). Cost: one attribute lookup per call.
 */
@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity {

    protected PlayerMixin(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public MobType getMobType() {
        AttributeInstance a = this.getAttribute(ModAttributes.ROGUE_NIGHTKIN.get());
        return a != null && a.getValue() >= 2.0D ? MobType.UNDEAD : super.getMobType();
    }
}
