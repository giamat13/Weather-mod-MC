package com.weather.disaster;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A live tornado or hurricane. It wanders the world, sucks in and flings nearby
 * entities, and rips blocks up into flying {@link FallingBlockEntity falling blocks}.
 */
public class ActiveDisaster {
	private final DisasterType type;
	private final ServerLevel level;

	private double x;
	private double y;
	private double z;
	private double heading; // radians, the direction the funnel is wandering in
	private int ticksLived;

	public ActiveDisaster(DisasterType type, ServerLevel level, double x, double y, double z) {
		this.type = type;
		this.level = level;
		this.x = x;
		this.y = y;
		this.z = z;
		this.heading = level.getRandom().nextDouble() * Math.PI * 2.0;
	}

	public DisasterType type() {
		return type;
	}

	public ServerLevel level() {
		return level;
	}

	public double x() {
		return x;
	}

	public double z() {
		return z;
	}

	public boolean finished() {
		return ticksLived >= type.lifetimeTicks;
	}

	public void tick() {
		ticksLived++;
		RandomSource rng = level.getRandom();
		if (type == DisasterType.EARTHQUAKE) {
			tickEarthquake(rng);
		} else {
			tickVortex(rng);
		}
	}

	private void tickVortex(RandomSource rng) {
		// Wander: nudge the heading a little each tick so the path curves naturally.
		heading += (rng.nextDouble() - 0.5) * 0.3;
		x += Math.cos(heading) * type.driftPerTick;
		z += Math.sin(heading) * type.driftPerTick;
		// Follow the surface so the funnel stays anchored to the ground as it moves.
		y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));

		flingEntities(rng);
		tearUpBlocks(rng);
		spawnVisuals(rng);

		// Roaring wind / thunder ambience.
		if (ticksLived % 30 == 0) {
			level.playSound(null, x, y, z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER,
				type == DisasterType.HURRICANE ? 4.0f : 2.0f, 0.6f);
		}
	}

	/**
	 * An earthquake stays put at its epicentre, repeatedly bouncing nearby entities into
	 * the air and crumbling a share of the blocks in the surrounding chunks.
	 */
	private void tickEarthquake(RandomSource rng) {
		// Bounce entities every half-second, with height/distance scaled by the magnitude.
		if (ticksLived % 10 == 0) {
			bounceEntities(rng);
		}
		crumbleBlocks(rng);

		// Ground-shake rumble and dust.
		if (ticksLived % 8 == 0) {
			level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 3.0f, 0.4f);
		}
	}

	private void bounceEntities(RandomSource rng) {
		double r = type.radius;
		AABB box = new AABB(x - r, y - 8, z - r, x + r, y + 8, z + r);
		for (Entity e : level.getEntities((Entity) null, box, Entity::isAlive)) {
			double dx = e.getX() - x;
			double dz = e.getZ() - z;
			if (dx * dx + dz * dz > r * r) {
				continue;
			}
			double power = type.quakeTossPower;
			double vx = (rng.nextDouble() - 0.5) * power;
			double vz = (rng.nextDouble() - 0.5) * power;
			double vy = power * (0.6 + rng.nextDouble() * 0.6);
			e.setDeltaMovement(e.getDeltaMovement().add(vx, vy, vz));
			e.fallDistance = 0.0f;
			if (e instanceof ServerPlayer player) {
				player.connection.send(new ClientboundSetEntityMotionPacket(player));
			} else {
				e.hurtMarked = true;
			}
		}
	}

	private void crumbleBlocks(RandomSource rng) {
		double r = type.radius;
		for (int i = 0; i < type.quakeBreaksPerTick; i++) {
			int bx = Mth.floor(x + (rng.nextDouble() * 2 - 1) * r);
			int bz = Mth.floor(z + (rng.nextDouble() * 2 - 1) * r);
			int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, bx, bz);
			// Break a block at or just below the surface so the quake opens cracks and craters.
			int by = surface - 1 - rng.nextInt(3);
			BlockPos pos = new BlockPos(bx, by, bz);
			BlockState state = level.getBlockState(pos);
			if (state.isAir() || state.getDestroySpeed(level, pos) < 0.0f || !state.getFluidState().isEmpty()) {
				continue;
			}
			level.destroyBlock(pos, false, null, 512);
		}
	}

	private void flingEntities(RandomSource rng) {
		double r = type.radius;
		AABB box = new AABB(x - r, y - 4, z - r, x + r, y + type.height, z + r);
		List<Entity> entities = level.getEntities((Entity) null, box, e -> e.isAlive());
		for (Entity e : entities) {
			double dx = e.getX() - x;
			double dz = e.getZ() - z;
			double horiz = Math.sqrt(dx * dx + dz * dz);
			if (horiz > r) {
				continue;
			}
			double nx = horiz < 1.0e-4 ? rng.nextDouble() - 0.5 : dx / horiz;
			double nz = horiz < 1.0e-4 ? rng.nextDouble() - 0.5 : dz / horiz;

			// Tangential swirl (perpendicular to the radius) + gentle inward pull + strong lift.
			double swirl = type.swirlStrength;
			double pull = type.pullStrength * 0.4;
			double closeness = 1.0 - (horiz / r); // stronger near the eye
			double up = type.upStrength * (0.4 + 0.6 * closeness);

			double vx = (-nz * swirl) - (nx * pull);
			double vz = (nx * swirl) - (nz * pull);

			Vec3 add = new Vec3(clamp(vx), clamp(up), clamp(vz));
			e.setDeltaMovement(e.getDeltaMovement().add(add));
			e.fallDistance = 0.0f;

			if (e instanceof ServerPlayer player) {
				// Player motion is client-authoritative, so push it explicitly.
				player.connection.send(new ClientboundSetEntityMotionPacket(player));
			} else {
				e.hurtMarked = true;
			}
		}
	}

	private void tearUpBlocks(RandomSource rng) {
		double r = type.radius;
		for (int i = 0; i < type.blocksPerTick; i++) {
			double a = rng.nextDouble() * Math.PI * 2.0;
			double dist = rng.nextDouble() * r;
			int bx = (int) Math.floor(x + Math.cos(a) * dist);
			int bz = (int) Math.floor(z + Math.sin(a) * dist);

			// Find the topmost solid block in a small window around the funnel's foot.
			for (int yy = (int) Math.floor(y) + 3; yy >= (int) Math.floor(y) - 4; yy--) {
				BlockPos pos = new BlockPos(bx, yy, bz);
				BlockState state = level.getBlockState(pos);
				if (state.isAir() || !state.getFluidState().isEmpty()) {
					continue;
				}
				// Unbreakable blocks (bedrock, etc.) are immovable.
				float hardness = state.getDestroySpeed(level, pos);
				if (hardness < 0.0f) {
					break;
				}
				// The harder a block is to mine, the less likely the storm can rip it up.
				// A hurricane is stronger, so it grabs tougher blocks more often.
				double grabChance = type.blockGrabStrength / (1.0 + hardness);
				if (rng.nextDouble() > grabChance) {
					break;
				}
				// fall() removes the original block and spawns the falling-block entity for us.
				FallingBlockEntity block = FallingBlockEntity.fall(level, pos, state);
				if (type.blocksHurtEntities) {
					block.setHurtsEntities(3.0f, 12);
				}
				double swirl = type.swirlStrength;
				block.setDeltaMovement((rng.nextDouble() - 0.5) * swirl, type.upStrength, (rng.nextDouble() - 0.5) * swirl);
				block.hurtMarked = true;
				break;
			}
		}
	}

	private void spawnVisuals(RandomSource rng) {
		double r = type.radius;
		for (int h = 0; h < type.height; h += 2) {
			double t = (double) h / type.height;
			double ringRadius = r * (0.25 + 0.75 * t);
			double angle = (ticksLived * 0.35) + h * 0.6;
			double px = x + Math.cos(angle) * ringRadius;
			double pz = z + Math.sin(angle) * ringRadius;
			level.sendParticles(ParticleTypes.CLOUD, px, y + h, pz, 1, 0.2, 0.2, 0.2, 0.02);
			if (rng.nextInt(3) == 0) {
				level.sendParticles(ParticleTypes.LARGE_SMOKE, px, y + h, pz, 1, 0.1, 0.1, 0.1, 0.01);
			}
		}
	}

	private static double clamp(double v) {
		// Keep velocities forceful but not physics-breaking.
		double max = 3.0;
		return Math.max(-max, Math.min(max, v));
	}
}
