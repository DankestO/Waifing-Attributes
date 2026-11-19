package com.waifing.waifingattributes.animal;

import com.waifing.waifingattributes.ModAttributes;
import com.waifing.waifingattributes.WaifingAttributesMod;
import com.waifing.waifingattributes.util.ChanceUtil;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * farmer_breed_twins (PERCENT): chance of a second baby when the player breeds two animals.
 * farmer_feed_keep   (PERCENT): chance of NOT spending the food used to put an adult animal in love mode.
 * Same trick as the bone meal: the count is remembered and given back on the next server task only if it really went down by one.
 */
@Mod.EventBusSubscriber(modid = WaifingAttributesMod.MODID)
public final class BreedingHandler {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBaby(BabyEntitySpawnEvent event) {
        if (event.isCanceled() || event.getChild() == null) return;
        if (!(event.getCausedByPlayer() instanceof ServerPlayer p) || p instanceof FakePlayer) return;
        if (!(event.getParentA() instanceof Animal a) || !(event.getParentB() instanceof AgeableMob b)) return;
        if (!(a.level() instanceof ServerLevel level)) return;
        if (!ChanceUtil.roll(p.getAttributeValue(ModAttributes.FARMER_BREED_TWINS.get()), p.getRandom())) return;
        AgeableMob twin = a.getBreedOffspring(level, b);
        if (twin == null) return;
        twin.setBaby(true);
        twin.moveTo(a.getX(), a.getY(), a.getZ(), 0.0F, 0.0F);
        level.addFreshEntityWithPassengers(twin);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onFeed(PlayerInteractEvent.EntityInteract event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer p) || p instanceof FakePlayer || p.getAbilities().instabuild) return;
        if (!(event.getTarget() instanceof Animal animal) || animal.isBaby() || !animal.canFallInLove()) return;
        InteractionHand hand = event.getHand();
        ItemStack stack = p.getItemInHand(hand);
        if (stack.isEmpty() || !animal.isFood(stack)) return;
        if (!ChanceUtil.roll(p.getAttributeValue(ModAttributes.FARMER_FEED_KEEP.get()), p.getRandom())) return;
        int before = stack.getCount();
        ItemStack template = stack.copyWithCount(1);
        p.server.tell(new TickTask(p.server.getTickCount(), () -> {
            if (stack.getCount() != before - 1) return; // nothing was spent (the animal did not accept it)
            if (stack.isEmpty()) {
                if (p.getItemInHand(hand).isEmpty()) p.setItemInHand(hand, template);
            } else {
                stack.setCount(before);
            }
            p.containerMenu.broadcastChanges();
        }));
    }

    private BreedingHandler() {}
}
