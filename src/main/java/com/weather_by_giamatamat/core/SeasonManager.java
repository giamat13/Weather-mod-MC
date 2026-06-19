package com.weather_by_giamatamat.core;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * Tracks the day-of-season counter and rolls the season over every
 * {@link ModConfig#SEASON_LENGTH_DAYS} Minecraft days. Each new day the season biases the weather
 * (chance of rain / thunder).
 */
public final class SeasonManager {
	private SeasonManager() {
	}

	/** Called every server tick with the current absolute day number. Advances the season on rollover. */
	public static void processDay(final MinecraftServer server, final DisasterGlobalData data, final long day) {
		if (data.getLastDayProcessed() == day) {
			return;
		}
		boolean firstObservation = data.getLastDayProcessed() < 0;
		data.setLastDayProcessed(day);
		if (firstObservation) {
			return;
		}
		int seasonDay = data.getSeasonDay() + 1;
		if (seasonDay >= ModConfig.SEASON_LENGTH_DAYS) {
			Season next = data.getSeason().next();
			data.setSeason(next);
			data.setSeasonDay(0);
			announce(server, "The season has changed to " + next.displayName() + "!");
		} else {
			data.setSeasonDay(seasonDay);
		}
		applySeasonWeather(server, data);
	}

	public static void setSeason(final MinecraftServer server, final DisasterGlobalData data, final Season season) {
		data.setSeason(season);
		data.setSeasonDay(0);
		announce(server, "The season is now " + season.displayName() + ".");
		applySeasonWeather(server, data);
	}

	private static void applySeasonWeather(final MinecraftServer server, final DisasterGlobalData data) {
		ServerLevel overworld = server.getLevel(Level.OVERWORLD);
		if (overworld == null) {
			return;
		}
		RandomSource random = overworld.getRandom();
		Season season = data.getSeason();
		if (random.nextFloat() < season.rainBias()) {
			int duration = 6000 + random.nextInt(6000);
			boolean thunder = (season == Season.WINTER || season == Season.AUTUMN) && random.nextFloat() < 0.3F;
			server.setWeatherParameters(0, duration, true, thunder);
		} else {
			server.setWeatherParameters(6000 + random.nextInt(6000), 0, false, false);
		}
	}

	private static void announce(final MinecraftServer server, final String message) {
		server.getPlayerList().broadcastSystemMessage(
			Component.literal(message).withStyle(ChatFormatting.GOLD), false);
	}
}
