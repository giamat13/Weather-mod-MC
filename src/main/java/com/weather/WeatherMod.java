package com.weather;

import com.weather.command.NaturalDisastersCommand;
import com.weather.disaster.DisasterManager;
import com.weather.registry.ModRegistry;

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

	@Override
	public void onInitialize() {
		ModRegistry.init();

		// Drive the whole system from the end of every server tick.
		ServerTickEvents.END_SERVER_TICK.register(DISASTERS::onEndServerTick);

		// Make sure a fresh world doesn't inherit leftover state from a previous one.
		ServerTickEvents.START_SERVER_TICK.register(server -> {});

		// /naturaldisasters command for manually triggering a disaster.
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			NaturalDisastersCommand.register(dispatcher));

		LOGGER.info("Natural Disasters loaded 🌪");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
