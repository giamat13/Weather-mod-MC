package com.weather.season;

import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

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

		// Summer heat can spontaneously set flammable blocks alight (very rarely).
		if (season == Season.SUMMER) {
			summerIgnition(server);
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

	// 0.000009% per attempt that a random flammable block near a player catches fire in summer.
	private static final double SUMMER_FIRE_CHANCE = 0.00000009;
	private static final int SUMMER_FIRE_SAMPLES = 16;
	private static final int SUMMER_FIRE_RADIUS = 40;

	private void summerIgnition(MinecraftServer server) {
		for (ServerLevel level : server.getAllLevels()) {
			for (ServerPlayer player : level.players()) {
				for (int i = 0; i < SUMMER_FIRE_SAMPLES; i++) {
					int bx = player.blockPosition().getX() + random.nextInt(SUMMER_FIRE_RADIUS * 2 + 1) - SUMMER_FIRE_RADIUS;
					int bz = player.blockPosition().getZ() + random.nextInt(SUMMER_FIRE_RADIUS * 2 + 1) - SUMMER_FIRE_RADIUS;
					int by = player.blockPosition().getY() + random.nextInt(17) - 8;
					BlockPos pos = new BlockPos(bx, by, bz);
					BlockState state = level.getBlockState(pos);
					if (state.ignitedByLava() && level.getBlockState(pos.above()).isAir()
							&& random.nextDouble() < SUMMER_FIRE_CHANCE) {
						level.setBlockAndUpdate(pos.above(), Blocks.FIRE.defaultBlockState());
					}
				}
			}
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
