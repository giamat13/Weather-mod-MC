package com.weather.disaster;

import net.minecraft.server.level.ServerLevel;

/**
 * A disaster that has been decided on but hasn't started yet. The gap between
 * {@code startTick} and now is the window in which an Alerter can warn players
 * (up to two minutes ahead).
 */
public record ScheduledDisaster(DisasterType type, ServerLevel level, double x, double y, double z, long startTick) {
}
