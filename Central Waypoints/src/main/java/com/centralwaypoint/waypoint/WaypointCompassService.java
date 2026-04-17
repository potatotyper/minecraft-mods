package com.centralwaypoint.waypoint;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WaypointCompassService {
	private static final Map<UUID, String> LAST_ACTIONBAR_TEXT = new ConcurrentHashMap<>();

	private WaypointCompassService() {
	}

	public static void registerTickHandler() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				sendHeldCompassWaypointInfo(player);
			}
		});
	}

	public static ItemStack createCompassForWaypoint(Waypoint waypoint) {
		ItemStack stack = new ItemStack(Items.COMPASS);

		Identifier dimensionId = Identifier.tryParse(waypoint.getDimensionId());
		if (dimensionId != null) {
			ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, dimensionId);
			GlobalPos globalPos = GlobalPos.of(dimensionKey, waypoint.blockPos());
			// Keep tracking to explicit coordinates without requiring an actual lodestone block.
			stack.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(globalPos), false));
		}

		stack.set(DataComponents.CUSTOM_NAME, Component.literal(
			waypoint.getName() + " (" + waypoint.getX() + ", " + waypoint.getY() + ", " + waypoint.getZ() + ")"
		));
		return stack;
	}

	public static int removeWaypointCompasses(ServerPlayer player) {
		int removed = 0;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (isWaypointCompass(stack)) {
				removed += stack.getCount();
				player.getInventory().setItem(slot, ItemStack.EMPTY);
			}
		}
		LAST_ACTIONBAR_TEXT.remove(player.getUUID());
		return removed;
	}

	private static void sendHeldCompassWaypointInfo(ServerPlayer player) {
		ItemStack held = findHeldCompass(player);
		if (held.isEmpty()) {
			LAST_ACTIONBAR_TEXT.remove(player.getUUID());
			return;
		}

		LodestoneTracker tracker = held.get(DataComponents.LODESTONE_TRACKER);
		if (tracker == null || tracker.target().isEmpty()) {
			LAST_ACTIONBAR_TEXT.remove(player.getUUID());
			return;
		}

		GlobalPos target = tracker.target().get();
		BlockPos targetPos = target.pos();
		String targetDimension = target.dimension().identifier().toString();

		String waypointName = WaypointManager.listWaypoints().stream()
			.filter(waypoint -> waypoint.getDimensionId().equals(targetDimension)
				&& waypoint.getX() == targetPos.getX()
				&& waypoint.getY() == targetPos.getY()
				&& waypoint.getZ() == targetPos.getZ())
			.map(Waypoint::getName)
			.findFirst()
			.orElse("Unknown");

		String playerDimension = player.level().dimension().identifier().toString();
		String distanceText;
		if (!playerDimension.equals(targetDimension)) {
			distanceText = "different dimension";
		} else {
			double dx = player.getX() - targetPos.getX();
			double dy = player.getY() - targetPos.getY();
			double dz = player.getZ() - targetPos.getZ();
			int distance = (int) Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz));
			distanceText = distance + "m";
		}

		String actionbar =
			"Waypoint " + waypointName + " | "
				+ targetPos.getX() + " " + targetPos.getY() + " " + targetPos.getZ()
				+ " | " + targetDimension
				+ " | " + distanceText;

		String previous = LAST_ACTIONBAR_TEXT.get(player.getUUID());
		if (!actionbar.equals(previous)) {
			player.sendSystemMessage(Component.literal(actionbar), true);
			LAST_ACTIONBAR_TEXT.put(player.getUUID(), actionbar);
		}
	}

	private static ItemStack findHeldCompass(ServerPlayer player) {
		ItemStack mainHand = player.getMainHandItem();
		if (isWaypointCompass(mainHand)) {
			return mainHand;
		}

		ItemStack offHand = player.getOffhandItem();
		if (isWaypointCompass(offHand)) {
			return offHand;
		}

		return ItemStack.EMPTY;
	}

	private static boolean isWaypointCompass(ItemStack stack) {
		if (stack.isEmpty() || !stack.is(Items.COMPASS)) {
			return false;
		}

		LodestoneTracker tracker = stack.get(DataComponents.LODESTONE_TRACKER);
		return tracker != null && tracker.target().isPresent();
	}
}
