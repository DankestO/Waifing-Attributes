package com.waifing.waifingattributes.crop;

import com.waifing.waifingattributes.ModAttributes;
import com.waifing.waifingattributes.WaifingAttributesMod;
import com.waifing.waifingattributes.util.ChanceUtil;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.BonemealEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * farmer_bonemeal_keep (PERCENT): chance of NOT spending the bone meal.
 *
 * Forge fires {@link BonemealEvent} right BEFORE vanilla shrinks the stack, and it also fires for blocks that turn out not to accept
 * bone meal. So the chance is rolled here, the count is remembered, and on the server's next task the stack is checked: if it really
 * shrank by one, that one is given back; if nothing was spent (the plant did not accept it) nothing happens.
 * Event driven, no ticks. Dispensers and other fake players are ignored.
 */
@Mod.EventBusSubscriber(modid = WaifingAttributesMod.MODID)
public final class BonemealHandler {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBonemeal(BonemealEvent event) {
        if (event.isCanceled()) return;
        if (!(event.getEntity() instanceof ServerPlayer p) || p instanceof FakePlayer || p.getAbilities().instabuild) return;
        ItemStack stack = event.getStack();
        if (stack.isEmpty()) return;
        if (!ChanceUtil.roll(p.getAttributeValue(ModAttributes.FARMER_BONEMEAL_KEEP.get()), p.getRandom())) return;

        int before = stack.getCount();
        p.server.tell(new TickTask(p.server.getTickCount(), () -> {
            if (stack.getCount() == before - 1 && stack.getItem() != net.minecraft.world.item.Items.AIR) {
                stack.setCount(before);
                p.containerMenu.broadcastChanges();
            }
        }));
    }

    private BonemealHandler() {}
}
