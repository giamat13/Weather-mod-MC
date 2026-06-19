package com.weather_by_giamatamat.disaster;

import com.weather_by_giamatamat.core.DisasterType;
import com.weather_by_giamatamat.core.ModConfig;
import com.weather_by_giamatamat.util.Fx;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * A marker for an ongoing wildfire so that fire-module Alerters react to it. The fire blocks
 * themselves are vanilla; this just tracks the event, throws up smoke, and times out.
 */
public class WildfireDisaster extends Disaster {
	public WildfireDisaster(final ServerLevel level, final Vec3 center) {
		super(level, DisasterType.FIRE, center);
	}

	@Override
	public double radius() {
		return 16.0;
	}

	@Override
	protected int warnTicks() {
		return 0;
	}

	@Override
	protected int activeTicks() {
		return 30 * ModConfig.TICKS_PER_SECOND;
	}

	@Override
	protected void onActiveTick() {
		if (age % 10 == 0) {
			Fx.particles(level, ParticleTypes.LARGE_SMOKE, center.x, center.y + 2.0, center.z, 10, 3.0, 2.0, 3.0, 0.02);
		}
		if (age % 40 == 0) {
			Fx.sound(level, center, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 2.0F, 1.0F);
		}
	}
}
