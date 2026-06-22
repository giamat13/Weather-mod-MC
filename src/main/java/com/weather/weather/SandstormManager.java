package com.weather.weather;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.weather.protection.ProtectedBlocks;

import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Sandstorms. In a dry biome (one with no precipitation, e.g. a desert) the vanilla
 * "rain" weather just darkens the sky; this turns it into a sandstorm instead — a
 * shifting wind that speeds players up when they move with it and slows them down
 * when they fight it (stronger in a thunderstorm), plus blowing sand particles. If the
 * <a href="https://github.com/giamat13/blocky13">blocky13</a> mod is present, sand also
 * piles up as {@code blocky13:sand_layer} blocks.
 */
public class SandstormManager {
	private static final int EFFECT_INTERVAL = 5;
	private static final int EFFECT_DURATION = 30;
	private static final int PARTICLE_INTERVAL = 4;
	private static final int SAND_LAYER_INTERVAL = 40;
	private static final double MOVE_DOT_THRESHOLD = 0.3;
	private static final int SAND_LAYER_RADIUS = 8;

	private long tick;
	private final Map<UUID, double[]> lastPos = new HashMap<>();

	private boolean sandLayerResolved;
	private Block sandLayerBlock;

	public void onEndServerTick(MinecraftServer server) {
		tick++;
		for (ServerLevel level : server.getAllLevels()) {
			if (!level.isRaining()) {
				continue; // sandstorms ride on the vanilla rain weather
			}
			boolean storm = level.isThundering();
			for (ServerPlayer player : level.players()) {
				if (!isSandstormHere(level, player.blockPosition())) {
					lastPos.remove(player.getUUID());
					continue;
				}
				Vec3 wind = windDirection(level);
				if (tick % EFFECT_INTERVAL == 0) {
					applyWind(player, wind, storm);
				}
				if (tick % PARTICLE_INTERVAL == 0) {
					spawnSand(level, player, wind, storm);
				}
				Block sandLayer = sandLayer();
				if (sandLayer != null && tick % SAND_LAYER_INTERVAL == 0) {
					maybePileSand(level, player, sandLayer);
				}
			}
		}
	}

	private boolean isSandstormHere(ServerLevel level, BlockPos pos) {
		return level.precipitationAt(pos) == Biome.Precipitation.NONE;
	}

	/** A horizontal wind direction that slowly rotates over the day. */
	private Vec3 windDirection(ServerLevel level) {
		double angle = level.getOverworldClockTime() * 0.0003;
		return new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
	}

	private void applyWind(ServerPlayer player, Vec3 wind, boolean storm) {
		double curX = player.getX();
		double curZ = player.getZ();
		double[] last = lastPos.put(player.getUUID(), new double[] { curX, curZ });
		if (last == null) {
			return;
		}
		double mx = curX - last[0];
		double mz = curZ - last[1];
		double speed = Math.sqrt(mx * mx + mz * mz);
		if (speed < 0.03) {
			return; // standing still — the wind neither helps nor hinders
		}
		double dot = (mx * wind.x + mz * wind.z) / speed;
		int amplifier = storm ? 1 : 0;
		if (dot > MOVE_DOT_THRESHOLD) {
			player.addEffect(new MobEffectInstance(MobEffects.SPEED, EFFECT_DURATION, amplifier, false, false));
		} else if (dot < -MOVE_DOT_THRESHOLD) {
			player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, EFFECT_DURATION, amplifier, false, false));
		}
	}

	private void spawnSand(ServerLevel level, ServerPlayer player, Vec3 wind, boolean storm) {
		RandomSource rng = level.getRandom();
		int count = storm ? 8 : 4;
		for (int i = 0; i < count; i++) {
			double px = player.getX() + (rng.nextDouble() - 0.5) * 12 - wind.x * 4;
			double py = player.getY() + rng.nextDouble() * 4;
			double pz = player.getZ() + (rng.nextDouble() - 0.5) * 12 - wind.z * 4;
			level.sendParticles(ParticleTypes.DUST_PLUME, px, py, pz, 0, wind.x, 0.05, wind.z, 0.4);
		}
	}

	private void maybePileSand(ServerLevel level, ServerPlayer player, Block sandLayer) {
		RandomSource rng = level.getRandom();
		int x = player.getBlockX() + rng.nextInt(SAND_LAYER_RADIUS * 2 + 1) - SAND_LAYER_RADIUS;
		int z = player.getBlockZ() + rng.nextInt(SAND_LAYER_RADIUS * 2 + 1) - SAND_LAYER_RADIUS;
		int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
		BlockPos top = new BlockPos(x, surfaceY, z);
		BlockPos ground = top.below();
		if (level.getBlockState(top).isAir() && level.getBlockState(ground).isSolid()
				&& !ProtectedBlocks.get(level).isProtected(ground)) {
			level.setBlockAndUpdate(top, sandLayer.defaultBlockState());
		}
	}

	/** Resolve the optional blocky13 sand-layer block once (null if the mod isn't installed). */
	private Block sandLayer() {
		if (!sandLayerResolved) {
			sandLayerResolved = true;
			if (FabricLoader.getInstance().isModLoaded("blocky13")) {
				Identifier id = Identifier.fromNamespaceAndPath("blocky13", "sand_layer");
				if (BuiltInRegistries.BLOCK.containsKey(id)) {
					sandLayerBlock = BuiltInRegistries.BLOCK.getValue(id);
				}
			}
		}
		return sandLayerBlock;
	}
}
