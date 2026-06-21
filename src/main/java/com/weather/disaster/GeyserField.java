package com.weather.disaster;

import com.weather.protection.ProtectedBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Turns a 10x10-chunk area into a geyser field for a volcanic eruption.
 *
 * <p>A real Minecraft 26.2 geyser is a {@code minecraft:potent_sulfur} block with water
 * above it and a heat source below: a magma block makes it erupt periodically, lava
 * makes it erupt continuously (per {@code causes_periodic/continuous_geyser_eruptions}).
 * So each geyser here is built as heat-source → potent sulfur → water column. To make it
 * a proper volcanic eruption, a good share sit on lava and lava pools well up around them.
 */
public final class GeyserField {
	private static final int FIELD_CHUNK_RADIUS = 5; // 10x10 chunks
	private static final int MAX_GEYSERS = 80;
	/** Fraction of geysers that sit on lava (continuous) rather than magma (periodic). */
	private static final double CONTINUOUS_SHARE = 0.4;
	/** Extra chance a (periodic) geyser still gets lava pooling around it. */
	private static final double LAVA_POOL_CHANCE = 0.25;

	private static final int[][] NEIGHBORS = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };

	private GeyserField() {
	}

	/** Place geysers across the loaded chunks around the centre. Returns how many were placed. */
	public static int create(ServerLevel level, double centerX, double centerZ) {
		RandomSource rng = level.getRandom();
		int centerCx = ((int) Math.floor(centerX)) >> 4;
		int centerCz = ((int) Math.floor(centerZ)) >> 4;
		int placed = 0;
		for (int cx = centerCx - FIELD_CHUNK_RADIUS; cx < centerCx + FIELD_CHUNK_RADIUS && placed < MAX_GEYSERS; cx++) {
			for (int cz = centerCz - FIELD_CHUNK_RADIUS; cz < centerCz + FIELD_CHUNK_RADIUS && placed < MAX_GEYSERS; cz++) {
				if (!level.hasChunk(cx, cz)) {
					continue; // only touch loaded terrain
				}
				int gx = (cx << 4) + rng.nextInt(16);
				int gz = (cz << 4) + rng.nextInt(16);
				if (placeGeyser(level, gx, gz, rng)) {
					placed++;
				}
			}
		}
		return placed;
	}

	private static boolean placeGeyser(ServerLevel level, int gx, int gz, RandomSource rng) {
		int airY = level.getHeight(Heightmap.Types.WORLD_SURFACE, gx, gz);
		BlockPos vent = new BlockPos(gx, airY, gz); // first air above the surface
		BlockPos heat = vent.below();               // the top solid block becomes the heat source

		ProtectedBlocks protectedBlocks = ProtectedBlocks.get(level);
		if (protectedBlocks.isProtected(heat) || protectedBlocks.isProtected(vent) || level.getBlockState(heat).isAir()) {
			return false;
		}

		boolean continuous = rng.nextDouble() < CONTINUOUS_SHARE;

		// heat source (lava = continuous, magma = periodic) -> potent sulfur -> water column
		level.setBlockAndUpdate(heat, (continuous ? Blocks.LAVA : Blocks.MAGMA_BLOCK).defaultBlockState());
		level.setBlockAndUpdate(vent, Blocks.POTENT_SULFUR.defaultBlockState());
		level.setBlockAndUpdate(vent.above(), Blocks.WATER.defaultBlockState());

		// Lava welling up around the eruption — always for lava geysers, sometimes otherwise.
		if (continuous || rng.nextDouble() < LAVA_POOL_CHANCE) {
			for (int[] dir : NEIGHBORS) {
				BlockPos around = new BlockPos(gx + dir[0], airY, gz + dir[1]);
				if (!protectedBlocks.isProtected(around) && level.getBlockState(around).isAir()
						&& !level.getBlockState(around.below()).isAir()) {
					level.setBlockAndUpdate(around, Blocks.LAVA.defaultBlockState());
				}
			}
		}
		return true;
	}
}
