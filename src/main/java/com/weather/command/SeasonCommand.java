package com.weather.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import com.weather.WeatherMod;
import com.weather.season.Season;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

/**
 * {@code /season} shows the current season; {@code /season <name>} changes it
 * (operators only).
 */
public final class SeasonCommand {
	private SeasonCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("season")
			.executes(SeasonCommand::query);

		for (Season season : Season.values()) {
			root.then(Commands.literal(season.name().toLowerCase())
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.executes(ctx -> set(ctx, season)));
		}

		dispatcher.register(root);
	}

	private static int query(CommandContext<CommandSourceStack> ctx) {
		MinecraftServer server = ctx.getSource().getServer();
		Season season = WeatherMod.SEASONS.current(server);
		long day = WeatherMod.SEASONS.daysIntoSeason(server) + 1;
		ctx.getSource().sendSuccess(() -> Component.literal(
			"§6" + season.icon + " עונה נוכחית: " + season.hebrewName + " (יום " + day + " מתוך 30)"), false);
		return 1;
	}

	private static int set(CommandContext<CommandSourceStack> ctx, Season season) {
		MinecraftServer server = ctx.getSource().getServer();
		WeatherMod.SEASONS.setSeason(server, season);
		ctx.getSource().sendSuccess(() -> Component.literal("§6העונה שונתה ל" + season.hebrewName), true);
		return 1;
	}
}
