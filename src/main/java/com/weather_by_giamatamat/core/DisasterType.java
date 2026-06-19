package com.weather_by_giamatamat.core;

import java.util.Locale;

/**
 * Every kind of natural disaster the mod can produce. Each one is warned about by an
 * {@link com.weather_by_giamatamat.alerter.Alerter} that has the matching module installed.
 */
public enum DisasterType {
	TORNADO("Tornado", "🌪"),
	HURRICANE("Hurricane", "🌀"),
	EARTHQUAKE("Earthquake", "🌋"),
	FIRE("Wildfire", "🔥"),
	METEOR("Meteor", "☄"),
	VOLCANO("Volcanic Eruption", "🌋"),
	SANDSTORM("Sandstorm", "🏜");

	private final String displayName;
	private final String emoji;

	DisasterType(final String displayName, final String emoji) {
		this.displayName = displayName;
		this.emoji = emoji;
	}

	public String displayName() {
		return this.displayName;
	}

	public String emoji() {
		return this.emoji;
	}

	public static DisasterType byName(final String name) {
		if (name == null) {
			return null;
		}
		String normalized = name.trim().toUpperCase(Locale.ROOT);
		for (DisasterType type : values()) {
			if (type.name().equals(normalized)) {
				return type;
			}
		}
		return null;
	}
}
