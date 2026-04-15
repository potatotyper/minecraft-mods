package com.centralwaypoint.waypoint;

import com.centralwaypoint.Centralwaypoints;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public final class WaypointManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path WAYPOINT_PATH = FabricLoader.getInstance()
		.getConfigDir()
		.resolve("central-waypoints.json");
	private static final Pattern VALID_NAME = Pattern.compile("^[A-Za-z0-9_-]{1,32}$");

	private static WaypointStore store = WaypointStore.createDefault();

	private WaypointManager() {
	}

	public static synchronized void loadOrCreate() {
		try {
			if (Files.notExists(WAYPOINT_PATH)) {
				store = WaypointStore.createDefault();
				save();
				return;
			}

			try (Reader reader = Files.newBufferedReader(WAYPOINT_PATH, StandardCharsets.UTF_8)) {
				WaypointStore loaded = GSON.fromJson(reader, WaypointStore.class);
				if (loaded == null || loaded.getWaypoints() == null) {
					Centralwaypoints.LOGGER.warn("Waypoint store was empty or invalid, resetting defaults.");
					store = WaypointStore.createDefault();
					save();
				} else {
					store = loaded;
				}
			}
		} catch (Exception exception) {
			Centralwaypoints.LOGGER.error("Failed to load waypoints, using empty store.", exception);
			store = WaypointStore.createDefault();
			save();
		}
	}

	public static synchronized void save() {
		try {
			Files.createDirectories(WAYPOINT_PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(WAYPOINT_PATH, StandardCharsets.UTF_8)) {
				GSON.toJson(store, writer);
			}
		} catch (IOException exception) {
			Centralwaypoints.LOGGER.error("Failed to save waypoints.", exception);
		}
	}

	public static synchronized AddResult addWaypoint(String rawName, ServerPlayer player) {
		if (!isValidName(rawName)) {
			return AddResult.INVALID_NAME;
		}

		String key = normalizeName(rawName);
		Map<String, Waypoint> waypoints = store.getWaypoints();
		if (waypoints.containsKey(key)) {
			return AddResult.ALREADY_EXISTS;
		}

		waypoints.put(key, Waypoint.fromPlayer(rawName, player));
		save();
		return AddResult.CREATED;
	}

	public static synchronized SetResult setWaypoint(String rawName, ServerPlayer player) {
		if (!isValidName(rawName)) {
			return SetResult.INVALID_NAME;
		}

		String key = normalizeName(rawName);
		boolean existed = store.getWaypoints().containsKey(key);
		store.getWaypoints().put(key, Waypoint.fromPlayer(rawName, player));
		save();
		return existed ? SetResult.UPDATED : SetResult.CREATED;
	}

	public static synchronized Optional<Waypoint> getWaypoint(String rawName) {
		String key = normalizeName(rawName);
		return Optional.ofNullable(store.getWaypoints().get(key));
	}

	public static synchronized boolean removeWaypoint(String rawName) {
		String key = normalizeName(rawName);
		Waypoint removed = store.getWaypoints().remove(key);
		if (removed != null) {
			save();
			return true;
		}
		return false;
	}

	public static synchronized RenameResult renameWaypoint(String oldRawName, String newRawName) {
		if (!isValidName(newRawName)) {
			return RenameResult.INVALID_NAME;
		}

		String oldKey = normalizeName(oldRawName);
		String newKey = normalizeName(newRawName);

		if (oldKey.equals(newKey)) {
			return RenameResult.SAME_NAME;
		}

		Map<String, Waypoint> waypoints = store.getWaypoints();
		Waypoint existing = waypoints.get(oldKey);
		if (existing == null) {
			return RenameResult.NOT_FOUND;
		}
		if (waypoints.containsKey(newKey)) {
			return RenameResult.NEW_NAME_EXISTS;
		}

		existing.setName(newRawName);
		waypoints.remove(oldKey);
		waypoints.put(newKey, existing);
		save();
		return RenameResult.RENAMED;
	}

	public static synchronized List<Waypoint> listWaypoints() {
		List<Waypoint> values = new ArrayList<>(store.getWaypoints().values());
		values.sort(Comparator.comparing(value -> value.getName().toLowerCase(Locale.ROOT)));
		return values;
	}

	public static synchronized boolean isValidName(String rawName) {
		if (rawName == null) {
			return false;
		}
		String trimmed = rawName.trim();
		return VALID_NAME.matcher(trimmed).matches();
	}

	public static String normalizeName(String rawName) {
		if (rawName == null) {
			return "";
		}
		return rawName.trim().toLowerCase(Locale.ROOT);
	}

	public static synchronized List<String> keysForSuggestions() {
		return new ArrayList<>(store.getWaypoints().keySet());
	}

	public enum AddResult {
		CREATED,
		ALREADY_EXISTS,
		INVALID_NAME
	}

	public enum RenameResult {
		RENAMED,
		NOT_FOUND,
		NEW_NAME_EXISTS,
		INVALID_NAME,
		SAME_NAME
	}

	public enum SetResult {
		CREATED,
		UPDATED,
		INVALID_NAME
	}
}
