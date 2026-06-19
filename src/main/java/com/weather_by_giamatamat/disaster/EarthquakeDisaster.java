package com.weather_by_giamatamat.disaster;

import java.util.List;

import com.weather_by_giamatamat.core.DisasterManager;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * An earthquake of a given magnitude (2-9). It violently shakes nearby entities (launching them
 * with height/distance scaled by magnitude), cracks open the ground in surrounding chunks, and may
 * spawn geyser fields (handled by {@link DisasterManager} at impact).
 */
public class EarthquakeDisaster extends Disaster {
	private final int magnitude;
	private final double radius;
	private int crackBudgetPerTick;

	public EarthquakeDisaster(final ServerLevel level, final Vec3 center, final int magnitude) {
		super(level, DisasterType.EARTHQUAKE, center);
		this.magnitude = Math.max(1, magnitude);
		this.radius = ModConfig.EARTHQUAKE_BASE_RADIUS * (0.5 + this.magnitude / 6.0);
	}

	public int magnitude() {
		return this.magnitude;
	}

	@Override
	public double radius() {
		return this.radius;
	}

	@Override
	protected int warnTicks() {
		return ModConfig.EARTHQUAKE_WARN_TICKS;
	}

	@Override
	protected int activeTicks() {
		return ModConfig.EARTHQUAKE_ACTIVE_TICKS;
	}

	@Override
	protected void onImpact() {
		Fx.sound(level, center, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4.0F, 0.2F);
		// Block-cracks per tick scale with magnitude (≈ magnitude% of the surface fissures) but are
		// hard-capped so even a magnitude-9 quake stays performant and doesn't level the world.
		this.crackBudgetPerTick = Math.min(ModConfig.EARTHQUAKE_MAX_CRACKS_PER_TICK,
			Math.max(2, (int) Math.round(magnitude * ModConfig.EARTHQUAKE_BREAK_PER_MAGNITUDE * 1000.0)));
		// Geyser fields / volcanic eruptions are rolled once, at impact.
		DisasterManager.onEarthquakeImpact(level, centerPos(), magnitude);
	}

	@Override
	protected void onActiveTick() {
		if (age % 6 == 0) {
			shakeEntities();
		}
		crackGround();
		if (age % 8 == 0) {
			Fx.sound(level, center, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1.6F, 0.25F);
		}
		Fx.particles(level, ParticleTypes.CRIT, center.x, center.y + 0.2, center.z, 8, radius * 0.5, 0.3, radius * 0.5, 0.0);
	}

	private void shakeEntities() {
		AABB box = new AABB(center.x - radius, center.y - 8, center.z - radius,
			center.x + radius, center.y + 8, center.z + radius);
		List<Entity> entities = level.getEntitiesOfClass(Entity.class, box);
		RandomSource random = level.getRandom();
		double lift = 0.25 + 0.07 * magnitude;
		double horiz = 0.05 * magnitude;
		for (Entity entity : entities) {
			if (!entity.onGround()) {
				continue; // only bounce things that are standing on the ground
			}
			WorldUtil.impulse(entity,
				(random.nextDouble() - 0.5) * 2.0 * horiz,
				lift,
				(random.nextDouble() - 0.5) * 2.0 * horiz);
		}
	}

	private void crackGround() {
		RandomSource random = level.getRandom();
		for (int i = 0; i < crackBudgetPerTick; i++) {
			double a = random.nextDouble() * Math.PI * 2.0;
			double r = random.nextDouble() * radius;
			int x = (int) (center.x + Math.cos(a) * r);
			int z = (int) (center.z + Math.sin(a) * r);
			int surface = WorldUtil.surfaceY(level, x, z);
			int depth = 1 + random.nextInt(6);
			int y = surface - random.nextInt(3);
			BlockPos pos = new BlockPos(x, y, z);
			for (int d = 0; d < depth; d++) {
				BlockState state = level.getBlockState(pos);
				if (!state.isAir() && state.getDestroySpeed(level, pos) >= 0) {
					level.destroyBlock(pos, false);
				}
				pos = pos.below();
			}
		}
	}
}
