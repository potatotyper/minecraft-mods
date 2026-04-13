package com.potatotyper.noelytra.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.potatotyper.noelytra.Noelytramod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance()
		.getConfigDir()
		.resolve("no-elytra-mod.json");

	private static NoElytraConfig config = NoElytraConfig.createDefault();

	private ConfigManager() {
	}

	public static void loadOrCreate() {
		try {
			if (Files.notExists(CONFIG_PATH)) {
				config = NoElytraConfig.createDefault();
				save();
				return;
			}

			try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
				NoElytraConfig loaded = GSON.fromJson(reader, NoElytraConfig.class);
				if (loaded == null || loaded.getAllowedDimensions() == null) {
					Noelytramod.LOGGER.warn("Config was empty or invalid; resetting defaults.");
					config = NoElytraConfig.createDefault();
					save();
				} else {
					config = loaded;
				}
			}
		} catch (Exception exception) {
			Noelytramod.LOGGER.error("Failed to load config, using defaults.", exception);
			config = NoElytraConfig.createDefault();
			save();
		}
	}

	public static void save() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
				GSON.toJson(config, writer);
			}
		} catch (IOException exception) {
			Noelytramod.LOGGER.error("Failed to save config.", exception);
		}
	}

	public static NoElytraConfig getConfig() {
		return config;
	}
}