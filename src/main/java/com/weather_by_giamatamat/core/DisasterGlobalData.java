package com.weather_by_giamatamat.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Server-wide persistent state: master on/off switch, the current season and its progress,
 * and the rolling countdown to the next meteor check.
 */
public class DisasterGlobalData extends SavedData {
	private static final Codec<Season> SEASON_CODEC = Codec.STRING.xmap(
		name -> {
			Season season = Season.byName(name);
			return season == null ? Season.SPRING : season;
		},
		Season::name
	);

	public static final Codec<DisasterGlobalData> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
				Codec.BOOL.optionalFieldOf("enabled", ModConfig.ENABLED_BY_DEFAULT).forGetter(d -> d.enabled),
				SEASON_CODEC.optionalFieldOf("season", Season.SPRING).forGetter(d -> d.season),
				Codec.INT.optionalFieldOf("seasonDay", 0).forGetter(d -> d.seasonDay),
				Codec.LONG.optionalFieldOf("lastDayProcessed", -1L).forGetter(d -> d.lastDayProcessed),
				Codec.INT.optionalFieldOf("meteorTimer", 0).forGetter(d -> d.meteorTimer)
			)
			.apply(instance, DisasterGlobalData::new)
	);

	public static final SavedDataType<DisasterGlobalData> TYPE = new SavedDataType<>(
		Identifier.fromNamespaceAndPath("weather-by-giamatamat", "global"),
		DisasterGlobalData::new,
		CODEC,
		DataFixTypes.SAVED_DATA_RAIDS
	);

	private boolean enabled = ModConfig.ENABLED_BY_DEFAULT;
	private Season season = Season.SPRING;
	private int seasonDay = 0;
	private long lastDayProcessed = -1L;
	private int meteorTimer = 0;

	public DisasterGlobalData() {
	}

	private DisasterGlobalData(final boolean enabled, final Season season, final int seasonDay,
			final long lastDayProcessed, final int meteorTimer) {
		this.enabled = enabled;
		this.season = season;
		this.seasonDay = seasonDay;
		this.lastDayProcessed = lastDayProcessed;
		this.meteorTimer = meteorTimer;
	}

	public static DisasterGlobalData get(final MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(final boolean enabled) {
		this.enabled = enabled;
		setDirty();
	}

	public Season getSeason() {
		return this.season;
	}

	public void setSeason(final Season season) {
		this.season = season;
		setDirty();
	}

	public int getSeasonDay() {
		return this.seasonDay;
	}

	public void setSeasonDay(final int seasonDay) {
		this.seasonDay = seasonDay;
		setDirty();
	}

	public long getLastDayProcessed() {
		return this.lastDayProcessed;
	}

	public void setLastDayProcessed(final long lastDayProcessed) {
		this.lastDayProcessed = lastDayProcessed;
		setDirty();
	}

	public int getMeteorTimer() {
		return this.meteorTimer;
	}

	public void setMeteorTimer(final int meteorTimer) {
		this.meteorTimer = meteorTimer;
		setDirty();
	}
}
