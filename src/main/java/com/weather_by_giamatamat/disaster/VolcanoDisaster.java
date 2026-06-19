package com.weather_by_giamatamat.disaster;

import com.weather_by_giamatamat.core.DisasterType;
import com.weather_by_giamatamat.core.ModConfig;
import com.weather_by_giamatamat.util.Fx;
import com.weather_by_giamatamat.util.WorldUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A volcanic eruption: converts a region into a geyser field by planting native MC 26.2 geysers
 * (a magma/lava heat-block topped by {@code potent_sulfur} and a contained water source). Roughly
 * one geyser per chunk; 10% of them are lava-rimmed continuous geysers. Triggered by earthquakes.
 */
public class VolcanoDisaster extends Disaster {
	private final int centerChunkX;
	private final int centerChunkZ;
	private final int sizeChunks;
	private int geysersPlaced;

	public VolcanoDisaster(final ServerLevel level, final int centerChunkX, final int centerChunkZ, final int sizeChunks) {
		super(level, DisasterType.VOLCANO, new Vec3(
			(centerChunkX << 4) + 8,
			WorldUtil.surfaceY(level, (centerChunkX << 4) + 8, (centerChunkZ << 4) + 8),
			(centerChunkZ << 4) + 8));
		this.centerChunkX = centerChunkX;
		this.centerChunkZ = centerChunkZ;
		this.sizeChunks = sizeChunks;
	}

	public int geysersPlaced() {
		return this.geysersPlaced;
	}

	@Override
	protected int warnTicks() {
		return 0; // erupts together with the earthquake
	}

	@Override
	protected int activeTicks() {
		return 30 * ModConfig.TICKS_PER_SECOND;
	}

	@Override
	protected void onStart() {
		plantGeysers();
		Fx.sound(level, center, SoundEvents.GEYSER_CONTINUOUS_START, SoundSource.BLOCKS, 5.0F, 0.6F);
	}

	@Override
	protected void onActiveTick() {
		if (age % 25 == 0) {
			Fx.sound(level, center, SoundEvents.GEYSER_ERUPTION_ACTIVE, SoundSource.BLOCKS, 3.0F, 0.8F);
		}
	}

	private void plantGeysers() {
		RandomSource random = level.getRandom();
		int half = sizeChunks / 2;
		for (int cx = centerChunkX - half; cx < centerChunkX - half + sizeChunks; cx++) {
			for (int cz = centerChunkZ - half; cz < centerChunkZ - half + sizeChunks; cz++) {
				// One geyser per chunk on average.
				if (!level.hasChunk(cx, cz)) {
					continue;
				}
				int x = (cx << 4) + random.nextInt(16);
				int z = (cz << 4) + random.nextInt(16);
				if (plantGeyser(x, z, random.nextDouble() < ModConfig.GEYSER_LAVA_CHANCE)) {
					geysersPlaced++;
				}
			}
		}
	}

	/**
	 * Plants a single geyser at the given column by carving a contained water source over a
	 * potent-sulfur block over a heat block (magma = periodic, lava = continuous).
	 */
	private boolean plantGeyser(final int x, final int z, final boolean lava) {
		int surface = WorldUtil.surfaceY(level, x, z);
		BlockPos top = new BlockPos(x, surface - 1, z);
		BlockState topState = level.getBlockState(top);
		if (topState.isAir() || !topState.getFluidState().isEmpty()) {
			return false; // need dry, solid ground (skip water/lava/air columns)
		}
		BlockPos sulfur = top.below();
		BlockPos heat = sulfur.below();
		level.setBlockAndUpdate(heat, lava ? Blocks.LAVA.defaultBlockState() : Blocks.MAGMA_BLOCK.defaultBlockState());
		level.setBlockAndUpdate(sulfur, Blocks.POTENT_SULFUR.defaultBlockState());
		level.setBlockAndUpdate(top, Blocks.WATER.defaultBlockState());
		if (lava) {
			ringWithLava(top);
		}
		return true;
	}

	private void ringWithLava(final BlockPos waterPos) {
		RandomSource random = level.getRandom();
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (dx == 0 && dz == 0) {
					continue;
				}
				if (random.nextDouble() > 0.5) {
					continue;
				}
				int x = waterPos.getX() + dx;
				int z = waterPos.getZ() + dz;
				BlockPos rim = new BlockPos(x, waterPos.getY(), z);
				BlockState state = level.getBlockState(rim);
				if (!state.isAir() && state.getFluidState().isEmpty() && state.getDestroySpeed(level, rim) >= 0) {
					level.setBlockAndUpdate(rim.above(), Blocks.LAVA.defaultBlockState());
				}
			}
		}
	}
}
