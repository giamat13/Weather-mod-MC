package com.weather.season;

import java.util.Random;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Tracks the current season from the overworld day clock — one season per
 * {@value #DAYS_PER_SEASON} Minecraft days — and biases the weather accordingly.
 *
 * <p>The season is derived from the persistent world time, so it survives restarts.
 * {@code /season} can shift it via an in-memory offset for the current session.
 */
public class SeasonManager {
	public static final long DAYS_PER_SEASON = 30L;
	private static final long TICKS_PER_DAY = 24000L;

	private final Random random = new Random();

	/** Manual shift applied on top of the time-derived season (reset on restart). */
	private int manualOffset;
	private Season lastSeason;
	private long lastDay = Long.MIN_VALUE;

	public void onEndServerTick(MinecraftServer server) {
		ServerLevel overworld = server.overworld();
		if (overworld == null) {
			return;
		}
		long day = overworld.getOverworldClockTime() / TICKS_PER_DAY;
		Season season = seasonForDay(day);

		if (lastSeason == null) {
			lastSeason = season;
		} else if (season != lastSeason) {
			lastSeason = season;
			announce(server, season);
		}

		// Once per new day, let the season nudge the weather.
		if (day != lastDay) {
			lastDay = day;
			applyWeather(server, overworld, season);
		}
	}

	public Season current(MinecraftServer server) {
		long day = server.overworld().getOverworldClockTime() / TICKS_PER_DAY;
		return seasonForDay(day);
	}

	public long daysIntoSeason(MinecraftServer server) {
		return (server.overworld().getOverworldClockTime() / TICKS_PER_DAY) % DAYS_PER_SEASON;
	}

	/** Shift the season so the current one becomes {@code target} (until restart). */
	public void setSeason(MinecraftServer server, Season target) {
		long day = server.overworld().getOverworldClockTime() / TICKS_PER_DAY;
		int base = baseIndex(day);
		manualOffset = Math.floorMod(target.ordinal() - base, 4);
		lastSeason = target;
		announce(server, target);
	}

	private Season seasonForDay(long day) {
		return Season.values()[Math.floorMod(baseIndex(day) + manualOffset, 4)];
	}

	private int baseIndex(long day) {
		return (int) Math.floorMod(day / DAYS_PER_SEASON, 4);
	}

	private void applyWeather(MinecraftServer server, ServerLevel overworld, Season season) {
		if (!overworld.isRaining()) {
			if (random.nextDouble() < season.rainChance) {
				int rainTime = 2000 + random.nextInt(8000);
				boolean thunder = random.nextDouble() < season.thunderChance;
				server.setWeatherParameters(0, rainTime, true, thunder);
			}
		} else if (random.nextDouble() < season.clearChance) {
			server.setWeatherParameters(3000 + random.nextInt(6000), 0, false, false);
		}
	}

	private void announce(MinecraftServer server, Season season) {
		Component message = Component.literal("§6" + season.icon + " " + seasonArrivalMessage(season));
		for (ServerLevel level : server.getAllLevels()) {
			for (ServerPlayer player : level.players()) {
				player.sendSystemMessage(message);
			}
		}
	}

	private String seasonArrivalMessage(Season season) {
		return "ה" + season.hebrewName + " הגיע!";
	}
}
