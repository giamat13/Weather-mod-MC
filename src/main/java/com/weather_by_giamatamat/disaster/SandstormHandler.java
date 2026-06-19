package com.weather_by_giamatamat.disaster;

import com.weather_by_giamatamat.core.ModConfig;
import com.weather_by_giamatamat.util.Fx;
import com.weather_by_giamatamat.util.ModCompat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.phys.Vec3;

/**
 * Sandstorms. While it is "raining" in a desert (which vanilla just renders as a black sky), this
 * turns the weather into a blowing sandstorm: directional wind that speeds up players moving with it
 * and slows those moving against it, drifting sand particles, and — when blocky13 is installed —
 * deposited {@code blocky13:sand_layer} blocks.
 */
public final class SandstormHandler {
	private static final int SAND_COLOR = 0xD9C58B;
	private static final int EFFECT_DURATION = 30;

	private SandstormHandler() {
	}

	public static void tick(final ServerLevel level, final long gameTime) {
		if (!level.isRaining()) {
			return;
		}
		boolean storm = level.isThundering();
		Vec3 wind = windDirection(gameTime);
		for (ServerPlayer player : level.players()) {
			BlockPos pos = player.blockPosition();
			if (!isDesert(level, pos)) {
				continue;
			}
			applyWind(level, player, wind, storm);
			spawnSandParticles(level, player, wind);
			maybeDepositSand(level, player);
		}
	}

	private static boolean isDesert(final ServerLevel level, final BlockPos pos) {
		return level.getBiome(pos).is(Biomes.DESERT);
	}

	/** A slowly-rotating, server-wide wind direction (full rotation roughly every 20 minutes). */
	public static Vec3 windDirection(final long gameTime) {
		double angle = (gameTime % 24000L) / 24000.0 * Math.PI * 2.0;
		return new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
	}

	private static void applyWind(final ServerLevel level, final ServerPlayer player, final Vec3 wind, final boolean storm) {
		Vec3 move = player.getDeltaMovement();
		double horiz = Math.sqrt(move.x * move.x + move.z * move.z);
		if (horiz < 0.03) {
			return; // standing still: the wind neither helps nor hinders
		}
		double dot = (move.x * wind.x + move.z * wind.z) / horiz;
		if (dot > 0.25) {
			int amp = storm ? ModConfig.STORM_SPEED_AMPLIFIER : ModConfig.SANDSTORM_SPEED_AMPLIFIER;
			player.addEffect(new MobEffectInstance(MobEffects.SPEED, EFFECT_DURATION, amp, true, false), null);
		} else if (dot < -0.25) {
			int amp = storm ? ModConfig.STORM_SLOW_AMPLIFIER : ModConfig.SANDSTORM_SLOW_AMPLIFIER;
			player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, EFFECT_DURATION, amp, true, false), null);
		}
	}

	private static void spawnSandParticles(final ServerLevel level, final ServerPlayer player, final Vec3 wind) {
		RandomSource random = level.getRandom();
		DustParticleOptions sand = new DustParticleOptions(SAND_COLOR, 1.6F);
		Vec3 base = player.position();
		for (int i = 0; i < 14; i++) {
			double ox = (random.nextDouble() - 0.5) * 14.0;
			double oy = random.nextDouble() * 4.0;
			double oz = (random.nextDouble() - 0.5) * 14.0;
			Fx.particles(level, sand, base.x + ox, base.y + oy, base.z + oz, 1,
				wind.x * 0.6, 0.0, wind.z * 0.6, 0.5);
		}
	}

	private static void maybeDepositSand(final ServerLevel level, final ServerPlayer player) {
		if (!ModCompat.isBlocky13Loaded()) {
			return;
		}
		RandomSource random = level.getRandom();
		if (random.nextInt(40) != 0) {
			return;
		}
		int x = player.blockPosition().getX() + random.nextInt(17) - 8;
		int z = player.blockPosition().getZ() + random.nextInt(17) - 8;
		int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
		ModCompat.tryPlaceSandLayer(level, new BlockPos(x, y, z));
	}
}
