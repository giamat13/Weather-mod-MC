package com.weather.disaster;

import com.weather.WeatherMod;
import com.weather.protection.ProtectedBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * A meteor falling from the sky toward a fixed impact point. While it descends it
 * leaves a fiery particle trail; on impact its behaviour depends on {@link MeteorType}.
 */
public class ActiveMeteor {
	private static final int DESCENT_HEIGHT = 110;
	private static final double DESCENT_SPEED = 3.5;
	private static final Block[] DEBRIS = {
		Blocks.BASALT, Blocks.OBSIDIAN, Blocks.STONE, Blocks.COBBLESTONE,
		Blocks.GRAVEL, Blocks.MAGMA_BLOCK, Blocks.BLACKSTONE
	};
	/** The junk a weak (LIGHT) meteor showers down as loose items. */
	private static final Item[] LIGHT_LOOT = {
		Items.GUNPOWDER, Items.COAL, Items.IRON_NUGGET, Items.FLINT,
		Items.COPPER_INGOT, Items.RAW_IRON, Items.REDSTONE, Items.CHARCOAL
	};
	/** Radius (blocks) an extinction meteor scours of water and life. */
	private static final int EXTINCTION_RADIUS = 100;

	private final ServerLevel level;
	private final MeteorType type;
	private final double x;
	private final double z;
	private final int impactY;
	private double y;
	private int ticksLived;
	private boolean finished;

	public ActiveMeteor(ServerLevel level, MeteorType type, double x, double impactY, double z) {
		this.level = level;
		this.type = type;
		this.x = x;
		this.z = z;
		this.impactY = Mth.floor(impactY);
		this.y = this.impactY + DESCENT_HEIGHT;
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
		return finished;
	}

	public void tick() {
		ticksLived++;
		y -= DESCENT_SPEED;
		spawnTrail();
		if (ticksLived % 5 == 0) {
			level.playSound(null, x, y, z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 3.0f, 0.4f);
		}
		if (y <= impactY) {
			impact();
			finished = true;
		}
	}

	private void spawnTrail() {
		int n = type == MeteorType.FIREBALL || type == MeteorType.CRATER || type == MeteorType.EXTINCTION ? 6 : 3;
		level.sendParticles(ParticleTypes.FLAME, x, y, z, n * 2, 0.7, 0.7, 0.7, 0.02);
		level.sendParticles(ParticleTypes.LAVA, x, y, z, n, 0.5, 0.5, 0.5, 0.0);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, n, 0.9, 0.9, 0.9, 0.01);
	}

	private void impact() {
		RandomSource rng = level.getRandom();

		// The weak meteor just showers items; nothing else below applies to it.
		if (type == MeteorType.LIGHT) {
			rainItems(rng);
			scatterFire(rng);
			level.playSound(null, x, impactY, z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 3.0f, 1.2f);
			return;
		}

		if (type.explosionPower > 0.0f) {
			explode();
		}

		if (type == MeteorType.CRATER) {
			carveCrater(rng);
		} else if (type == MeteorType.EXTINCTION) {
			extinctionSweep(rng);
		}

		rainDebris(rng);
		scatterFire(rng);

		float volume = type == MeteorType.EXTINCTION ? 10.0f : 6.0f;
		level.playSound(null, x, impactY, z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, volume, 0.5f);
	}

	/** An explosion that refuses to break shielded blocks. */
	private void explode() {
		ProtectedBlocks protectedBlocks = ProtectedBlocks.get(level);
		ExplosionDamageCalculator calculator = new ExplosionDamageCalculator() {
			@Override
			public boolean shouldBlockExplode(Explosion explosion, BlockGetter getter, BlockPos pos, BlockState state, float power) {
				return !protectedBlocks.isProtected(pos) && super.shouldBlockExplode(explosion, getter, pos, state, power);
			}
		};
		level.explode(null, level.damageSources().explosion(null, null), calculator,
			x, impactY, z, type.explosionPower, true, Level.ExplosionInteraction.TNT);
	}

	/** Rain loose, useful items over the area — the weak meteor's whole payload. */
	private void rainItems(RandomSource rng) {
		int drops = 10 + rng.nextInt(10);
		for (int i = 0; i < drops; i++) {
			double a = rng.nextDouble() * Math.PI * 2.0;
			double dist = rng.nextDouble() * type.scatterRadius;
			double ix = x + Math.cos(a) * dist;
			double iz = z + Math.sin(a) * dist;
			Item item = LIGHT_LOOT[rng.nextInt(LIGHT_LOOT.length)];
			ItemStack stack = new ItemStack(item, 1 + rng.nextInt(3));
			ItemEntity drop = new ItemEntity(level, ix, impactY + 12 + rng.nextInt(8), iz, stack);
			drop.setDeltaMovement((rng.nextDouble() - 0.5) * 0.2, -0.1, (rng.nextDouble() - 0.5) * 0.2);
			level.addFreshEntity(drop);
		}
	}

	/** Rain volcanic debris down as falling blocks. */
	private void rainDebris(RandomSource rng) {
		for (int i = 0; i < type.debrisCount; i++) {
			double a = rng.nextDouble() * Math.PI * 2.0;
			double dist = rng.nextDouble() * type.scatterRadius;
			int bx = Mth.floor(x + Math.cos(a) * dist);
			int bz = Mth.floor(z + Math.sin(a) * dist);
			BlockPos spawn = new BlockPos(bx, impactY + 8 + rng.nextInt(6), bz);
			Block debris = DEBRIS[rng.nextInt(DEBRIS.length)];
			FallingBlockEntity block = FallingBlockEntity.fall(level, spawn, debris.defaultBlockState());
			block.setDeltaMovement((rng.nextDouble() - 0.5) * 0.3, -0.2, (rng.nextDouble() - 0.5) * 0.3);
			block.hurtMarked = true;
		}
	}

	/** Light a few stray fires around the crater. */
	private void scatterFire(RandomSource rng) {
		ProtectedBlocks protectedBlocks = ProtectedBlocks.get(level);
		for (int i = 0; i < type.fireCount; i++) {
			double a = rng.nextDouble() * Math.PI * 2.0;
			double dist = rng.nextDouble() * type.scatterRadius;
			int fx = Mth.floor(x + Math.cos(a) * dist);
			int fz = Mth.floor(z + Math.sin(a) * dist);
			int sy = level.getHeight(Heightmap.Types.WORLD_SURFACE, fx, fz);
			BlockPos firePos = new BlockPos(fx, sy, fz);
			if (level.getBlockState(firePos).isAir() && !level.getBlockState(firePos.below()).isAir()
					&& !protectedBlocks.isProtected(firePos.below())) {
				level.setBlockAndUpdate(firePos, Blocks.FIRE.defaultBlockState());
			}
		}
	}

	/** Dig a big bowl-shaped crater into the ground at the impact point. */
	private void carveCrater(RandomSource rng) {
		ProtectedBlocks protectedBlocks = ProtectedBlocks.get(level);
		int radius = 14;
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				double horiz = Math.sqrt(dx * dx + dz * dz);
				if (horiz > radius) {
					continue;
				}
				// A bowl: deeper toward the centre, with a little jitter on the rim.
				int depth = (int) ((radius - horiz) * 0.9) + rng.nextInt(2);
				int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, Mth.floor(x) + dx, Mth.floor(z) + dz);
				for (int dy = 0; dy <= depth; dy++) {
					cursor.set(Mth.floor(x) + dx, surface - dy, Mth.floor(z) + dz);
					if (!protectedBlocks.isProtected(cursor) && !level.getBlockState(cursor).isAir()) {
						level.setBlockAndUpdate(cursor.immutable(), Blocks.AIR.defaultBlockState());
					}
				}
			}
		}
	}

	/**
	 * The extinction event: boil away all water in {@link #EXTINCTION_RADIUS} blocks
	 * (recording it so the rain can seep it back), scorch random blocks to lava, and
	 * kill every entity caught in the radius.
	 */
	private void extinctionSweep(RandomSource rng) {
		ProtectedBlocks protectedBlocks = ProtectedBlocks.get(level);
		List<BlockPos> dried = new ArrayList<>();
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		int r = EXTINCTION_RADIUS;
		int r2 = r * r;
		int minY = level.getMinY();
		int maxY = level.getMaxY();

		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				if (dx * dx + dz * dz > r2) {
					continue;
				}
				int wx = Mth.floor(x) + dx;
				int wz = Mth.floor(z) + dz;
				for (int wy = minY; wy < maxY; wy++) {
					cursor.set(wx, wy, wz);
					BlockState state = level.getBlockState(cursor);
					if (protectedBlocks.isProtected(cursor)) {
						continue;
					}
					if (state.getBlock() == Blocks.WATER) {
						level.setBlockAndUpdate(cursor.immutable(), Blocks.AIR.defaultBlockState());
						dried.add(cursor.immutable());
					} else if (state.hasProperty(BlockStateProperties.WATERLOGGED)
							&& state.getValue(BlockStateProperties.WATERLOGGED)) {
						level.setBlockAndUpdate(cursor.immutable(),
							state.setValue(BlockStateProperties.WATERLOGGED, false));
					}
				}
			}
		}

		// A ring of secondary blasts spread across the whole radius, so the whole
		// area is devastated rather than just the impact point.
		int secondaryBlasts = 12;
		for (int i = 0; i < secondaryBlasts; i++) {
			double a = rng.nextDouble() * Math.PI * 2.0;
			double dist = rng.nextDouble() * r;
			int bx = Mth.floor(x + Math.cos(a) * dist);
			int bz = Mth.floor(z + Math.sin(a) * dist);
			int by = level.getHeight(Heightmap.Types.WORLD_SURFACE, bx, bz);
			level.explode(null, bx + 0.5, by, bz + 0.5, 10.0f, true, Level.ExplosionInteraction.TNT);
		}

		// Scorch a massive scattering of random blocks into lava — surface and
		// several blocks down, so the ground is riddled with molten pockets.
		int lavaSpots = 1200;
		for (int i = 0; i < lavaSpots; i++) {
			double a = rng.nextDouble() * Math.PI * 2.0;
			double dist = rng.nextDouble() * r;
			int lx = Mth.floor(x + Math.cos(a) * dist);
			int lz = Mth.floor(z + Math.sin(a) * dist);
			int ly = level.getHeight(Heightmap.Types.WORLD_SURFACE, lx, lz) - 1 - rng.nextInt(6);
			cursor.set(lx, ly, lz);
			if (!protectedBlocks.isProtected(cursor) && !level.getBlockState(cursor).isAir()) {
				level.setBlockAndUpdate(cursor.immutable(), Blocks.LAVA.defaultBlockState());
			}
		}

		// Kill every entity within the radius.
		AABB box = new AABB(x - r, minY, z - r, x + r, maxY, z + r);
		for (var entity : level.getEntities((net.minecraft.world.entity.Entity) null, box,
				e -> e.distanceToSqr(x, e.getY(), z) <= r2)) {
			entity.kill(level);
		}

		if (!dried.isEmpty()) {
			WeatherMod.DISASTERS.reportDriedWater(level, dried);
		}
	}
}
