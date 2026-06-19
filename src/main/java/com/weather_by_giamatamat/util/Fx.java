package com.weather_by_giamatamat.util;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Small helpers for broadcasting particles and sounds from the server to nearby clients.
 */
public final class Fx {
	public static final int COLOR_GREEN = 0x33DD33;
	public static final int COLOR_YELLOW = 0xFFE000;
	public static final int COLOR_RED = 0xFF2020;

	private Fx() {
	}

	public static void particles(final ServerLevel level, final ParticleOptions particle, final Vec3 pos,
			final int count, final double spread, final double speed) {
		level.sendParticles(particle, pos.x, pos.y, pos.z, count, spread, spread, spread, speed);
	}

	public static void particles(final ServerLevel level, final ParticleOptions particle,
			final double x, final double y, final double z,
			final int count, final double dx, final double dy, final double dz, final double speed) {
		level.sendParticles(particle, x, y, z, count, dx, dy, dz, speed);
	}

	public static void sound(final ServerLevel level, final Vec3 pos, final SoundEvent sound,
			final SoundSource source, final float volume, final float pitch) {
		level.playSound(null, pos.x, pos.y, pos.z, sound, source, volume, pitch);
	}

	public static void sound(final ServerLevel level, final double x, final double y, final double z,
			final SoundEvent sound, final SoundSource source, final float volume, final float pitch) {
		level.playSound(null, x, y, z, sound, source, volume, pitch);
	}
}
