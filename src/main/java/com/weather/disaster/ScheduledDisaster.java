package com.weather.disaster;

import net.minecraft.server.level.ServerLevel;

/**
 * A disaster that has been decided on but hasn't started yet. The gap between
 * {@code startTick} and now is the window in which an Alerter can warn players.
 *
 * <p>{@code meteorType} is only meaningful when {@code type == METEOR}; it is
 * {@code null} for every other disaster.
 */
public record ScheduledDisaster(DisasterType type, ServerLevel level, double x, double y, double z,
		long startTick, MeteorType meteorType) {

	public ScheduledDisaster(DisasterType type, ServerLevel level, double x, double y, double z, long startTick) {
		this(type, level, x, y, z, startTick, null);
	}
}
