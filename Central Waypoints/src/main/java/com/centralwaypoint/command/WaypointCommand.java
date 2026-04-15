package com.centralwaypoint.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.centralwaypoint.waypoint.Waypoint;
import com.centralwaypoint.waypoint.WaypointCompassService;
import com.centralwaypoint.waypoint.WaypointManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public final class WaypointCommand {
	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ISO_INSTANT;
	private static final SuggestionProvider<CommandSourceStack> WAYPOINT_SUGGESTIONS = (context, builder) ->
		SharedSuggestionProvider.suggest(WaypointManager.keysForSuggestions(), builder);

	private WaypointCommand() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
			Commands.literal("waypoint")
				.then(Commands.literal("add")
					.then(Commands.argument("name", StringArgumentType.word())
						.executes(WaypointCommand::addWaypoint)))
				.then(Commands.literal("set")
					.then(Commands.argument("name", StringArgumentType.word())
						.executes(WaypointCommand::setWaypoint)))
				.then(Commands.literal("list")
					.executes(WaypointCommand::listWaypoints))
				.then(Commands.literal("view")
					.then(Commands.argument("name", StringArgumentType.word())
						.suggests(WAYPOINT_SUGGESTIONS)
						.executes(WaypointCommand::viewWaypoint)))
				.then(Commands.literal("remove")
					.then(Commands.argument("name", StringArgumentType.word())
						.suggests(WAYPOINT_SUGGESTIONS)
						.executes(WaypointCommand::removeWaypoint)))
				.then(Commands.literal("rename")
					.then(Commands.argument("oldName", StringArgumentType.word())
						.suggests(WAYPOINT_SUGGESTIONS)
						.then(Commands.argument("newName", StringArgumentType.word())
							.executes(WaypointCommand::renameWaypoint))))
				.then(Commands.literal("compass")
					.then(Commands.literal("add")
						.then(Commands.argument("name", StringArgumentType.word())
							.suggests(WAYPOINT_SUGGESTIONS)
							.executes(WaypointCommand::addCompass))))
		));
	}

	private static int addWaypoint(CommandContext<CommandSourceStack> context) {
		ServerPlayer player = context.getSource().getPlayer();
		if (player == null) {
			context.getSource().sendFailure(Component.literal("This command can only be run by a player."));
			return 0;
		}

		String name = StringArgumentType.getString(context, "name");
		WaypointManager.AddResult result = WaypointManager.addWaypoint(name, player);
		switch (result) {
			case CREATED -> {
				Waypoint waypoint = WaypointManager.getWaypoint(name).orElseThrow();
				context.getSource().sendSuccess(() -> Component.literal(
					"Added waypoint " + waypoint.getName() + " at "
						+ waypoint.getX() + " " + waypoint.getY() + " " + waypoint.getZ()
						+ " in " + waypoint.getDimensionId()
				), false);
				return 1;
			}
			case ALREADY_EXISTS -> {
				context.getSource().sendFailure(Component.literal("A waypoint with that name already exists."));
				return 0;
			}
			case INVALID_NAME -> {
				context.getSource().sendFailure(Component.literal("Invalid name. Use 1-32 characters: A-Z, a-z, 0-9, _ or -"));
				return 0;
			}
		}
		return 0;
	}

	private static int listWaypoints(CommandContext<CommandSourceStack> context) {
		List<Waypoint> waypoints = WaypointManager.listWaypoints();
		if (waypoints.isEmpty()) {
			context.getSource().sendSuccess(() -> Component.literal("No waypoints set yet."), false);
			return 1;
		}

		context.getSource().sendSuccess(() -> Component.literal("Global waypoints (" + waypoints.size() + "):"), false);
		for (Waypoint waypoint : waypoints) {
			context.getSource().sendSuccess(() -> Component.literal(
				"- " + waypoint.getName() + " -> "
					+ waypoint.getX() + " " + waypoint.getY() + " " + waypoint.getZ()
					+ " (" + waypoint.getDimensionId() + ")"
			), false);
		}
		return 1;
	}

	private static int setWaypoint(CommandContext<CommandSourceStack> context) {
		ServerPlayer player = context.getSource().getPlayer();
		if (player == null) {
			context.getSource().sendFailure(Component.literal("This command can only be run by a player."));
			return 0;
		}

		String name = StringArgumentType.getString(context, "name");
		WaypointManager.SetResult result = WaypointManager.setWaypoint(name, player);
		switch (result) {
			case CREATED -> {
				Waypoint waypoint = WaypointManager.getWaypoint(name).orElseThrow();
				context.getSource().sendSuccess(() -> Component.literal(
					"Created waypoint " + waypoint.getName() + " at "
						+ waypoint.getX() + " " + waypoint.getY() + " " + waypoint.getZ()
						+ " in " + waypoint.getDimensionId()
				), false);
				return 1;
			}
			case UPDATED -> {
				Waypoint waypoint = WaypointManager.getWaypoint(name).orElseThrow();
				context.getSource().sendSuccess(() -> Component.literal(
					"Updated waypoint " + waypoint.getName() + " to "
						+ waypoint.getX() + " " + waypoint.getY() + " " + waypoint.getZ()
						+ " in " + waypoint.getDimensionId()
				), false);
				return 1;
			}
			case INVALID_NAME -> {
				context.getSource().sendFailure(Component.literal("Invalid name. Use 1-32 characters: A-Z, a-z, 0-9, _ or -"));
				return 0;
			}
		}

		return 0;
	}

	private static int viewWaypoint(CommandContext<CommandSourceStack> context) {
		String name = StringArgumentType.getString(context, "name");
		Optional<Waypoint> maybeWaypoint = WaypointManager.getWaypoint(name);
		if (maybeWaypoint.isEmpty()) {
			context.getSource().sendFailure(Component.literal("Waypoint not found: " + name));
			return 0;
		}

		Waypoint waypoint = maybeWaypoint.get();
		String createdAt = TIME_FORMATTER.format(Instant.ofEpochMilli(waypoint.getCreatedAtEpochMillis()).atOffset(ZoneOffset.UTC));
		context.getSource().sendSuccess(() -> Component.literal("Waypoint: " + waypoint.getName()), false);
		context.getSource().sendSuccess(() -> Component.literal(
			"Coordinates: " + waypoint.getX() + " " + waypoint.getY() + " " + waypoint.getZ()), false);
		context.getSource().sendSuccess(() -> Component.literal("Dimension: " + waypoint.getDimensionId()), false);
		context.getSource().sendSuccess(() -> Component.literal("Created by: " + waypoint.getCreatedByName()), false);
		context.getSource().sendSuccess(() -> Component.literal("Created at (UTC): " + createdAt), false);
		return 1;
	}

	private static int removeWaypoint(CommandContext<CommandSourceStack> context) {
		String name = StringArgumentType.getString(context, "name");
		boolean removed = WaypointManager.removeWaypoint(name);
		if (!removed) {
			context.getSource().sendFailure(Component.literal("Waypoint not found: " + name));
			return 0;
		}

		context.getSource().sendSuccess(() -> Component.literal("Removed waypoint: " + name), true);
		return 1;
	}

	private static int renameWaypoint(CommandContext<CommandSourceStack> context) {
		String oldName = StringArgumentType.getString(context, "oldName");
		String newName = StringArgumentType.getString(context, "newName");

		WaypointManager.RenameResult result = WaypointManager.renameWaypoint(oldName, newName);
		switch (result) {
			case RENAMED -> {
				context.getSource().sendSuccess(() -> Component.literal("Renamed waypoint " + oldName + " to " + newName), true);
				return 1;
			}
			case NOT_FOUND -> {
				context.getSource().sendFailure(Component.literal("Waypoint not found: " + oldName));
				return 0;
			}
			case NEW_NAME_EXISTS -> {
				context.getSource().sendFailure(Component.literal("A waypoint with the new name already exists."));
				return 0;
			}
			case INVALID_NAME -> {
				context.getSource().sendFailure(Component.literal("Invalid new name. Use 1-32 characters: A-Z, a-z, 0-9, _ or -"));
				return 0;
			}
			case SAME_NAME -> {
				context.getSource().sendFailure(Component.literal("New name must be different from old name."));
				return 0;
			}
		}

		return 0;
	}

	private static int addCompass(CommandContext<CommandSourceStack> context) {
		ServerPlayer player = context.getSource().getPlayer();
		if (player == null) {
			context.getSource().sendFailure(Component.literal("This command can only be run by a player."));
			return 0;
		}

		String name = StringArgumentType.getString(context, "name");
		Optional<Waypoint> maybeWaypoint = WaypointManager.getWaypoint(name);
		if (maybeWaypoint.isEmpty()) {
			context.getSource().sendFailure(Component.literal("Waypoint not found: " + name));
			return 0;
		}

		Waypoint waypoint = maybeWaypoint.get();
		boolean inserted = player.getInventory().add(WaypointCompassService.createCompassForWaypoint(waypoint));
		if (!inserted) {
			player.drop(WaypointCompassService.createCompassForWaypoint(waypoint), false, false);
		}
		player.containerMenu.broadcastChanges();

		context.getSource().sendSuccess(() -> Component.literal(
			"Added waypoint compass for " + waypoint.getName() + " (" + waypoint.getX() + " " + waypoint.getY() + " " + waypoint.getZ() + ")"
		), false);
		return 1;
	}
}
