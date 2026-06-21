package com.weather.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.weather.disaster.DisasterType;

import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/**
 * Warns about incoming meteors. Crafted from a useless alerter plus stone;
 * marked with a falling-meteor symbol.
 */
public class MeteorAlerterBlock extends AbstractAlerterBlock {
	public static final MapCodec<MeteorAlerterBlock> CODEC =
		RecordCodecBuilder.mapCodec(instance -> instance.group(propertiesCodec()).apply(instance, MeteorAlerterBlock::new));

	public MeteorAlerterBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
		return CODEC;
	}

	@Override
	public boolean warnsAbout(DisasterType type) {
		return type == DisasterType.METEOR;
	}
}
