package com.weather.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import com.weather.WeatherMod;
import com.weather.disaster.DisasterManager;
import com.weather.disaster.DisasterType;
import com.weather.disaster.MeteorType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * {@code /naturaldisasters <tornado|hurricane> [delaySeconds]}
 *
 * <p>Triggers a disaster at the caller's position. With no delay it strikes almost
 * immediately; with a delay it is scheduled ahead so a built Alerter can warn first.
 */
public final class NaturalDisastersCommand {
	private NaturalDisastersCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("naturaldisasters")
			.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));

		for (DisasterType type : DisasterType.values()) {
			if (!type.spawnable) {
				continue; // e.g. wildfires are detected, not spawned
			}
			LiteralArgumentBuilder<CommandSourceStack> branch = Commands.literal(type.name().toLowerCase())
				.executes(ctx -> trigger(ctx, type, 0, null));

			if (type == DisasterType.METEOR) {
				// /naturaldisasters meteor <delaySeconds> [type 1=light 2=crater 3=extinction]
				branch.then(Commands.argument("delaySeconds", IntegerArgumentType.integer(0, 600))
					.executes(ctx -> trigger(ctx, type, IntegerArgumentType.getInteger(ctx, "delaySeconds"), null))
					.then(Commands.argument("type", IntegerArgumentType.integer(1, 3))
						.executes(ctx -> trigger(ctx, type,
							IntegerArgumentType.getInteger(ctx, "delaySeconds"),
							MeteorType.byCommandId(IntegerArgumentType.getInteger(ctx, "type"))))));
			} else {
				branch.then(Commands.argument("delaySeconds", IntegerArgumentType.integer(0, 600))
					.executes(ctx -> trigger(ctx, type, IntegerArgumentType.getInteger(ctx, "delaySeconds"), null)));
			}

			root.then(branch);
		}

		dispatcher.register(root);
	}

	private static int trigger(CommandContext<CommandSourceStack> ctx, DisasterType type, int delaySeconds,
			MeteorType meteorType) {
		CommandSourceStack source = ctx.getSource();
		ServerLevel level = source.getLevel();
		Vec3 pos = source.getPosition();

		DisasterManager manager = WeatherMod.DISASTERS;
		manager.schedule(type, level, pos.x, pos.y, pos.z, delaySeconds * 20, meteorType);

		String when = delaySeconds == 0 ? "עכשיו" : "בעוד " + delaySeconds + " שניות";
		String label = meteorType != null ? type.hebrewName + " (" + meteorHebrew(meteorType) + ")" : type.hebrewName;
		source.sendSuccess(() -> Component.literal("§e" + label + " זומן " + when + "!"), true);
		return 1;
	}

	private static String meteorHebrew(MeteorType type) {
		return switch (type) {
			case LIGHT -> "חלש";
			case CRATER -> "מכתש";
			case EXTINCTION -> "מכחיד";
			default -> type.name().toLowerCase();
		};
	}
}
