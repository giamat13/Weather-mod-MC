package com.weather.season;

/**
 * The four seasons. Each one biases the overworld weather: how likely rain is to
 * start on a given day, how often that rain becomes a thunderstorm, and how likely
 * ongoing rain is to clear early.
 */
public enum Season {
	SPRING("Spring", "אביב", "🌱", 0.45, 0.20, 0.20),
	SUMMER("Summer", "קיץ", "☀", 0.12, 0.10, 0.60),
	AUTUMN("Autumn", "סתיו", "🍂", 0.40, 0.45, 0.20),
	WINTER("Winter", "חורף", "❄", 0.35, 0.05, 0.25);

	/** Chance per day that rain starts while the sky is clear. */
	public final double rainChance;
	/** Chance that a rain that starts is a thunderstorm. */
	public final double thunderChance;
	/** Chance per day that ongoing rain clears early. */
	public final double clearChance;

	public final String englishName;
	public final String hebrewName;
	public final String icon;

	Season(String englishName, String hebrewName, String icon, double rainChance, double thunderChance, double clearChance) {
		this.englishName = englishName;
		this.hebrewName = hebrewName;
		this.icon = icon;
		this.rainChance = rainChance;
		this.thunderChance = thunderChance;
		this.clearChance = clearChance;
	}
}
