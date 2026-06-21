package com.weather.disaster;

import com.weather.protection.ProtectedBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * A meteor falling from the sky toward a fixed impact point. While it descends it
 * leaves a fiery particle trail; on impact it explodes and (for the weak type)
 * scatters volcanic debris as falling blocks with the odd patch of fire.
 */
public class ActiveMeteor {
	private static final int DESCENT_HEIGHT = 110;
	private static final double DESCENT_SPEED = 3.5;
	private static final Block[] DEBRIS = {
		Blocks.BASALT, Blocks.OBSIDIAN, Blocks.STONE, Blocks.COBBLESTONE,
		Blocks.GRAVEL, Blocks.MAGMA_BLOCK, Blocks.BLACKSTONE
	};

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
		int n = type == MeteorType.FIREBALL ? 6 : 3;
		level.sendParticles(ParticleTypes.FLAME, x, y, z, n * 2, 0.7, 0.7, 0.7, 0.02);
		level.sendParticles(ParticleTypes.LAVA, x, y, z, n, 0.5, 0.5, 0.5, 0.0);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, n, 0.9, 0.9, 0.9, 0.01);
	}

	private void impact() {
		RandomSource rng = level.getRandom();

		// An explosion that refuses to break shielded blocks.
		ProtectedBlocks protectedBlocks = ProtectedBlocks.get(level);
		ExplosionDamageCalculator calculator = new ExplosionDamageCalculator() {
			@Override
			public boolean shouldBlockExplode(Explosion explosion, BlockGetter getter, BlockPos pos, BlockState state, float power) {
				return !protectedBlocks.isProtected(pos) && super.shouldBlockExplode(explosion, getter, pos, state, power);
			}
		};
		level.explode(null, level.damageSources().explosion(null, null), calculator,
			x, impactY, z, type.explosionPower, true, Level.ExplosionInteraction.TNT);

		// Rain volcanic debris down as falling blocks.
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

		// Light a few stray fires around the crater.
		for (int i = 0; i < type.fireCount; i++) {
			double a = rng.nextDouble() * Math.PI * 2.0;
			double dist = rng.nextDouble() * type.scatterRadius;
			int fx = Mth.floor(x + Math.cos(a) * dist);
			int fz = Mth.floor(z + Math.sin(a) * dist);
			int sy = level.getHeight(Heightmap.Types.WORLD_SURFACE, fx, fz);
			BlockPos firePos = new BlockPos(fx, sy, fz);
			if (level.getBlockState(firePos).isAir() && !level.getBlockState(firePos.below()).isAir()
					&& !ProtectedBlocks.get(level).isProtected(firePos.below())) {
				level.setBlockAndUpdate(firePos, Blocks.FIRE.defaultBlockState());
			}
		}

		level.playSound(null, x, impactY, z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 6.0f, 0.5f);
	}
}
