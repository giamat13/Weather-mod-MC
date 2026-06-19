package com.weather_by_giamatamat.util;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Geometry / entity helpers shared by the disasters.
 */
public final class WorldUtil {
	private WorldUtil() {
	}

	public static int surfaceY(final ServerLevel level, final int x, final int z) {
		return level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
	}

	public static BlockPos surfacePos(final ServerLevel level, final int x, final int z) {
		return new BlockPos(x, surfaceY(level, x, z), z);
	}

	/** Pushes an entity away from {@code from} horizontally and upward, syncing to players. */
	public static void fling(final Entity entity, final Vec3 from, final double horizontal, final double vertical) {
		Vec3 p = entity.position();
		double dx = p.x - from.x;
		double dz = p.z - from.z;
		double dist = Math.sqrt(dx * dx + dz * dz);
		double nx;
		double nz;
		if (dist < 1.0e-3) {
			double angle = entity.level().getRandom().nextDouble() * Math.PI * 2.0;
			nx = Math.cos(angle);
			nz = Math.sin(angle);
		} else {
			nx = dx / dist;
			nz = dz / dist;
		}
		Vec3 v = entity.getDeltaMovement();
		entity.setDeltaMovement(v.x + nx * horizontal, Math.max(v.y, 0.0) + vertical, v.z + nz * horizontal);
		markMoved(entity);
	}

	/** Applies an explicit velocity impulse and syncs it to players. */
	public static void impulse(final Entity entity, final double vx, final double vy, final double vz) {
		Vec3 v = entity.getDeltaMovement();
		entity.setDeltaMovement(v.x + vx, v.y + vy, v.z + vz);
		markMoved(entity);
	}

	/** Sets an absolute velocity and syncs it to players. */
	public static void setVelocity(final Entity entity, final double vx, final double vy, final double vz) {
		entity.setDeltaMovement(vx, vy, vz);
		markMoved(entity);
	}

	private static void markMoved(final Entity entity) {
		entity.hurtMarked = true;
		if (entity instanceof ServerPlayer player) {
			player.connection.send(new ClientboundSetEntityMotionPacket(player));
		}
	}
}
