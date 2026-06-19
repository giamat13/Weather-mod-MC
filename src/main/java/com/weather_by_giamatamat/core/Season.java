package com.weather_by_giamatamat.core;

import java.util.Locale;

/**
 * The four seasons. Advances every {@link ModConfig#SEASON_LENGTH_DAYS} Minecraft days.
 * Each season biases weather (rain chance / thunder) differently.
 */
public enum Season {
	SPRING("Spring", 0.30F),
	SUMMER("Summer", 0.10F),
	AUTUMN("Autumn", 0.45F),
	WINTER("Winter", 0.55F);

	private final String displayName;
	/** Base probability (per weather-roll) that it should be raining in this season. */
	private final float rainBias;

	Season(final String displayName, final float rainBias) {
		this.displayName = displayName;
		this.rainBias = rainBias;
	}

	public String displayName() {
		return this.displayName;
	}

	public float rainBias() {
		return this.rainBias;
	}

	public Season next() {
		return values()[(this.ordinal() + 1) % values().length];
	}

	/** Parses a season from a user-supplied string, or {@code null} if it does not match. */
	public static Season byName(final String name) {
		if (name == null) {
			return null;
		}
		String normalized = name.trim().toUpperCase(Locale.ROOT);
		for (Season season : values()) {
			if (season.name().equals(normalized)) {
				return season;
			}
		}
		return null;
	}
}
