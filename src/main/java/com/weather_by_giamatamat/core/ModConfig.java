package com.weather_by_giamatamat.core;

/**
 * Central tunables for the whole mod. Kept as plain constants so the behaviour is easy to read
 * and adjust in one place.
 */
public final class ModConfig {
	private ModConfig() {
	}

	// --- Time helpers -------------------------------------------------------
	public static final int TICKS_PER_SECOND = 20;
	public static final int TICKS_PER_MINUTE = 60 * TICKS_PER_SECOND;
	public static final long TICKS_PER_DAY = 24000L;

	// --- Master state -------------------------------------------------------
	public static final boolean ENABLED_BY_DEFAULT = true;

	// --- Alerter ------------------------------------------------------------
	/** How long before impact the alerter starts warning (2 minutes). */
	public static final int WARN_TICKS = 2 * TICKS_PER_MINUTE;
	/** Yellow light radius: a disaster within this many blocks. */
	public static final double ALERT_YELLOW_RANGE = 500.0;
	/** Red light radius: a disaster within this many blocks (also loud beeping). */
	public static final double ALERT_RED_RANGE = 100.0;
	/** Re-validate alerter structures and refresh their lights this often. */
	public static final int ALERTER_TICK_INTERVAL = 5;

	// --- Tornado / Hurricane ------------------------------------------------
	/** Chance that a newly-started storm spawns a tornado or hurricane. */
	public static final double STORM_DISASTER_CHANCE = 0.15;
	/** Of those, the share that becomes a (stronger, bigger) hurricane. */
	public static final double HURRICANE_SHARE = 0.4;
	public static final double TORNADO_RADIUS = 8.0;
	public static final double HURRICANE_RADIUS = 22.0;
	public static final int TORNADO_ACTIVE_TICKS = 40 * TICKS_PER_SECOND;
	public static final int HURRICANE_ACTIVE_TICKS = 70 * TICKS_PER_SECOND;
	/** Distance the funnel travels per tick. */
	public static final double TORNADO_MOVE_SPEED = 0.20;

	// --- Earthquake ---------------------------------------------------------
	public static final int EARTHQUAKE_WARN_TICKS = 30 * TICKS_PER_SECOND;
	public static final int EARTHQUAKE_ACTIVE_TICKS = 12 * TICKS_PER_SECOND;
	/** Radius (in blocks) of the shaking / damage area at magnitude scaling. */
	public static final double EARTHQUAKE_BASE_RADIUS = 24.0;
	/** Fraction of breakable blocks destroyed per chunk, per point of magnitude. */
	public static final double EARTHQUAKE_BREAK_PER_MAGNITUDE = 0.004;
	public static final int EARTHQUAKE_MIN_MAGNITUDE = 2;
	public static final int EARTHQUAKE_MAX_MAGNITUDE = 9;
	/** Per-server-tick chance of a natural earthquake (≈ one every two Minecraft days). */
	public static final double EARTHQUAKE_TICK_CHANCE = 1.0 / 40000.0;
	/** Hard cap on block-cracks attempted per tick, to keep large quakes performant. */
	public static final int EARTHQUAKE_MAX_CRACKS_PER_TICK = 24;

	// --- Wildfire -----------------------------------------------------------
	/** Per qualifying random-tick attempt in summer on a flammable block: chance to ignite. */
	public static final double FIRE_SUMMER_IGNITE_CHANCE = 0.000009 / 100.0; // 0.000009%
	/** How many random flammable-block samples to attempt per loaded chunk per scan. */
	public static final int FIRE_SAMPLES_PER_CHUNK = 3;

	// --- Meteor -------------------------------------------------------------
	public static final int METEOR_INTERVAL_MIN_TICKS = 45 * TICKS_PER_MINUTE;
	public static final int METEOR_INTERVAL_MAX_TICKS = 90 * TICKS_PER_MINUTE;
	public static final double METEOR_CHANCE = 0.10;
	public static final int METEOR_WARN_TICKS = WARN_TICKS;
	public static final float METEOR_EXPLOSION_POWER = 6.0F;

	// --- Volcano / Geysers --------------------------------------------------
	/** Side length (in chunks) of the region checked for geyser-field conversion. */
	public static final int GEYSER_REGION_CHUNKS = 10;
	/** Base chance a region becomes a geyser field during an earthquake (scaled by biome). */
	public static final double GEYSER_FIELD_BASE_CHANCE = 0.15;
	/** Chance a spawned geyser is surrounded by lava. */
	public static final double GEYSER_LAVA_CHANCE = 0.10;

	// --- Seasons ------------------------------------------------------------
	public static final int SEASON_LENGTH_DAYS = 30;

	// --- Sandstorm ----------------------------------------------------------
	public static final int SANDSTORM_SLOW_AMPLIFIER = 1;
	public static final int SANDSTORM_SPEED_AMPLIFIER = 1;
	public static final int STORM_SLOW_AMPLIFIER = 2;
	public static final int STORM_SPEED_AMPLIFIER = 2;
	/** blocky13 sand-layer block id, placed if that mod is present. */
	public static final String BLOCKY13_MOD_ID = "blocky13";
	public static final String BLOCKY13_SAND_LAYER = "blocky13:sand_layer";
}
