package com.weather.protection;

import java.util.ArrayList;

import com.mojang.serialization.Codec;
import com.weather.WeatherMod;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Per-level persistent set of block positions that are shielded from natural
 * disasters. Stored as packed {@link BlockPos#asLong() longs} and saved with the
 * world, so protection survives restarts.
 */
public class ProtectedBlocks extends SavedData {
	public static final Codec<ProtectedBlocks> CODEC = Codec.LONG.listOf().xmap(
		list -> new ProtectedBlocks(new LongOpenHashSet(list)),
		blocks -> new ArrayList<>(blocks.positions));

	public static final SavedDataType<ProtectedBlocks> TYPE = new SavedDataType<>(
		WeatherMod.id("protected_blocks"), ProtectedBlocks::new, CODEC, DataFixTypes.LEVEL);

	private final LongSet positions;

	public ProtectedBlocks() {
		this(new LongOpenHashSet());
	}

	public ProtectedBlocks(LongSet positions) {
		this.positions = positions;
	}

	public static ProtectedBlocks get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(TYPE);
	}

	public boolean isProtected(BlockPos pos) {
		return positions.contains(pos.asLong());
	}

	/** Mark a position as protected. Returns false if it already was. */
	public boolean protect(BlockPos pos) {
		if (positions.add(pos.asLong())) {
			setDirty();
			return true;
		}
		return false;
	}

	/** Remove protection from a position. Returns false if it wasn't protected. */
	public boolean unprotect(BlockPos pos) {
		if (positions.remove(pos.asLong())) {
			setDirty();
			return true;
		}
		return false;
	}
}
