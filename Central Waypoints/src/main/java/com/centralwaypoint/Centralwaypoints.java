package com.centralwaypoint;

import com.centralwaypoint.command.WaypointCommand;
import com.centralwaypoint.waypoint.WaypointCompassService;
import com.centralwaypoint.waypoint.WaypointManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Centralwaypoints implements ModInitializer {
	public static final String MOD_ID = "central-waypoints";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		WaypointManager.loadOrCreate();
		WaypointCommand.register();
		WaypointCompassService.registerTickHandler();

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> WaypointManager.save());

		LOGGER.info("Central Waypoints initialized");
	}
}