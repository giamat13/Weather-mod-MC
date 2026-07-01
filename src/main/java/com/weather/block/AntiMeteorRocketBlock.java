package com.weather.block;

import com.mojang.serialization.MapCodec;

import com.weather.WeatherMod;
import com.weather.protection.ProtectedBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;

/**
 * The anti-meteor rocket. Placed like a normal block and armed with redstone: the
 * moment it receives a signal it fires — a small blast, a shower of blocks flung
 * skyward, and (its whole purpose) the cancellation of the nearest incoming meteor
 * of any kind. Firing consumes the rocket.
 */
public class AntiMeteorRocketBlock extends Block {
	public static final MapCodec<AntiMeteorRocketBlock> CODEC = simpleCodec(AntiMeteorRocketBlock::new);

	/** Small launch blast. */
	private static final float BLAST_POWER = 2.0f;
	/** Radius of blocks flung up when the rocket fires. */
	private static final int FLING_RADIUS = 4;

	public AntiMeteorRocketBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor,
			Orientation orientation, boolean movedByPiston) {
		if (level instanceof ServerLevel serverLevel && level.hasNeighborSignal(pos)) {
			fire(serverLevel, pos);
		}
	}

	private void fire(ServerLevel level, BlockPos pos) {
		double x = pos.getX() + 0.5;
		double y = pos.getY() + 0.5;
		double z = pos.getZ() + 0.5;

		// Consume the rocket before the blast so it isn't caught in its own explosion logic.
		level.removeBlock(pos, false);

		boolean cancelled = WeatherMod.DISASTERS.cancelNearestMeteor(level, pos);

		flingBlocksUp(level, pos);

		level.explode(null, x, y, z, BLAST_POWER, Level.ExplosionInteraction.TNT);
		level.playSound(null, x, y, z, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.BLOCKS, 2.0f, 0.8f);

		String message = cancelled
			? "§a🚀 טיל אנטי-מטאור שוגר — המטאור הקרוב בוטל!"
			: "§e🚀 טיל אנטי-מטאור שוגר, אך לא היה מטאור קרוב לבטל.";
		for (var player : level.players()) {
			player.sendSystemMessage(net.minecraft.network.chat.Component.literal(message));
		}
	}

	/** Rip loose blocks around the launch site up into the sky. */
	private void flingBlocksUp(ServerLevel level, BlockPos centre) {
		RandomSource rng = level.getRandom();
		ProtectedBlocks protectedBlocks = ProtectedBlocks.get(level);
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int dx = -FLING_RADIUS; dx <= FLING_RADIUS; dx++) {
			for (int dz = -FLING_RADIUS; dz <= FLING_RADIUS; dz++) {
				for (int dy = 0; dy <= 2; dy++) {
					cursor.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
					if (dx * dx + dz * dz > FLING_RADIUS * FLING_RADIUS) {
						continue;
					}
					BlockState state = level.getBlockState(cursor);
					if (state.isAir() || protectedBlocks.isProtected(cursor)
							|| state.getDestroySpeed(level, cursor) < 0.0f) {
						continue;
					}
					if (rng.nextFloat() > 0.5f) {
						continue;
					}
					BlockPos immutable = cursor.immutable();
					FallingBlockEntity block = FallingBlockEntity.fall(level, immutable, state);
					block.setDeltaMovement((rng.nextDouble() - 0.5) * 0.4,
						0.6 + rng.nextDouble() * 0.5, (rng.nextDouble() - 0.5) * 0.4);
					block.hurtMarked = true;
				}
			}
		}
		// Make sure the rocket's own footprint is clear.
		level.setBlockAndUpdate(centre, Blocks.AIR.defaultBlockState());
	}
}
