package com.weather.disaster;

import com.weather.block.TornadoHurricaneAlerterBlock;
import com.weather.registry.ModRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Drives a placed {@link TornadoHurricaneAlerterBlock}: picks a warning colour from the
 * distance to the nearest disaster and beeps loudly on a red alert.
 *
 * <ul>
 *   <li>GREEN  – no disaster nearby</li>
 *   <li>YELLOW – a disaster within {@value #YELLOW_RANGE} blocks</li>
 *   <li>RED    – a disaster within {@value #RED_RANGE} blocks (plus a loud bell)</li>
 * </ul>
 */
public final class Alerter {
	public static final int RED_RANGE = 100;
	public static final int YELLOW_RANGE = 500;

	private Alerter() {
	}

	public static int warningFor(double distance) {
		if (distance <= RED_RANGE) {
			return TornadoHurricaneAlerterBlock.WARNING_RED;
		}
		if (distance <= YELLOW_RANGE) {
			return TornadoHurricaneAlerterBlock.WARNING_YELLOW;
		}
		return TornadoHurricaneAlerterBlock.WARNING_NONE;
	}

	/** Update the alerter at {@code pos} to the given warning colour. */
	public static void apply(ServerLevel level, BlockPos pos, int warning) {
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() != ModRegistry.TORNADO_HURRICANE_ALERTER) {
			return;
		}
		if (state.getValue(TornadoHurricaneAlerterBlock.WARNING) != warning) {
			// Changing the block state lets an observer detect it; the explicit call
			// refreshes any comparator reading the alerter's warning level.
			level.setBlockAndUpdate(pos, state.setValue(TornadoHurricaneAlerterBlock.WARNING, warning));
			level.updateNeighbourForOutputSignal(pos, ModRegistry.TORNADO_HURRICANE_ALERTER);
		}
	}

	/** A loud, repeated alarm tone played from a red-alert alerter. */
	public static void beep(ServerLevel level, BlockPos pos) {
		level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
			SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 8.0f, 1.7f);
	}
}
