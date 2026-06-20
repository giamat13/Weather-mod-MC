package com.weather.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * The Tornado &amp; Hurricane Alerter — a wall-mountable warning device crafted from a
 * useless alerter plus a wind charge. It faces the player when placed and lights up to
 * warn about an approaching tornado or hurricane. (More disaster-specific alerters may
 * follow later.)
 *
 * <p>{@link #WARNING} drives the colour shown on its front face:
 * 0 = green (clear), 1 = yellow (within 500 blocks), 2 = red (within 100 blocks).
 * The actual threat detection lives in the disaster system, which sets this value.
 */
public class TornadoHurricaneAlerterBlock extends HorizontalDirectionalBlock {
	public static final MapCodec<TornadoHurricaneAlerterBlock> CODEC =
		RecordCodecBuilder.mapCodec(instance -> instance.group(propertiesCodec()).apply(instance, TornadoHurricaneAlerterBlock::new));

	public static final IntegerProperty WARNING = IntegerProperty.create("warning", 0, 2);

	public static final int WARNING_NONE = 0;
	public static final int WARNING_YELLOW = 1;
	public static final int WARNING_RED = 2;

	public TornadoHurricaneAlerterBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WARNING, WARNING_NONE));
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, WARNING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		// Face the player, so the lit front sits flat against the wall behind it.
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	// --- redstone: a comparator next to the alerter reads the warning level ---

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		return switch (state.getValue(WARNING)) {
			case WARNING_RED -> 15;
			case WARNING_YELLOW -> 7;
			default -> 0;
		};
	}
}
