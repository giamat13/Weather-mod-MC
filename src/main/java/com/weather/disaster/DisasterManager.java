package com.weather.disaster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.weather.WeatherMod;
import com.weather.block.TornadoHurricaneAlerterBlock;
import com.weather.registry.ModRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side brain of the mod. Ticked once per server tick, it:
 * <ul>
 *   <li>rolls a 15% chance of a tornado/hurricane whenever a thunderstorm begins,</li>
 *   <li>schedules disasters two minutes ahead so Alerters can warn,</li>
 *   <li>promotes scheduled disasters into live ones and ticks them,</li>
 *   <li>scans for Alerter multiblocks near players and drives their warning lights.</li>
 * </ul>
 */
public class DisasterManager {
	/** Two minutes of warning before a storm-spawned disaster strikes. */
	public static final int WARNING_LEAD_TICKS = 2 * 60 * 20;
	private static final double STORM_DISASTER_CHANCE = 0.15;
	private static final double HURRICANE_SHARE = 0.4;

	private static final int ALERTER_SCAN_INTERVAL = 20; // once per second
	private static final int SCAN_RADIUS_H = 24;
	private static final int SCAN_RADIUS_V = 12;

	/** How often a red-alert alerter beeps, and how close a player must be to trigger it. */
	private static final int BEEP_INTERVAL = 5; // ~4 beeps per second
	private static final double BEEP_RANGE = 24.0;

	private long tick;
	private final Random random = new Random();

	private final List<ScheduledDisaster> pending = new ArrayList<>();
	private final List<ActiveDisaster> active = new ArrayList<>();

	/** Tracks which levels are currently thundering, to detect the start of a storm. */
	private final Map<ServerLevel, Boolean> thundering = new HashMap<>();
	/** Alerters we've lit up, so we can reset them to green once a threat passes. */
	private final Map<ServerLevel, Set<BlockPos>> knownAlerters = new HashMap<>();
	/** Alerters currently at red alert, beeped rapidly while a player is close. */
	private final Map<ServerLevel, Set<BlockPos>> redAlerters = new HashMap<>();

	public void onEndServerTick(MinecraftServer server) {
		tick++;

		detectStormStarts(server);
		promoteScheduled();
		tickActive();

		if (tick % ALERTER_SCAN_INTERVAL == 0) {
			updateAlerters(server);
		}
		if (tick % BEEP_INTERVAL == 0) {
			beepRedAlerters(server);
		}
	}

	/** Manually queue a disaster (used by the /naturaldisasters command). */
	public void schedule(DisasterType type, ServerLevel level, double x, double y, double z, int delayTicks) {
		pending.add(new ScheduledDisaster(type, level, x, y, z, tick + Math.max(0, delayTicks)));
	}

	public int activeCount() {
		return active.size();
	}

	public int pendingCount() {
		return pending.size();
	}

	// --- storm spawning -------------------------------------------------------

	private void detectStormStarts(MinecraftServer server) {
		for (ServerLevel level : server.getAllLevels()) {
			boolean now = level.isThundering();
			boolean was = thundering.getOrDefault(level, false);
			if (now && !was) {
				maybeSpawnFromStorm(level);
			}
			thundering.put(level, now);
		}
	}

	private void maybeSpawnFromStorm(ServerLevel level) {
		if (random.nextDouble() >= STORM_DISASTER_CHANCE) {
			return;
		}
		List<ServerPlayer> players = level.players();
		if (players.isEmpty()) {
			return;
		}
		ServerPlayer target = players.get(random.nextInt(players.size()));
		DisasterType type = random.nextDouble() < HURRICANE_SHARE ? DisasterType.HURRICANE : DisasterType.TORNADO;

		// Somewhere 80–200 blocks from a random player, so the Alerter has something to warn about.
		double angle = random.nextDouble() * Math.PI * 2.0;
		double dist = 80 + random.nextDouble() * 120;
		double x = target.getX() + Math.cos(angle) * dist;
		double z = target.getZ() + Math.sin(angle) * dist;
		double y = target.getY();

		schedule(type, level, x, y, z, WARNING_LEAD_TICKS);
		WeatherMod.LOGGER.info("Storm in {} will spawn a {} in 2 minutes at [{}, {}]",
			level.dimension().identifier(), type.englishName, (int) x, (int) z);
	}

	// --- disaster lifecycle ---------------------------------------------------

	private void promoteScheduled() {
		Iterator<ScheduledDisaster> it = pending.iterator();
		while (it.hasNext()) {
			ScheduledDisaster s = it.next();
			if (tick >= s.startTick()) {
				active.add(new ActiveDisaster(s.type(), s.level(), s.x(), s.y(), s.z()));
				it.remove();
				broadcast(s.level(), "§c⚠ " + hebrewAnnouncement(s.type(), true));
			}
		}
	}

	private void tickActive() {
		Iterator<ActiveDisaster> it = active.iterator();
		while (it.hasNext()) {
			ActiveDisaster d = it.next();
			d.tick();
			if (d.finished()) {
				it.remove();
				broadcast(d.level(), "§a" + hebrewAnnouncement(d.type(), false));
			}
		}
	}

	// --- alerter warning lights ----------------------------------------------

	private void updateAlerters(MinecraftServer server) {
		for (ServerLevel level : server.getAllLevels()) {
			List<double[]> threats = threatPositionsIn(level);
			if (threats.isEmpty()) {
				resetLevel(level);
				continue;
			}

			Set<BlockPos> found = new HashSet<>();
			Set<BlockPos> reds = new HashSet<>();
			for (ServerPlayer player : level.players()) {
				scanAround(level, player.blockPosition(), threats, found, reds);
			}
			knownAlerters.computeIfAbsent(level, l -> new HashSet<>()).addAll(found);
			redAlerters.put(level, reds);
		}
	}

	private List<double[]> threatPositionsIn(ServerLevel level) {
		List<double[]> threats = new ArrayList<>();
		for (ScheduledDisaster s : pending) {
			if (s.level() == level) {
				threats.add(new double[] { s.x(), s.z() });
			}
		}
		for (ActiveDisaster d : active) {
			if (d.level() == level) {
				threats.add(new double[] { d.x(), d.z() });
			}
		}
		return threats;
	}

	private void scanAround(ServerLevel level, BlockPos around, List<double[]> threats, Set<BlockPos> found, Set<BlockPos> reds) {
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int dx = -SCAN_RADIUS_H; dx <= SCAN_RADIUS_H; dx++) {
			for (int dy = -SCAN_RADIUS_V; dy <= SCAN_RADIUS_V; dy++) {
				for (int dz = -SCAN_RADIUS_H; dz <= SCAN_RADIUS_H; dz++) {
					cursor.set(around.getX() + dx, around.getY() + dy, around.getZ() + dz);
					if (level.getBlockState(cursor).getBlock() != ModRegistry.TORNADO_HURRICANE_ALERTER) {
						continue;
					}
					BlockPos pos = cursor.immutable();
					if (found.contains(pos)) {
						continue;
					}
					int warning = Alerter.warningFor(nearestDistance(pos, threats));
					Alerter.apply(level, pos, warning);
					found.add(pos);
					if (warning == TornadoHurricaneAlerterBlock.WARNING_RED) {
						reds.add(pos);
					}
				}
			}
		}
	}

	private double nearestDistance(BlockPos centre, List<double[]> threats) {
		double best = Double.MAX_VALUE;
		for (double[] t : threats) {
			double dx = centre.getX() - t[0];
			double dz = centre.getZ() - t[1];
			best = Math.min(best, Math.sqrt(dx * dx + dz * dz));
		}
		return best;
	}

	private void resetLevel(ServerLevel level) {
		Set<BlockPos> reds = redAlerters.get(level);
		if (reds != null) {
			reds.clear();
		}
		Set<BlockPos> known = knownAlerters.get(level);
		if (known == null || known.isEmpty()) {
			return;
		}
		for (BlockPos pos : known) {
			Alerter.apply(level, pos, TornadoHurricaneAlerterBlock.WARNING_NONE);
		}
		known.clear();
	}

	/** Beep every red-alert alerter that has a player within earshot. */
	private void beepRedAlerters(MinecraftServer server) {
		for (ServerLevel level : server.getAllLevels()) {
			Set<BlockPos> reds = redAlerters.get(level);
			if (reds == null || reds.isEmpty()) {
				continue;
			}
			List<ServerPlayer> players = level.players();
			if (players.isEmpty()) {
				continue;
			}
			for (BlockPos pos : reds) {
				if (hasPlayerWithin(players, pos, BEEP_RANGE)) {
					Alerter.beep(level, pos);
				}
			}
		}
	}

	private boolean hasPlayerWithin(List<ServerPlayer> players, BlockPos pos, double range) {
		double r2 = range * range;
		for (ServerPlayer player : players) {
			double dx = player.getX() - (pos.getX() + 0.5);
			double dy = player.getY() - (pos.getY() + 0.5);
			double dz = player.getZ() - (pos.getZ() + 0.5);
			if (dx * dx + dy * dy + dz * dz <= r2) {
				return true;
			}
		}
		return false;
	}

	// --- helpers --------------------------------------------------------------

	private void broadcast(ServerLevel level, String message) {
		Component component = Component.literal(message);
		for (ServerPlayer player : level.players()) {
			player.sendSystemMessage(component);
		}
	}

	private String hebrewAnnouncement(DisasterType type, boolean starting) {
		if (starting) {
			return type.hebrewName + " פגע! חפשו מחסה!";
		}
		return "ה" + type.hebrewName + " חלף.";
	}
}
