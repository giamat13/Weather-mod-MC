package com.weather_by_giamatamat;

import com.weather_by_giamatamat.alerter.AlerterManager;
import com.weather_by_giamatamat.command.NaturalDisastersCommand;
import com.weather_by_giamatamat.command.SeasonCommand;
import com.weather_by_giamatamat.core.DisasterManager;
import com.weather_by_giamatamat.util.ModCompat;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Natural Disasters by giamatamat — server-side entrypoint. Wires the disaster orchestrator,
 * the Alerter interaction handler and the commands to Fabric events.
 */
public class WeatherByGiamatamat implements ModInitializer {
	public static final String MOD_ID = "weather-by-giamatamat";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModCompat.init();

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			NaturalDisastersCommand.register(dispatcher);
			SeasonCommand.register(dispatcher);
		});

		ServerLifecycleEvents.SERVER_STARTED.register(DisasterManager::onServerStarted);
		ServerLifecycleEvents.SERVER_STOPPING.register(DisasterManager::onServerStopping);

		ServerTickEvents.END_SERVER_TICK.register(DisasterManager::onServerTick);
		ServerTickEvents.END_LEVEL_TICK.register(DisasterManager::onLevelTick);

		UseBlockCallback.EVENT.register(AlerterManager::onUseBlock);

		LOGGER.info("Natural Disasters by giamatamat initialised (build the Alerter and run /Naturaldisasters).");
	}

	public static Identifier id(final String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
