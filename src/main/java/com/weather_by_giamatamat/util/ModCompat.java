package com.weather_by_giamatamat.util;

import com.weather_by_giamatamat.core.ModConfig;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import org.jspecify.annotations.Nullable;

/**
 * Soft compatibility with the optional <a href="https://github.com/giamat13/blocky13">blocky13</a> mod.
 * When present, sandstorms deposit {@code blocky13:sand_layer} blocks on the ground.
 */
public final class ModCompat {
	private static boolean blocky13Loaded;
	private static @Nullable Block sandLayerBlock;
	private static boolean resolved;

	private ModCompat() {
	}

	public static void init() {
		blocky13Loaded = FabricLoader.getInstance().isModLoaded(ModConfig.BLOCKY13_MOD_ID);
	}

	public static boolean isBlocky13Loaded() {
		return blocky13Loaded;
	}

	private static @Nullable Block sandLayer() {
		if (!resolved) {
			resolved = true;
			if (blocky13Loaded) {
				Identifier id = Identifier.parse(ModConfig.BLOCKY13_SAND_LAYER);
				Block block = BuiltInRegistries.BLOCK.getValue(id);
				if (block != Blocks.AIR) {
					sandLayerBlock = block;
				}
			}
		}
		return sandLayerBlock;
	}

	/**
	 * Places a blocky13 sand layer at {@code pos} if blocky13 is installed and the spot is suitable
	 * (currently air with a solid block beneath). Returns true if a layer was placed.
	 */
	public static boolean tryPlaceSandLayer(final ServerLevel level, final BlockPos pos) {
		Block block = sandLayer();
		if (block == null) {
			return false;
		}
		BlockState current = level.getBlockState(pos);
		if (!current.isAir()) {
			return false;
		}
		BlockState below = level.getBlockState(pos.below());
		if (below.isAir()) {
			return false;
		}
		level.setBlockAndUpdate(pos, block.defaultBlockState());
		return true;
	}
}
