package com.example.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.mojang.blaze3d.platform.InputConstants;
import com.example.Extar_hotbar;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public class Extar_hotbarClient implements ClientModInitializer {
	private static final String KEY_CATEGORY = "key.categories.extar_hotbar";
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
		KeyMapping.Category keyCategory = KeyMapping.Category.MISC;
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
		renderOverlay(client);
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
		int sourceBase = (CONFIG.secondaryInventoryRow - 1) * 9;
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
		int sourceSlot = (CONFIG.secondaryInventoryRow - 1) * 9 + slot;
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

	private void renderOverlay(Minecraft client) {
		if (client.player == null || client.options.hideGui || client.player.isSpectator()) {
			return;
		}

		StringBuilder overlay = new StringBuilder();
		overlay.append(secondaryRowActive
			? Component.translatable("text.extar_hotbar.active_secondary").getString()
			: Component.translatable("text.extar_hotbar.active_primary").getString());

		if (CONFIG.showSecondHotbar) {
			overlay.append(" | 2nd: ");
			for (int slot = 0; slot < 9; slot++) {
				if (slot > 0) {
					overlay.append(" ");
				}
				ItemStack stack = client.player.getInventory().getItem((CONFIG.secondaryInventoryRow - 1) * 9 + slot);
				String name = stack.isEmpty() ? "-" : stack.getHoverName().getString();
				overlay.append(slot + 1).append(":").append(shorten(name, 8));
				if (CONFIG.lockedSlots.contains(slot)) {
					overlay.append("*");
				}
			}
		}

		if (CONFIG.panelMode == 1 || CONFIG.panelMode == 3) {
			overlay.append(" | ").append(Component.translatable("text.extar_hotbar.armor_panel").getString()).append(": ");
			EquipmentSlot[] slots = new EquipmentSlot[] {
				EquipmentSlot.HEAD,
				EquipmentSlot.CHEST,
				EquipmentSlot.LEGS,
				EquipmentSlot.FEET
			};
			for (int i = 0; i < slots.length; i++) {
				if (i > 0) {
					overlay.append(", ");
				}
				ItemStack armor = client.player.getItemBySlot(slots[i]);
				if (armor.isEmpty() || !armor.isDamageableItem()) {
					overlay.append(Component.translatable("text.extar_hotbar.none").getString());
				} else {
					int remaining = armor.getMaxDamage() - armor.getDamageValue();
					int percent = (int) ((remaining * 100.0f) / armor.getMaxDamage());
					overlay.append(shorten(armor.getHoverName().getString(), 6)).append(":").append(remaining);
					if (CONFIG.showArmorDurabilityPercent) {
						overlay.append("(").append(percent).append("%)");
					}
				}
			}
		}

		if (CONFIG.panelMode == 2 || CONFIG.panelMode == 3) {
			overlay.append(" | ").append(Component.translatable("text.extar_hotbar.food_panel").getString()).append(": ");
			Set<String> shown = new LinkedHashSet<>();
			int shownCount = 0;
			for (ItemStack stack : client.player.getInventory().getNonEquipmentItems()) {
				FoodProperties food = stack.get(DataComponents.FOOD);
				if (stack.isEmpty() || food == null) {
					continue;
				}
				String key = stack.getItem().toString();
				if (!shown.add(key)) {
					continue;
				}
				if (shownCount++ > 0) {
					overlay.append(", ");
				}
				overlay.append(shorten(stack.getHoverName().getString(), 10))
					.append(" H:").append(food.nutrition())
					.append(" S:").append(String.format("%.1f", food.saturation()));
				if (shownCount >= 4) {
					overlay.append(" ...");
					break;
				}
			}
			if (shownCount == 0) {
				overlay.append(Component.translatable("text.extar_hotbar.none").getString());
			}
		}

		showOverlay(Component.literal(overlay.toString()));
	}

	private void showOverlay(Component component) {
		Minecraft client = Minecraft.getInstance();
		if (client.gui != null) {
			client.gui.setOverlayMessage(component, false);
		}
	}

	private static String shorten(String value, int maxLength) {
		if (value.length() <= maxLength) {
			return value;
		}
		if (maxLength <= 1) {
			return value.substring(0, maxLength);
		}
		return value.substring(0, maxLength - 1) + ".";
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
		public int secondaryInventoryRow = 3;
		public boolean showSecondHotbar = true;
		public boolean showActiveIndicator = true;
		public int secondHotbarYOffset = 24;
		public int activeIndicatorYOffset = 36;

		public boolean enableDoubleTapSwap = true;
		public int doubleTapWindowMs = 300;
		public boolean enableHoldSwap = true;
		public int holdSwapMs = 200;
		public boolean holdSwapsSingleSlot = false;

		public int panelMode = 0;
		public int panelX = 4;
		public int panelY = 4;
		public boolean showArmorDurabilityPercent = true;

		public Set<Integer> lockedSlots = new HashSet<>();

		private void normalize() {
			secondaryInventoryRow = clamp(secondaryInventoryRow, 1, 3);
			doubleTapWindowMs = clamp(doubleTapWindowMs, 100, 1000);
			holdSwapMs = clamp(holdSwapMs, 100, 1200);
			panelMode = clamp(panelMode, 0, 3);
			if (lockedSlots == null) {
				lockedSlots = new HashSet<>();
			}
			lockedSlots.removeIf(slot -> slot < 0 || slot > 8);
		}
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
}