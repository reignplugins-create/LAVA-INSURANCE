package com.lavainsurance;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Saves a player once from a lava / fire death if they are carrying a charm.
 *
 * Compatibility notes:
 *  - Runs at LOW priority, after other mods have had their turn. If another mod (a totem,
 *    a grave mod, anything) already cancelled this death, this handler never runs and the
 *    charm is NOT used up.
 *  - The charm is only consumed when the player really would have died from lava or fire.
 *  - Only the player's own vanilla inventory is read. No other mod's items or slots are touched.
 */
@Mod.EventBusSubscriber(modid = LavaInsuranceMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class InsuranceHandler {

    private static final int SEARCH_RADIUS = 10;

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.isCanceled()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!isLavaOrFire(event.getSource())) {
            return;
        }

        ItemStack charm = findCharm(player);
        if (charm.isEmpty()) {
            return;
        }

        charm.shrink(1);
        event.setCanceled(true);
        rescue(player);
    }

    private static boolean isLavaOrFire(DamageSource source) {
        return source == DamageSource.LAVA
                || source == DamageSource.IN_FIRE
                || source == DamageSource.ON_FIRE
                || source == DamageSource.HOT_FLOOR;
    }

    private static ItemStack findCharm(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        for (ItemStack stack : inventory.items) {
            if (stack.is(LavaInsuranceMod.CHARM.get())) {
                return stack;
            }
        }
        for (ItemStack stack : inventory.offhand) {
            if (stack.is(LavaInsuranceMod.CHARM.get())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static void rescue(ServerPlayer player) {
        ServerLevel level = player.getLevel();

        // Heal to half health and put the fire out.
        player.setHealth(Math.min(10.0F, player.getMaxHealth()));
        player.clearFire();
        player.fallDistance = 0.0F;

        // Fire resistance means that even if no safe spot is found, lava can't kill them right away.
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 30, 0));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 8, 1));

        // Move to the nearest safe standing spot, if there is one close by.
        BlockPos safe = findSafeSpot(level, player.blockPosition());
        if (safe != null) {
            player.connection.teleport(safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D,
                    player.getYRot(), player.getXRot());
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                player.getX(), player.getY() + 1.0D, player.getZ(), 40, 0.4D, 0.8D, 0.4D, 0.4D);
        player.displayClientMessage(Component.translatable("message.lavainsurance.saved"), true);
    }

    private static BlockPos findSafeSpot(ServerLevel level, BlockPos origin) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
            for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                for (int dy = -6; dy <= 10; dy++) {
                    pos.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (!isSafeToStand(level, pos)) {
                        continue;
                    }
                    double distance = pos.distSqr(origin);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = pos.immutable();
                    }
                }
            }
        }
        return best;
    }

    // Two blocks of free space, no fluid or fire, solid ground underneath.
    private static boolean isSafeToStand(ServerLevel level, BlockPos feetPos) {
        BlockPos headPos = feetPos.above();
        BlockPos groundPos = feetPos.below();
        if (level.isOutsideBuildHeight(groundPos.getY()) || level.isOutsideBuildHeight(headPos.getY())) {
            return false;
        }
        BlockState feet = level.getBlockState(feetPos);
        BlockState head = level.getBlockState(headPos);
        BlockState ground = level.getBlockState(groundPos);

        return feet.getCollisionShape(level, feetPos).isEmpty()
                && head.getCollisionShape(level, headPos).isEmpty()
                && feet.getFluidState().isEmpty()
                && head.getFluidState().isEmpty()
                && !feet.is(BlockTags.FIRE)
                && !head.is(BlockTags.FIRE)
                && ground.getFluidState().isEmpty()
                && ground.isFaceSturdy(level, groundPos, Direction.UP)
                && !ground.is(Blocks.MAGMA_BLOCK)
                && !ground.is(BlockTags.FIRE);
    }
}
