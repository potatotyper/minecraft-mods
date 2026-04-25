package com.example.client;

import com.example.Extar_hotbar;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.mojang.blaze3d.platform.InputConstants;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public class Extar_hotbarClient implements ClientModInitializer {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("extar_hotbar.json");

	private static final ClientConfig CONFIG = loadConfig();

	private static KeyMapping swapKey;
	private static KeyMapping panelCycleKey;
	private static KeyMapping slotLockKey;

	private static final boolean[] HOTBAR_KEY_STATES = new boolean[9];
	private static final long[] HOTBAR_KEY_TIMERS = new long[9];
	private static long swapKeyTimer;
	private static boolean swapHandledOnHold;
	private static boolean swapPressedLastTick;

	private static boolean secondaryRowActive;

	@Override
	public void onInitializeClient() {
		KeyMapping.Category keyCategory = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("extar_hotbar", "keybinds"));
		swapKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.extar_hotbar.swap",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_R,
			keyCategory
		));
		panelCycleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.extar_hotbar.cycle_panels",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_H,
			keyCategory
		));
		slotLockKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.extar_hotbar.toggle_slot_lock",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_L,
			keyCategory
		));

		ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
	}

	private void onClientTick(Minecraft client) {
		if (client.player == null) {
			return;
		}

		handlePanelCycle();
		handleSlotLockToggle(client.player);
		handleSwapKey(client.player);
		handlePerSlotDoubleTap(client);
	}

	private void handlePanelCycle() {
		while (panelCycleKey.consumeClick()) {
			CONFIG.panelMode = (CONFIG.panelMode + 1) % 4;
			saveConfig();
		}
	}

	private void handleSlotLockToggle(LocalPlayer player) {
		while (slotLockKey.consumeClick()) {
			int selectedSlot = player.getInventory().getSelectedSlot();
			if (CONFIG.lockedSlots.contains(selectedSlot)) {
				CONFIG.lockedSlots.remove(selectedSlot);
				showOverlay(Component.translatable("text.extar_hotbar.slot_unlocked", selectedSlot + 1));
			} else {
				CONFIG.lockedSlots.add(selectedSlot);
				showOverlay(Component.translatable("text.extar_hotbar.slot_locked", selectedSlot + 1));
			}
			saveConfig();
		}
	}

	private void handleSwapKey(LocalPlayer player) {
		boolean pressed = swapKey.isDown();
		long now = Instant.now().toEpochMilli();

		if (pressed && !swapPressedLastTick) {
			swapKeyTimer = now;
			swapHandledOnHold = false;
		}

		if (CONFIG.enableHoldSwap && pressed && !swapHandledOnHold && now - swapKeyTimer >= CONFIG.holdSwapMs) {
			if (CONFIG.holdSwapsSingleSlot) {
				swapSingleSlot(player, player.getInventory().getSelectedSlot());
			} else {
				swapFullRow(player);
			}
			swapHandledOnHold = true;
		}

		if (!pressed && swapPressedLastTick) {
			if (!CONFIG.enableHoldSwap || now - swapKeyTimer < CONFIG.holdSwapMs) {
				if (CONFIG.enableHoldSwap && CONFIG.holdSwapsSingleSlot) {
					swapFullRow(player);
				} else if (CONFIG.enableHoldSwap) {
					swapSingleSlot(player, player.getInventory().getSelectedSlot());
				} else {
					swapFullRow(player);
				}
			}
			swapHandledOnHold = false;
		}

		swapPressedLastTick = pressed;
	}

	private void handlePerSlotDoubleTap(Minecraft client) {
		if (!CONFIG.enableDoubleTapSwap || client.player == null || client.options == null) {
			return;
		}

		long now = Instant.now().toEpochMilli();
		for (int i = 0; i < 9; i++) {
			boolean down = client.options.keyHotbarSlots[i].isDown();
			if (down != HOTBAR_KEY_STATES[i]) {
				HOTBAR_KEY_STATES[i] = down;
				if (down) {
					if (now - HOTBAR_KEY_TIMERS[i] <= CONFIG.doubleTapWindowMs) {
						swapSingleSlot(client.player, i);
						HOTBAR_KEY_TIMERS[i] = 0;
					} else {
						HOTBAR_KEY_TIMERS[i] = now;
					}
				}
			}
		}
	}

	private void swapFullRow(LocalPlayer player) {
		if (player.isSpectator()) {
			return;
		}
		int sourceBase = getSecondaryInventorySourceBase();
		boolean swappedAny = false;

		for (int slot = 0; slot < 9; slot++) {
			if (CONFIG.lockedSlots.contains(slot)) {
				continue;
			}
			int sourceSlot = sourceBase + slot;
			if (sourceSlot < 0 || sourceSlot >= player.getInventory().getContainerSize()) {
				continue;
			}
			if (swapInventorySlots(player, slot, sourceSlot)) {
				swappedAny = true;
			}
		}

		if (swappedAny) {
			secondaryRowActive = !secondaryRowActive;
		}
	}

	private void swapSingleSlot(LocalPlayer player, int slot) {
		if (player.isSpectator() || CONFIG.lockedSlots.contains(slot)) {
			return;
		}
		int sourceSlot = getSecondaryInventorySourceBase() + slot;
		if (sourceSlot < 0 || sourceSlot >= player.getInventory().getContainerSize()) {
			return;
		}
		swapInventorySlots(player, slot, sourceSlot);
	}

	private boolean swapInventorySlots(LocalPlayer player, int hotbarSlot, int sourceSlot) {
		ItemStack hotbarStack = player.getInventory().getItem(hotbarSlot).copy();
		ItemStack sourceStack = player.getInventory().getItem(sourceSlot).copy();

		if (ItemStack.isSameItemSameComponents(hotbarStack, sourceStack) && hotbarStack.getCount() == sourceStack.getCount()) {
			return false;
		}

		player.getInventory().setItem(hotbarSlot, sourceStack);
		player.getInventory().setItem(sourceSlot, hotbarStack);
		player.inventoryMenu.broadcastChanges();
		return true;
	}

	private void showOverlay(Component component) {
		Minecraft client = Minecraft.getInstance();
		if (client.gui != null) {
			client.gui.setOverlayMessage(component, false);
		}
	}

	public static int getSecondaryInventoryRowZeroBased() {
		return CONFIG.secondaryInventoryRow - 1;
	}

	public static int getSecondaryInventorySourceBase() {
		return 9 + getSecondaryInventoryRowZeroBased() * 9;
	}

	public static boolean showSecondHotbar() {
		return CONFIG.showSecondHotbar;
	}

	public static boolean showActiveIndicator() {
		return CONFIG.showActiveIndicator;
	}

	public static int getSecondHotbarYOffset() {
		return CONFIG.secondHotbarYOffset;
	}

	public static int getActiveIndicatorYOffset() {
		return CONFIG.activeIndicatorYOffset;
	}

	public static boolean showArmorPanel() {
		return CONFIG.panelMode == 1 || CONFIG.panelMode == 3;
	}

	public static boolean showFoodPanel() {
		return CONFIG.panelMode == 2 || CONFIG.panelMode == 3;
	}

	public static int getPanelX() {
		return CONFIG.panelX;
	}

	public static int getPanelY() {
		return CONFIG.panelY;
	}

	public static boolean showArmorDurabilityPercent() {
		return CONFIG.showArmorDurabilityPercent;
	}

	public static boolean isSlotLocked(int slot) {
		return CONFIG.lockedSlots.contains(slot);
	}

	public static boolean isSecondaryRowActive() {
		return secondaryRowActive;
	}

	private static ClientConfig loadConfig() {
		if (!Files.exists(CONFIG_PATH)) {
			ClientConfig defaults = new ClientConfig();
			saveConfig(defaults);
			return defaults;
		}

		try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
			ClientConfig loaded = GSON.fromJson(reader, ClientConfig.class);
			if (loaded == null) {
				return new ClientConfig();
			}
			loaded.normalize();
			return loaded;
		} catch (IOException | JsonParseException error) {
			Extar_hotbar.LOGGER.error("Failed to load extar_hotbar config, using defaults", error);
			return new ClientConfig();
		}
	}

	private static void saveConfig() {
		saveConfig(CONFIG);
	}

	private static void saveConfig(ClientConfig config) {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
				GSON.toJson(config, writer);
			}
		} catch (IOException error) {
			Extar_hotbar.LOGGER.error("Failed to save extar_hotbar config", error);
		}
	}

	private static class ClientConfig {
		public int secondaryInventoryRow = 1;
		public boolean showSecondHotbar = true;
		public boolean showActiveIndicator = true;
		public int secondHotbarYOffset = 22;
		public int activeIndicatorYOffset = 56;

		public boolean enableDoubleTapSwap = true;
		public int doubleTapWindowMs = 300;
		public boolean enableHoldSwap = true;
		public int holdSwapMs = 200;
		public boolean holdSwapsSingleSlot = false;

		public int panelMode = 0;
		public int panelX = 6;
		public int panelY = 6;
		public boolean showArmorDurabilityPercent = true;

		public Set<Integer> lockedSlots = new HashSet<>();

		private void normalize() {
			secondaryInventoryRow = clamp(secondaryInventoryRow, 1, 3);
			secondHotbarYOffset = clamp(secondHotbarYOffset, 0, 80);
			activeIndicatorYOffset = clamp(activeIndicatorYOffset, 12, 140);
			doubleTapWindowMs = clamp(doubleTapWindowMs, 100, 1000);
			holdSwapMs = clamp(holdSwapMs, 100, 1200);
			panelX = clamp(panelX, 0, 10000);
			panelY = clamp(panelY, 0, 10000);
			if (lockedSlots == null) {
				lockedSlots = new HashSet<>();
			}
			lockedSlots.removeIf(slot -> slot < 0 || slot > 8);
		}
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(value, max));
	}
}
