package com.weather_by_giamatamat.command;

import java.util.Locale;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;

import com.weather_by_giamatamat.core.DisasterGlobalData;
import com.weather_by_giamatamat.core.DisasterManager;
import com.weather_by_giamatamat.core.DisasterType;
import com.weather_by_giamatamat.core.ModConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * {@code /Naturaldisasters} — master switch and admin trigger for the disaster system.
 * <ul>
 *   <li>{@code /Naturaldisasters} or {@code status} – show state (anyone)</li>
 *   <li>{@code /Naturaldisasters on|off} – toggle the system (ops)</li>
 *   <li>{@code /Naturaldisasters trigger <type> [magnitude]} – spawn a disaster at your position (ops)</li>
 * </ul>
 */
public final class NaturalDisastersCommand {
	private NaturalDisastersCommand() {
	}

	public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
		LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("Naturaldisasters")
			.executes(NaturalDisastersCommand::status);
		root.then(Commands.literal("status").executes(NaturalDisastersCommand::status));
		root.then(Commands.literal("on")
			.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
			.executes(ctx -> setEnabled(ctx, true)));
		root.then(Commands.literal("off")
			.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
			.executes(ctx -> setEnabled(ctx, false)));

		LiteralArgumentBuilder<CommandSourceStack> trigger = Commands.literal("trigger")
			.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
		for (DisasterType type : DisasterType.values()) {
			if (type == DisasterType.EARTHQUAKE) {
				trigger.then(Commands.literal("earthquake")
					.executes(ctx -> triggerAt(ctx, DisasterType.EARTHQUAKE, 5))
					.then(Commands.argument("magnitude", IntegerArgumentType.integer(1, 10))
						.executes(ctx -> triggerAt(ctx, DisasterType.EARTHQUAKE, IntegerArgumentType.getInteger(ctx, "magnitude")))));
			} else {
				trigger.then(Commands.literal(type.name().toLowerCase(Locale.ROOT))
					.executes(ctx -> triggerAt(ctx, type, 5)));
			}
		}
		root.then(trigger);

		LiteralCommandNode<CommandSourceStack> node = dispatcher.register(root);
		dispatcher.register(Commands.literal("naturaldisasters").redirect(node));
	}

	private static int status(final CommandContext<CommandSourceStack> ctx) {
		MinecraftServer server = ctx.getSource().getServer();
		DisasterGlobalData data = DisasterGlobalData.get(server);
		int activeCount = DisasterManager.active().size();
		ctx.getSource().sendSuccess(() -> Component.literal("Natural Disasters: ")
			.append(Component.literal(data.isEnabled() ? "ON" : "OFF")
				.withStyle(data.isEnabled() ? ChatFormatting.GREEN : ChatFormatting.RED))
			.append(Component.literal(
				" | Season: " + data.getSeason().displayName()
					+ " (day " + (data.getSeasonDay() + 1) + "/" + ModConfig.SEASON_LENGTH_DAYS + ")"
					+ " | Active events: " + activeCount)), false);
		return 1;
	}

	private static int setEnabled(final CommandContext<CommandSourceStack> ctx, final boolean enabled) {
		DisasterGlobalData data = DisasterGlobalData.get(ctx.getSource().getServer());
		data.setEnabled(enabled);
		ctx.getSource().sendSuccess(
			() -> Component.literal("Natural Disasters " + (enabled ? "enabled" : "disabled") + ".")
				.withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED),
			true);
		return 1;
	}

	private static int triggerAt(final CommandContext<CommandSourceStack> ctx, final DisasterType type, final int magnitude) {
		CommandSourceStack source = ctx.getSource();
		ServerLevel level = source.getLevel();
		Vec3 pos = source.getPosition();
		DisasterManager.trigger(level, type, pos, magnitude);
		source.sendSuccess(() -> Component.literal("Triggered " + type.displayName() + " " + type.emoji()
			+ (type == DisasterType.EARTHQUAKE ? " (magnitude " + magnitude + ")" : "")), true);
		return 1;
	}
}
