package com.weather;

import com.weather.command.NaturalDisastersCommand;
import com.weather.command.SeasonCommand;
import com.weather.disaster.DisasterManager;
import com.weather.registry.ModRegistry;
import com.weather.season.SeasonManager;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WeatherMod implements ModInitializer {
	public static final String MOD_ID = "weather-mod";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	// The single server-side manager that drives all natural disasters and alerters.
	public static final DisasterManager DISASTERS = new DisasterManager();
	// Tracks the seasons and biases the weather.
	public static final SeasonManager SEASONS = new SeasonManager();

	@Override
	public void onInitialize() {
		ModRegistry.init();

		// Drive the whole system from the end of every server tick.
		ServerTickEvents.END_SERVER_TICK.register(DISASTERS::onEndServerTick);
		ServerTickEvents.END_SERVER_TICK.register(SEASONS::onEndServerTick);

		// Commands: trigger disasters, and view/change the season.
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			NaturalDisastersCommand.register(dispatcher);
			SeasonCommand.register(dispatcher);
		});

		LOGGER.info("Natural Disasters loaded 🌪");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
