package com.weather.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.weather.disaster.DisasterType;

import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/**
 * Warns about wildfires — fires burning in forest biomes nearby. Crafted from a
 * useless alerter plus a fire charge; marked with a flame symbol.
 */
public class FireAlerterBlock extends AbstractAlerterBlock {
	public static final MapCodec<FireAlerterBlock> CODEC =
		RecordCodecBuilder.mapCodec(instance -> instance.group(propertiesCodec()).apply(instance, FireAlerterBlock::new));

	public FireAlerterBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
		return CODEC;
	}

	@Override
	public boolean warnsAbout(DisasterType type) {
		return type == DisasterType.WILDFIRE;
	}
}
