package com.weather_by_giamatamat.command;

import java.util.Locale;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;

import com.weather_by_giamatamat.core.DisasterGlobalData;
import com.weather_by_giamatamat.core.ModConfig;
import com.weather_by_giamatamat.core.Season;
import com.weather_by_giamatamat.core.SeasonManager;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

/**
 * {@code /Season} — inspect or change the current season.
 * <ul>
 *   <li>{@code /Season} – show the current season and day (anyone)</li>
 *   <li>{@code /Season set <spring|summer|autumn|winter>} – set it (ops)</li>
 *   <li>{@code /Season next} – advance to the next season (ops)</li>
 * </ul>
 */
public final class SeasonCommand {
	private SeasonCommand() {
	}

	public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
		LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("Season")
			.executes(SeasonCommand::show);

		LiteralArgumentBuilder<CommandSourceStack> set = Commands.literal("set")
			.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
		for (Season season : Season.values()) {
			set.then(Commands.literal(season.name().toLowerCase(Locale.ROOT))
				.executes(ctx -> apply(ctx, season)));
		}
		root.then(set);
		root.then(Commands.literal("next")
			.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
			.executes(SeasonCommand::next));

		LiteralCommandNode<CommandSourceStack> node = dispatcher.register(root);
		dispatcher.register(Commands.literal("season").redirect(node));
	}

	private static int show(final CommandContext<CommandSourceStack> ctx) {
		DisasterGlobalData data = DisasterGlobalData.get(ctx.getSource().getServer());
		ctx.getSource().sendSuccess(() -> Component.literal("Current season: ")
			.append(Component.literal(data.getSeason().displayName()).withStyle(ChatFormatting.GOLD))
			.append(Component.literal(" (day " + (data.getSeasonDay() + 1) + "/" + ModConfig.SEASON_LENGTH_DAYS + ")")),
			false);
		return 1;
	}

	private static int apply(final CommandContext<CommandSourceStack> ctx, final Season season) {
		MinecraftServer server = ctx.getSource().getServer();
		SeasonManager.setSeason(server, DisasterGlobalData.get(server), season);
		return 1;
	}

	private static int next(final CommandContext<CommandSourceStack> ctx) {
		MinecraftServer server = ctx.getSource().getServer();
		DisasterGlobalData data = DisasterGlobalData.get(server);
		SeasonManager.setSeason(server, data, data.getSeason().next());
		return 1;
	}
}
