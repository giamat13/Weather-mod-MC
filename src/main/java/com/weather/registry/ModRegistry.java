package com.weather.registry;

import java.util.function.Function;

import com.weather.WeatherMod;
import com.weather.block.AbstractAlerterBlock;
import com.weather.block.EarthquakeAlerterBlock;
import com.weather.block.FireAlerterBlock;
import com.weather.block.MeteorAlerterBlock;
import com.weather.block.TornadoHurricaneAlerterBlock;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Registers the mod's blocks, items and its creative-menu tab.
 */
public final class ModRegistry {
	/** Crafting intermediate: does nothing on its own. */
	public static final Item USELESS_ALERTER = registerItem("useless_alerter",
		key -> new Item(new Item.Properties().setId(key)));

	/** Right-click a block with this to shield it from natural disasters. */
	public static final Item DISASTER_SHIELD = registerItem("disaster_shield",
		key -> new Item(new Item.Properties().setId(key)));

	/** Warns about approaching tornadoes and hurricanes; placed on walls, glows while warning. */
	public static final Block TORNADO_HURRICANE_ALERTER = registerBlock("tornado_hurricane_alerter",
		key -> new TornadoHurricaneAlerterBlock(alerterProperties(key)));

	/** Warns about approaching earthquakes. */
	public static final Block EARTHQUAKE_ALERTER = registerBlock("earthquake_alerter",
		key -> new EarthquakeAlerterBlock(alerterProperties(key)));

	/** Warns about wildfires in nearby forest biomes. */
	public static final Block FIRE_ALERTER = registerBlock("fire_alerter",
		key -> new FireAlerterBlock(alerterProperties(key)));

	/** Warns about incoming meteors. */
	public static final Block METEOR_ALERTER = registerBlock("meteor_alerter",
		key -> new MeteorAlerterBlock(alerterProperties(key)));

	private static BlockBehaviour.Properties alerterProperties(ResourceKey<Block> key) {
		return BlockBehaviour.Properties.of()
			.setId(key)
			.strength(1.5f, 6.0f)
			.sound(SoundType.METAL)
			.lightLevel(state -> state.getValue(AbstractAlerterBlock.WARNING) > 0 ? 13 : 4);
	}

	private ModRegistry() {
	}

	/** Touch this class so its static fields initialise and everything registers. */
	public static void init() {
		WeatherMod.LOGGER.info("Registered blocks and items");
	}

	private static Item registerItem(String name, Function<ResourceKey<Item>, Item> factory) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, WeatherMod.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(key));
	}

	private static Block registerBlock(String name, Function<ResourceKey<Block>, Block> factory) {
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, WeatherMod.id(name));
		Block block = Registry.register(BuiltInRegistries.BLOCK, blockKey, factory.apply(blockKey));

		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, WeatherMod.id(name));
		Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey)));
		return block;
	}
}
