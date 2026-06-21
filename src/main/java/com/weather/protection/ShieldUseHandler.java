package com.weather.protection;

import com.weather.registry.ModRegistry;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Right-clicking a block with the Disaster Shield marks that block as protected
 * (consuming one shield), so natural disasters leave it alone.
 */
public final class ShieldUseHandler {
	private ShieldUseHandler() {
	}

	public static void register() {
		UseBlockCallback.EVENT.register(ShieldUseHandler::onUseBlock);
	}

	private static InteractionResult onUseBlock(Player player, Level world, InteractionHand hand, BlockHitResult hit) {
		ItemStack stack = player.getItemInHand(hand);
		if (stack.getItem() != ModRegistry.DISASTER_SHIELD) {
			return InteractionResult.PASS;
		}
		if (!(world instanceof ServerLevel level)) {
			return InteractionResult.SUCCESS; // act server-side; swing the arm on the client
		}
		BlockPos pos = hit.getBlockPos();
		if (level.getBlockState(pos).isAir()) {
			return InteractionResult.PASS;
		}
		ProtectedBlocks protectedBlocks = ProtectedBlocks.get(level);
		if (!protectedBlocks.protect(pos)) {
			return InteractionResult.SUCCESS; // already shielded — do nothing, keep the item
		}
		if (!player.getAbilities().instabuild) {
			stack.shrink(1);
		}
		level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
			SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 0.7f, 1.6f);
		level.sendParticles(ParticleTypes.POOF, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
			12, 0.4, 0.4, 0.4, 0.0);
		return InteractionResult.SUCCESS;
	}
}
