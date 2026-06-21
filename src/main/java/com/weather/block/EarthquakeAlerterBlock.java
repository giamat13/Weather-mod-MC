package com.weather.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.weather.disaster.DisasterType;

import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/**
 * Warns about approaching earthquakes. Crafted from a useless alerter plus dirt;
 * marked with a crack symbol to tell it apart from other alerter types.
 */
public class EarthquakeAlerterBlock extends AbstractAlerterBlock {
	public static final MapCodec<EarthquakeAlerterBlock> CODEC =
		RecordCodecBuilder.mapCodec(instance -> instance.group(propertiesCodec()).apply(instance, EarthquakeAlerterBlock::new));

	public EarthquakeAlerterBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
		return CODEC;
	}

	@Override
	public boolean warnsAbout(DisasterType type) {
		return type == DisasterType.EARTHQUAKE;
	}
}
