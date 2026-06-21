package com.weather.block;

import com.weather.disaster.DisasterType;

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
 * Shared behaviour for every wall-mounted alerter: a facing, a three-level warning
 * light, an analog comparator output, and observer-friendly state changes. Subclasses
 * only declare which disaster types they warn about (and their own codec).
 *
 * <p>{@link #WARNING}: 0 = green (clear), 1 = yellow (within 500 blocks), 2 = red (within 100).
 */
public abstract class AbstractAlerterBlock extends HorizontalDirectionalBlock {
	public static final IntegerProperty WARNING = IntegerProperty.create("warning", 0, 2);

	public static final int WARNING_NONE = 0;
	public static final int WARNING_YELLOW = 1;
	public static final int WARNING_RED = 2;

	protected AbstractAlerterBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WARNING, WARNING_NONE));
	}

	/** Whether this alerter reacts to the given disaster type. */
	public abstract boolean warnsAbout(DisasterType type);

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
