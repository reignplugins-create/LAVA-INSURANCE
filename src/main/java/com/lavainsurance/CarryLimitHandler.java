package com.lavainsurance;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * A player can carry at most one Lava Insurance Charm.
 *
 * Three layers, so it holds no matter how a second charm tries to get in:
 *  1. The item's stack size is 1 (see LavaInsuranceMod).
 *  2. Picking up a charm off the ground is blocked while you already carry one.
 *  3. If an extra still shows up in the inventory (crafting, a chest, shift-click, another mod),
 *     it is dropped back on the ground a moment later. Creative mode is exempt.
 */
@Mod.EventBusSubscriber(modid = LavaInsuranceMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CarryLimitHandler {
    public static final int MAX_CARRIED = 1;

    @SubscribeEvent
    public static void onPickup(EntityItemPickupEvent event) {
        Player player = event.getEntity();
        if (player.isCreative()) {
            return;
        }
        if (!event.getItem().getItem().is(LavaInsuranceMod.CHARM.get())) {
            return;
        }
        if (countCharms(player) >= MAX_CARRIED) {
            event.setCanceled(true);
            // Pickup is attempted every tick while standing on it, so only show the message now and then.
            if (!player.level.isClientSide && player.tickCount % 20 == 0) {
                player.displayClientMessage(Component.translatable("message.lavainsurance.limit"), true);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()) {
            return;
        }
        Player player = event.player;
        if (player.tickCount % 10 != 0 || player.isCreative()) {
            return;
        }
        if (countCharms(player) <= MAX_CARRIED) {
            return;
        }

        int allowed = MAX_CARRIED;
        allowed = trim(player, player.getInventory().items, allowed);
        trim(player, player.getInventory().offhand, allowed);
        player.displayClientMessage(Component.translatable("message.lavainsurance.limit"), true);
    }

    private static int countCharms(Player player) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(LavaInsuranceMod.CHARM.get())) {
                total += stack.getCount();
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.is(LavaInsuranceMod.CHARM.get())) {
                total += stack.getCount();
            }
        }
        return total;
    }

    // Keeps up to "allowed" charms (earliest slots first), drops the rest. Returns how many are still allowed.
    private static int trim(Player player, List<ItemStack> stacks, int allowed) {
        for (ItemStack stack : stacks) {
            if (!stack.is(LavaInsuranceMod.CHARM.get())) {
                continue;
            }
            int keep = Math.min(stack.getCount(), allowed);
            allowed -= keep;
            int extra = stack.getCount() - keep;
            if (extra > 0) {
                player.drop(stack.split(extra), false);
            }
        }
        return allowed;
    }
}
