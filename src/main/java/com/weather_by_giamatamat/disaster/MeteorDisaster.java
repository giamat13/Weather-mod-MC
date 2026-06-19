package com.weather_by_giamatamat.disaster;

import com.weather_by_giamatamat.core.DisasterType;
import com.weather_by_giamatamat.core.ModConfig;
import com.weather_by_giamatamat.util.Fx;
import com.weather_by_giamatamat.util.WorldUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * A meteor: it streaks down from the sky over a couple of seconds, then explodes on impact,
 * leaving a burning, magma-lined crater.
 */
public class MeteorDisaster extends Disaster {
	private static final int DESCENT_TICKS = 40;

	private Vec3 skyStart;

	public MeteorDisaster(final ServerLevel level, final Vec3 impact) {
		super(level, DisasterType.METEOR, impact);
	}

	@Override
	public double radius() {
		return ModConfig.METEOR_EXPLOSION_POWER;
	}

	@Override
	protected int warnTicks() {
		return ModConfig.METEOR_WARN_TICKS;
	}

	@Override
	protected int activeTicks() {
		return DESCENT_TICKS;
	}

	@Override
	protected void onImpact() {
		RandomSource random = level.getRandom();
		double angleOffset = 50.0;
		this.skyStart = new Vec3(
			center.x + (random.nextDouble() - 0.5) * angleOffset,
			center.y + 130.0,
			center.z + (random.nextDouble() - 0.5) * angleOffset);
		Fx.sound(level, center, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.WEATHER, 6.0F, 0.6F);
	}

	@Override
	protected void onActiveTick() {
		int descentAge = age - warnTicks();
		double t = Math.min(1.0, descentAge / (double) DESCENT_TICKS);
		Vec3 pos = skyStart.lerp(center, t);
		// Fiery trail.
		Fx.particles(level, ParticleTypes.FLAME, pos.x, pos.y, pos.z, 12, 0.4, 0.4, 0.4, 0.02);
		Fx.particles(level, ParticleTypes.LARGE_SMOKE, pos.x, pos.y, pos.z, 8, 0.5, 0.5, 0.5, 0.01);
		Fx.particles(level, ParticleTypes.LAVA, pos.x, pos.y, pos.z, 2, 0.3, 0.3, 0.3, 0.0);
		if (descentAge >= DESCENT_TICKS - 1) {
			impactExplosion();
		}
	}

	private void impactExplosion() {
		level.explode(null, center.x, center.y, center.z, ModConfig.METEOR_EXPLOSION_POWER, Level.ExplosionInteraction.MOB);
		RandomSource random = level.getRandom();
		int craterRadius = (int) ModConfig.METEOR_EXPLOSION_POWER;
		BlockPos impact = centerPos();
		for (int dx = -craterRadius; dx <= craterRadius; dx++) {
			for (int dz = -craterRadius; dz <= craterRadius; dz++) {
				if (dx * dx + dz * dz > craterRadius * craterRadius) {
					continue;
				}
				int x = impact.getX() + dx;
				int z = impact.getZ() + dz;
				int surface = WorldUtil.surfaceY(level, x, z);
				BlockPos floor = new BlockPos(x, surface - 1, z);
				if (level.getBlockState(floor).isAir()) {
					continue;
				}
				double roll = random.nextDouble();
				if (roll < 0.15) {
					level.setBlockAndUpdate(floor, Blocks.MAGMA_BLOCK.defaultBlockState());
				}
				BlockPos above = floor.above();
				if (roll > 0.7 && level.getBlockState(above).isAir()) {
					level.setBlockAndUpdate(above, Blocks.FIRE.defaultBlockState());
				}
			}
		}
	}
}
