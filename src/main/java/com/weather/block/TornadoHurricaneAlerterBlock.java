package com.weather.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.weather.disaster.DisasterType;

import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/**
 * Warns about approaching tornadoes and hurricanes. Crafted from a useless alerter plus
 * a wind charge; marked with a fan symbol to tell it apart from other alerter types.
 */
public class TornadoHurricaneAlerterBlock extends AbstractAlerterBlock {
	public static final MapCodec<TornadoHurricaneAlerterBlock> CODEC =
		RecordCodecBuilder.mapCodec(instance -> instance.group(propertiesCodec()).apply(instance, TornadoHurricaneAlerterBlock::new));

	public TornadoHurricaneAlerterBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
		return CODEC;
	}

	@Override
	public boolean warnsAbout(DisasterType type) {
		return type == DisasterType.TORNADO || type == DisasterType.HURRICANE;
	}
}
