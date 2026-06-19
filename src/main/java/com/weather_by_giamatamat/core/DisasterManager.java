package com.weather_by_giamatamat.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.weather_by_giamatamat.alerter.Alerter;
import com.weather_by_giamatamat.alerter.AlerterManager;
import com.weather_by_giamatamat.alerter.DisasterLevelData;
import com.weather_by_giamatamat.disaster.Disaster;
import com.weather_by_giamatamat.disaster.EarthquakeDisaster;
import com.weather_by_giamatamat.disaster.MeteorDisaster;
import com.weather_by_giamatamat.disaster.SandstormHandler;
import com.weather_by_giamatamat.disaster.VolcanoDisaster;
import com.weather_by_giamatamat.disaster.WildfireDisaster;
import com.weather_by_giamatamat.disaster.WindstormDisaster;
import com.weather_by_giamatamat.util.WorldUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

/**
 * Central orchestrator: ticks active disasters, schedules storms / meteors, drives the season clock,
 * runs per-level effects (alerters, sandstorms, wildfires) and exposes spawn/trigger helpers.
 * State that must survive a restart lives in {@link DisasterGlobalData} / {@link DisasterLevelData};
 * the in-flight disaster list is transient.
 */
public final class DisasterManager {
	private static final List<Disaster> ACTIVE = new ArrayList<>();
	/** Disasters spawned during a tick are queued here and merged after iteration (avoids CME). */
	private static final List<Disaster> PENDING = new ArrayList<>();
	private static final Map<ResourceKey<Level>, Boolean> PREV_RAINING = new HashMap<>();

	private DisasterManager() {
	}

	// --- Lifecycle ----------------------------------------------------------

	public static void onServerStarted(final MinecraftServer server) {
		ACTIVE.clear();
		PENDING.clear();
		PREV_RAINING.clear();
		DisasterGlobalData data = DisasterGlobalData.get(server);
		if (data.getMeteorTimer() <= 0) {
			data.setMeteorTimer(randomMeteorInterval(server));
		}
	}

	public static void onServerStopping(final MinecraftServer server) {
		ACTIVE.clear();
		PENDING.clear();
		PREV_RAINING.clear();
	}

	public static List<Disaster> active() {
		return ACTIVE;
	}

	public static boolean isEnabled(final MinecraftServer server) {
		return DisasterGlobalData.get(server).isEnabled();
	}

	public static void spawn(final Disaster disaster) {
		PENDING.add(disaster);
	}

	// --- Server tick (global cadence) --------------------------------------

	public static void onServerTick(final MinecraftServer server) {
		DisasterGlobalData data = DisasterGlobalData.get(server);
		if (!data.isEnabled()) {
			if (!ACTIVE.isEmpty() || !PENDING.isEmpty()) {
				ACTIVE.clear();
				PENDING.clear();
			}
			return;
		}

		Iterator<Disaster> it = ACTIVE.iterator();
		while (it.hasNext()) {
			Disaster disaster = it.next();
			disaster.tick();
			if (disaster.isFinished()) {
				it.remove();
			}
		}
		// Merge disasters spawned during this tick (e.g. an earthquake spawning a geyser field).
		if (!PENDING.isEmpty()) {
			ACTIVE.addAll(PENDING);
			PENDING.clear();
		}

		ServerLevel overworld = server.getLevel(Level.OVERWORLD);
		if (overworld != null) {
			long day = overworld.getGameTime() / ModConfig.TICKS_PER_DAY;
			SeasonManager.processDay(server, data, day);

			int timer = data.getMeteorTimer() - 1;
			if (timer <= 0) {
				if (overworld.getRandom().nextDouble() < ModConfig.METEOR_CHANCE) {
					spawnMeteorNearPlayer(overworld);
				}
				timer = randomMeteorInterval(server);
			}
			data.setMeteorTimer(timer);

			if (overworld.getRandom().nextDouble() < ModConfig.EARTHQUAKE_TICK_CHANCE) {
				spawnEarthquakeNearPlayer(overworld);
			}
		}
	}

	public static void spawnEarthquakeNearPlayer(final ServerLevel level) {
		ServerPlayer player = randomPlayer(level);
		if (player == null) {
			return;
		}
		int magnitude = level.getRandom().nextIntBetweenInclusive(
			ModConfig.EARTHQUAKE_MIN_MAGNITUDE, ModConfig.EARTHQUAKE_MAX_MAGNITUDE);
		Vec3 center = offsetSurface(level, player.position(), 20, 90);
		spawn(new EarthquakeDisaster(level, center, magnitude));
		announceNear(level, center, "An earthquake (magnitude " + magnitude + ") is about to strike!");
	}

	private static int randomMeteorInterval(final MinecraftServer server) {
		ServerLevel overworld = server.getLevel(Level.OVERWORLD);
		RandomSource random = overworld != null ? overworld.getRandom() : RandomSource.create();
		return random.nextIntBetweenInclusive(ModConfig.METEOR_INTERVAL_MIN_TICKS, ModConfig.METEOR_INTERVAL_MAX_TICKS);
	}

	// --- Per-level tick -----------------------------------------------------

	public static void onLevelTick(final ServerLevel level) {
		DisasterGlobalData data = DisasterGlobalData.get(level.getServer());
		if (!data.isEnabled()) {
			return;
		}
		long time = level.getGameTime();

		boolean raining = level.isRaining();
		ResourceKey<Level> key = level.dimension();
		boolean wasRaining = PREV_RAINING.getOrDefault(key, false);
		if (raining && !wasRaining) {
			onStormStart(level);
		}
		PREV_RAINING.put(key, raining);

		if (time % ModConfig.ALERTER_TICK_INTERVAL == 0) {
			updateAlerters(level);
		}
		if (time % 10 == 0) {
			SandstormHandler.tick(level, time);
		}
		if (time % 60 == 0) {
			fireScan(level, data.getSeason());
		}
	}

	private static void onStormStart(final ServerLevel level) {
		RandomSource random = level.getRandom();
		if (random.nextDouble() < ModConfig.STORM_DISASTER_CHANCE) {
			boolean hurricane = random.nextDouble() < ModConfig.HURRICANE_SHARE;
			spawnWindstormNearPlayer(level, hurricane);
		}
	}

	private static void updateAlerters(final ServerLevel level) {
		DisasterLevelData data = DisasterLevelData.get(level);
		Iterator<Alerter> it = data.alerters().iterator();
		boolean changed = false;
		while (it.hasNext()) {
			Alerter alerter = it.next();
			if (!AlerterManager.updateAlerter(level, alerter, ACTIVE)) {
				it.remove();
				changed = true;
			}
		}
		if (changed) {
			data.setDirty();
		}
	}

	// --- Wildfire scanning --------------------------------------------------

	private static void fireScan(final ServerLevel level, final Season season) {
		RandomSource random = level.getRandom();
		for (ServerPlayer player : level.players()) {
			for (int i = 0; i < 8; i++) {
				int x = player.blockPosition().getX() + random.nextInt(96) - 48;
				int z = player.blockPosition().getZ() + random.nextInt(96) - 48;
				if (!level.hasChunk(x >> 4, z >> 4)) {
					continue;
				}
				int surface = WorldUtil.surfaceY(level, x, z);
				BlockPos ground = new BlockPos(x, surface - 1, z);
				BlockPos above = new BlockPos(x, surface, z);

				// Forest wildfire detection: any fire already burning in a forest counts as a wildfire.
				if (level.getBiome(ground).is(BiomeTags.IS_FOREST) && level.getBlockState(above).is(Blocks.FIRE)) {
					reportFire(level, Vec3.atCenterOf(above));
				}

				// Summer spontaneous ignition on flammable blocks (astronomically rare, per spec).
				if (season == Season.SUMMER
						&& level.getBlockState(ground).ignitedByLava()
						&& level.getBlockState(above).isAir()
						&& random.nextDouble() < ModConfig.FIRE_SUMMER_IGNITE_CHANCE) {
					level.setBlockAndUpdate(above, Blocks.FIRE.defaultBlockState());
					reportFire(level, Vec3.atCenterOf(above));
				}
			}
		}
	}

	private static void reportFire(final ServerLevel level, final Vec3 pos) {
		if (hasNearbyFire(ACTIVE, level, pos) || hasNearbyFire(PENDING, level, pos)) {
			return; // already tracking a wildfire here
		}
		spawn(new WildfireDisaster(level, pos));
	}

	private static boolean hasNearbyFire(final List<Disaster> list, final ServerLevel level, final Vec3 pos) {
		for (Disaster disaster : list) {
			if (disaster.type() == DisasterType.FIRE && disaster.level() == level
					&& disaster.center().distanceToSqr(pos) < 32 * 32) {
				return true;
			}
		}
		return false;
	}

	// --- Spawn helpers ------------------------------------------------------

	public static @Nullable ServerPlayer randomPlayer(final ServerLevel level) {
		List<ServerPlayer> players = level.players();
		if (players.isEmpty()) {
			return null;
		}
		return players.get(level.getRandom().nextInt(players.size()));
	}

	public static void spawnWindstormNearPlayer(final ServerLevel level, final boolean hurricane) {
		ServerPlayer player = randomPlayer(level);
		if (player == null) {
			return;
		}
		Vec3 center = offsetSurface(level, player.position(), 60, 180);
		spawn(new WindstormDisaster(level, hurricane ? DisasterType.HURRICANE : DisasterType.TORNADO, center));
		announceNear(level, center, (hurricane ? "A hurricane" : "A tornado") + " is forming nearby!");
	}

	public static void spawnMeteorNearPlayer(final ServerLevel level) {
		ServerPlayer player = randomPlayer(level);
		if (player == null) {
			return;
		}
		int renderDistance = level.getServer().getPlayerList().getViewDistance() * 16;
		Vec3 center = offsetSurface(level, player.position(), Math.max(80, renderDistance - 32), renderDistance + 16);
		spawn(new MeteorDisaster(level, center));
		announceNear(level, center, "A meteor has been detected on approach!");
	}

	private static Vec3 offsetSurface(final ServerLevel level, final Vec3 from, final double min, final double max) {
		RandomSource random = level.getRandom();
		double angle = random.nextDouble() * Math.PI * 2.0;
		double dist = min + random.nextDouble() * (max - min);
		int x = (int) (from.x + Math.cos(angle) * dist);
		int z = (int) (from.z + Math.sin(angle) * dist);
		int y = WorldUtil.surfaceY(level, x, z);
		return new Vec3(x + 0.5, y, z + 0.5);
	}

	// --- Earthquake aftermath: geyser fields / volcanic eruptions -----------

	public static void onEarthquakeImpact(final ServerLevel level, final BlockPos epicenter, final int magnitude) {
		double chance = ModConfig.GEYSER_FIELD_BASE_CHANCE
			* biomeGeyserMultiplier(level, epicenter)
			* (magnitude / 5.0);
		if (level.getRandom().nextDouble() < chance) {
			int centerChunkX = epicenter.getX() >> 4;
			int centerChunkZ = epicenter.getZ() >> 4;
			spawn(new VolcanoDisaster(level, centerChunkX, centerChunkZ, ModConfig.GEYSER_REGION_CHUNKS));
			announceNear(level, Vec3.atCenterOf(epicenter), "A geyser field is erupting from the quake!");
		}
	}

	private static double biomeGeyserMultiplier(final ServerLevel level, final BlockPos pos) {
		var biome = level.getBiome(pos);
		if (biome.is(Biomes.DESERT) || biome.is(BiomeTags.IS_BADLANDS) || biome.is(BiomeTags.IS_SAVANNA)) {
			return 2.0;
		}
		if (biome.is(BiomeTags.IS_OCEAN)) {
			return 0.3;
		}
		return 1.0;
	}

	// --- Command-triggered disasters ---------------------------------------

	public static void trigger(final ServerLevel level, final DisasterType type, final Vec3 pos, final int magnitude) {
		switch (type) {
			case TORNADO -> spawn(new WindstormDisaster(level, DisasterType.TORNADO, pos));
			case HURRICANE -> spawn(new WindstormDisaster(level, DisasterType.HURRICANE, pos));
			case EARTHQUAKE -> spawn(new EarthquakeDisaster(level, pos, magnitude));
			case METEOR -> spawn(new MeteorDisaster(level, pos));
			case VOLCANO -> spawn(new VolcanoDisaster(level, ((int) pos.x) >> 4, ((int) pos.z) >> 4, ModConfig.GEYSER_REGION_CHUNKS));
			case FIRE -> {
				level.setBlockAndUpdate(BlockPos.containing(pos.x, pos.y, pos.z), Blocks.FIRE.defaultBlockState());
				spawn(new WildfireDisaster(level, pos));
			}
			case SANDSTORM -> level.getServer().setWeatherParameters(0, 6000, true, false);
		}
	}

	private static void announceNear(final ServerLevel level, final Vec3 pos, final String message) {
		Component component = Component.literal("⚠ " + message).withStyle(ChatFormatting.RED);
		for (ServerPlayer player : level.players()) {
			if (player.position().distanceToSqr(pos) < 600 * 600) {
				player.sendSystemMessage(component);
			}
		}
	}
}
