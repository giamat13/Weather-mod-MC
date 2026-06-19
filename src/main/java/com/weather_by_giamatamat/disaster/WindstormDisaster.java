package com.weather_by_giamatamat.disaster;

import java.util.List;

import com.weather_by_giamatamat.core.DisasterType;
import com.weather_by_giamatamat.core.ModConfig;
import com.weather_by_giamatamat.util.Fx;
import com.weather_by_giamatamat.util.WorldUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A tornado or hurricane. Forms during a storm, travels across the surface, flings entities into
 * its swirling funnel, and rips up surface blocks (turning them into thrown falling-blocks).
 * Hurricanes are larger and stronger than tornadoes.
 */
public class WindstormDisaster extends Disaster {
	private final boolean hurricane;
	private final double radius;
	private double windAngle;
	private double windSpeed;

	public WindstormDisaster(final ServerLevel level, final DisasterType type, final Vec3 center) {
		super(level, type, center);
		this.hurricane = type == DisasterType.HURRICANE;
		this.radius = hurricane ? ModConfig.HURRICANE_RADIUS : ModConfig.TORNADO_RADIUS;
		RandomSource random = level.getRandom();
		this.windAngle = random.nextDouble() * Math.PI * 2.0;
		this.windSpeed = ModConfig.TORNADO_MOVE_SPEED * (hurricane ? 1.4 : 1.0);
	}

	@Override
	public double radius() {
		return this.radius;
	}

	@Override
	protected int warnTicks() {
		return ModConfig.WARN_TICKS;
	}

	@Override
	protected int activeTicks() {
		return hurricane ? ModConfig.HURRICANE_ACTIVE_TICKS : ModConfig.TORNADO_ACTIVE_TICKS;
	}

	@Override
	protected void onWarnTick() {
		// Foreshadow during the final 15 seconds: dark clouds gather over the predicted touchdown.
		if (ticksUntilImpact() <= 15 * ModConfig.TICKS_PER_SECOND && age % 4 == 0) {
			double y = WorldUtil.surfaceY(level, (int) center.x, (int) center.z) + 14;
			Fx.particles(level, ParticleTypes.LARGE_SMOKE, center.x, y, center.z, 6, radius * 0.4, 2.0, radius * 0.4, 0.02);
		}
	}

	@Override
	protected void onImpact() {
		Fx.sound(level, center, SoundEvents.WITHER_SPAWN, SoundSource.WEATHER, 4.0F, hurricane ? 0.4F : 0.7F);
	}

	@Override
	protected void onActiveTick() {
		moveFunnel();
		drawFunnel();
		flingEntities();
		if (age % 3 == 0) {
			ripUpBlocks();
		}
		if (age % 20 == 0) {
			Fx.sound(level, center, SoundEvents.WEATHER_RAIN, SoundSource.WEATHER, 4.0F, 0.5F);
		}
	}

	private void moveFunnel() {
		RandomSource random = level.getRandom();
		this.windAngle += (random.nextDouble() - 0.5) * 0.15;
		double nx = center.x + Math.cos(windAngle) * windSpeed;
		double nz = center.z + Math.sin(windAngle) * windSpeed;
		double ny = WorldUtil.surfaceY(level, (int) nx, (int) nz);
		this.center = new Vec3(nx, ny, nz);
	}

	private void drawFunnel() {
		int height = (int) (radius * 2.4);
		for (int h = 0; h < height; h += 2) {
			double t = h / (double) height;
			double r = radius * (0.15 + 0.85 * t);
			int points = 2 + (int) (r / 3.0);
			for (int k = 0; k < points; k++) {
				double angle = age * 0.45 + h * 0.6 + k * (Math.PI * 2.0 / points);
				double px = center.x + Math.cos(angle) * r;
				double pz = center.z + Math.sin(angle) * r;
				double py = center.y + h;
				boolean dark = (h + k) % 2 == 0;
				Fx.particles(level, dark ? ParticleTypes.LARGE_SMOKE : ParticleTypes.CLOUD, px, py, pz, 1, 0.08, 0.08, 0.08, 0.02);
			}
		}
		// Debris kicked up at the base.
		Fx.particles(level, ParticleTypes.POOF, center.x, center.y + 0.5, center.z, 4, radius * 0.3, 0.4, radius * 0.3, 0.1);
	}

	private void flingEntities() {
		int height = (int) (radius * 2.4);
		AABB box = new AABB(
			center.x - radius, center.y - 4, center.z - radius,
			center.x + radius, center.y + height, center.z + radius
		);
		List<Entity> entities = level.getEntitiesOfClass(Entity.class, box);
		for (Entity entity : entities) {
			Vec3 p = entity.position();
			double dx = p.x - center.x;
			double dz = p.z - center.z;
			double dist = Math.sqrt(dx * dx + dz * dz);
			if (dist > radius) {
				continue;
			}
			double strength = 1.0 - (dist / radius);
			double inv = dist < 1.0e-3 ? 0.0 : 1.0 / dist;
			double nx = dx * inv;
			double nz = dz * inv;
			double tx = -nz;
			double tz = nx;
			double swirl = (hurricane ? 0.95 : 0.65) * (0.4 + strength);
			double inward = (hurricane ? 0.30 : 0.18) * strength;
			double lift = (hurricane ? 1.1 : 0.8) * (0.3 + strength);
			WorldUtil.setVelocity(entity,
				tx * swirl - nx * inward,
				lift,
				tz * swirl - nz * inward);
		}
	}

	private void ripUpBlocks() {
		RandomSource random = level.getRandom();
		int attempts = hurricane ? 4 : 2;
		for (int i = 0; i < attempts; i++) {
			double a = random.nextDouble() * Math.PI * 2.0;
			double r = random.nextDouble() * radius;
			int x = (int) (center.x + Math.cos(a) * r);
			int z = (int) (center.z + Math.sin(a) * r);
			int y = WorldUtil.surfaceY(level, x, z) - 1;
			BlockPos pos = new BlockPos(x, y, z);
			BlockState state = level.getBlockState(pos);
			if (state.isAir() || state.getDestroySpeed(level, pos) < 0) {
				continue; // skip air and unbreakable (bedrock)
			}
			FallingBlockEntity falling = FallingBlockEntity.fall(level, pos, state);
			double a2 = random.nextDouble() * Math.PI * 2.0;
			WorldUtil.setVelocity(falling,
				Math.cos(a2) * (hurricane ? 0.8 : 0.5),
				0.6 + random.nextDouble() * (hurricane ? 0.9 : 0.6),
				Math.sin(a2) * (hurricane ? 0.8 : 0.5));
		}
	}
}
