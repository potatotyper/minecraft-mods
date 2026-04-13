package com.potatotyper.noelytra;

import com.potatotyper.noelytra.command.ElytraCommand;
import com.potatotyper.noelytra.config.ConfigManager;
import com.potatotyper.noelytra.enforcement.ElytraEnforcer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Noelytramod implements ModInitializer {
	public static final String MOD_ID = "no-elytra-mod";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ConfigManager.loadOrCreate();
		ElytraCommand.register();

		// Re-check all players each tick so denied dimensions auto-unequip Elytra.
		ServerTickEvents.END_SERVER_TICK.register(ElytraEnforcer::revalidateAllPlayers);

		LOGGER.info("No Elytra Mod initialized");
	}
}