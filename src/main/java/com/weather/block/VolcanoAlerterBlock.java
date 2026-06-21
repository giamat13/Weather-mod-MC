package com.weather.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.weather.disaster.DisasterType;

import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/**
 * Warns about volcanic eruptions (earthquake-triggered geyser fields). Crafted from a
 * useless alerter plus stone and a fire charge; marked with a geyser-plume symbol.
 */
public class VolcanoAlerterBlock extends AbstractAlerterBlock {
	public static final MapCodec<VolcanoAlerterBlock> CODEC =
		RecordCodecBuilder.mapCodec(instance -> instance.group(propertiesCodec()).apply(instance, VolcanoAlerterBlock::new));

	public VolcanoAlerterBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
		return CODEC;
	}

	@Override
	public boolean warnsAbout(DisasterType type) {
		return type == DisasterType.VOLCANO;
	}
}
