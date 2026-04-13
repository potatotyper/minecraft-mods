package com.potatotyper.noelytra.enforcement;

import com.potatotyper.noelytra.config.ConfigManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Map;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ElytraEnforcer {
	private static final String BLOCKED_MESSAGE = "Elytra is disabled in this dimension.";
	private static final long MESSAGE_COOLDOWN_MILLIS = 2000L;
	private static final long PENDING_DROP_DELAY_MILLIS = 1000L;
	private static final Map<UUID, Long> LAST_MESSAGE_TIME = new ConcurrentHashMap<>();
	private static final Map<UUID, List<PendingReturn>> PENDING_RETURNS = new ConcurrentHashMap<>();

	private record PendingReturn(ItemStack stack, long earliestDropAtMillis) {
	}

	private ElytraEnforcer() {
	}

	public static boolean isBlocked(ServerPlayer player) {
		Identifier dimensionId = player.level().dimension().identifier();
		return !ConfigManager.getConfig().isDimensionAllowed(dimensionId.toString());
	}

	public static boolean forceUnequipIfBlocked(ServerPlayer player) {
		if (!isBlocked(player)) {
			return false;
		}

		ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
		if (!chestStack.is(Items.ELYTRA)) {
			return false;
		}

		ItemStack toMove = chestStack.copy();
		player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);

		boolean inserted = player.getInventory().add(toMove);
		if (!inserted) {
			enqueuePendingReturn(player, toMove);
		}

		player.containerMenu.broadcastChanges();
		sendBlockedMessage(player);
		return true;
	}

	private static void enqueuePendingReturn(ServerPlayer player, ItemStack stack) {
		long dropAt = System.currentTimeMillis() + PENDING_DROP_DELAY_MILLIS;
		PENDING_RETURNS
			.computeIfAbsent(player.getUUID(), key -> new ArrayList<>())
			.add(new PendingReturn(stack, dropAt));
	}

	private static void processPendingReturns(ServerPlayer player) {
		List<PendingReturn> pending = PENDING_RETURNS.get(player.getUUID());
		if (pending == null || pending.isEmpty()) {
			return;
		}

		long now = System.currentTimeMillis();
		boolean changed = false;
		Iterator<PendingReturn> iterator = pending.iterator();
		while (iterator.hasNext()) {
			PendingReturn entry = iterator.next();
			if (player.getInventory().add(entry.stack())) {
				iterator.remove();
				changed = true;
				continue;
			}

			if (now >= entry.earliestDropAtMillis()) {
				player.drop(entry.stack(), false, false);
				iterator.remove();
				changed = true;
			}
		}

		if (pending.isEmpty()) {
			PENDING_RETURNS.remove(player.getUUID());
		}

		if (changed) {
			player.containerMenu.broadcastChanges();
		}
	}

	public static void sendBlockedMessage(ServerPlayer player) {
		long now = System.currentTimeMillis();
		Long last = LAST_MESSAGE_TIME.get(player.getUUID());
		if (last != null && now - last < MESSAGE_COOLDOWN_MILLIS) {
			return;
		}

		LAST_MESSAGE_TIME.put(player.getUUID(), now);
		player.sendSystemMessage(Component.literal(BLOCKED_MESSAGE), true);
	}

	public static void revalidateAllPlayers(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			processPendingReturns(player);
			forceUnequipIfBlocked(player);
		}
	}
}