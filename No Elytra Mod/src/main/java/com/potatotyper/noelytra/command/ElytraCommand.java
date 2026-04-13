package com.potatotyper.noelytra.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.potatotyper.noelytra.config.ConfigManager;
import com.potatotyper.noelytra.enforcement.ElytraEnforcer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.Permissions;

import java.util.Set;

public final class ElytraCommand {
	private ElytraCommand() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
			Commands.literal("elytra")
				.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
				.then(Commands.literal("allow")
					.then(Commands.argument("dimension", StringArgumentType.word())
						.executes(ElytraCommand::allowDimension)))
				.then(Commands.literal("deny")
					.then(Commands.argument("dimension", StringArgumentType.word())
						.executes(ElytraCommand::denyDimension)))
				.then(Commands.literal("list")
					.executes(ElytraCommand::listDimensions))
		));
	}

	private static int allowDimension(CommandContext<CommandSourceStack> context) {
		String dimension = parseAndNormalizeDimension(context);
		if (dimension == null) {
			return 0;
		}

		boolean changed = ConfigManager.getConfig().allowDimension(dimension);
		ConfigManager.save();
		ElytraEnforcer.revalidateAllPlayers(context.getSource().getServer());

		if (changed) {
			context.getSource().sendSuccess(() -> Component.literal("Allowed Elytra in: " + dimension), true);
		} else {
			context.getSource().sendSuccess(() -> Component.literal("Dimension already allowed: " + dimension), false);
		}
		return 1;
	}

	private static int denyDimension(CommandContext<CommandSourceStack> context) {
		String dimension = parseAndNormalizeDimension(context);
		if (dimension == null) {
			return 0;
		}

		boolean changed = ConfigManager.getConfig().denyDimension(dimension);
		ConfigManager.save();
		ElytraEnforcer.revalidateAllPlayers(context.getSource().getServer());

		if (changed) {
			context.getSource().sendSuccess(() -> Component.literal("Denied Elytra in: " + dimension), true);
		} else {
			context.getSource().sendSuccess(() -> Component.literal("Dimension already denied: " + dimension), false);
		}
		return 1;
	}

	private static int listDimensions(CommandContext<CommandSourceStack> context) {
		Set<String> allowed = ConfigManager.getConfig().getAllowedDimensions();
		if (allowed.isEmpty()) {
			context.getSource().sendSuccess(() -> Component.literal("No dimensions are currently allowed for Elytra."), false);
			return 1;
		}

		String joined = String.join(", ", allowed);
		context.getSource().sendSuccess(() -> Component.literal("Elytra allowed dimensions: " + joined), false);
		return 1;
	}

	private static String parseAndNormalizeDimension(CommandContext<CommandSourceStack> context) {
		String raw = StringArgumentType.getString(context, "dimension");
		String normalized = raw.contains(":") ? raw : "minecraft:" + raw;

		Identifier identifier = Identifier.tryParse(normalized);
		if (identifier == null) {
			context.getSource().sendFailure(Component.literal("Invalid dimension identifier: " + raw));
			return null;
		}

		return identifier.toString();
	}
}