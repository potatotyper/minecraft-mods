package com.example.client.mixin;

import com.example.client.Extar_hotbarClient;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Shadow
	private static Identifier HOTBAR_SPRITE;

	@Shadow
	private Player getCameraPlayer() {
		throw new AssertionError();
	}

	@Shadow
	private void extractSlot(GuiGraphicsExtractor graphics, int x, int y, DeltaTracker deltaTracker, Player player, ItemStack stack, int seed) {
		throw new AssertionError();
	}

	@Inject(method = "extractItemHotbar", at = @At("TAIL"))
	private void extarHotbar$renderSecondHotbar(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		Player player = getCameraPlayer();
		if (player == null || player.isSpectator() || !Extar_hotbarClient.showSecondHotbar()) {
			return;
		}

		int width = graphics.guiWidth();
		int height = graphics.guiHeight();
		int shift = Math.max(0, Extar_hotbarClient.getSecondHotbarYOffset());
		int hotbarX = width / 2 - 91;
		int hotbarY = height - 22 - shift;
		graphics.fill(hotbarX, hotbarY, hotbarX + 182, hotbarY + 22, 0x60000000);

		int seed = 1;
		int rowBase = Extar_hotbarClient.getSecondaryInventoryRowZeroBased() * 9;
		for (int slot = 0; slot < 9; slot++) {
			int slotX = hotbarX + 1 + slot * 20 + 1;
			int slotY = hotbarY + 3;
			ItemStack stack = player.getInventory().getItem(rowBase + slot);

			extractSlot(graphics, slotX, slotY, deltaTracker, player, stack, seed++);
			if (Extar_hotbarClient.isSlotLocked(slot)) {
				graphics.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, 0x55AA3333);
			}
			graphics.text(minecraft.font, String.valueOf(slot + 1), slotX + 1, slotY - 8, 0xFFFFFF, false);
		}
	}

	@Inject(method = "extractHotbarAndDecorations", at = @At("TAIL"))
	private void extarHotbar$renderPanels(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		Player player = getCameraPlayer();
		if (player == null || player.isSpectator()) {
			return;
		}

		int width = graphics.guiWidth();
		int height = graphics.guiHeight();

		if (Extar_hotbarClient.showActiveIndicator()) {
			String text = Extar_hotbarClient.isSecondaryRowActive()
				? Component.translatable("text.extar_hotbar.active_secondary").getString()
				: Component.translatable("text.extar_hotbar.active_primary").getString();
			int textWidth = minecraft.font.width(text);
			int x = width / 2 - (textWidth / 2) - 4;
			int y = height - Extar_hotbarClient.getActiveIndicatorYOffset();
			graphics.fill(x, y, x + textWidth + 8, y + 11, 0x90000000);
			graphics.text(minecraft.font, text, x + 4, y + 2, 0xFFFFFF, false);
		}

		if (Extar_hotbarClient.showArmorPanel()) {
			renderArmorPanel(graphics, deltaTracker, player);
		}
		if (Extar_hotbarClient.showFoodPanel()) {
			renderFoodPanel(graphics, deltaTracker, player);
		}
	}

	private void renderArmorPanel(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, Player player) {
		int x = Extar_hotbarClient.getPanelX();
		int y = Extar_hotbarClient.getPanelY();
		graphics.fill(x - 3, y - 3, x + 140, y + 72, 0x90000000);
		graphics.text(minecraft.font, Component.translatable("text.extar_hotbar.armor_panel"), x, y, 0xFFFFFF, false);

		EquipmentSlot[] slots = new EquipmentSlot[] {
			EquipmentSlot.HEAD,
			EquipmentSlot.CHEST,
			EquipmentSlot.LEGS,
			EquipmentSlot.FEET
		};

		for (int i = 0; i < slots.length; i++) {
			int rowY = y + 14 + i * 14;
			ItemStack stack = player.getItemBySlot(slots[i]);
			graphics.fill(x, rowY - 1, x + 18, rowY + 17, 0x55202020);
			extractSlot(graphics, x + 1, rowY, deltaTracker, player, stack, 1000 + i);

			if (!stack.isEmpty() && stack.isDamageableItem()) {
				int remaining = stack.getMaxDamage() - stack.getDamageValue();
				int percent = (int) ((remaining * 100.0f) / stack.getMaxDamage());
				String durabilityText = Extar_hotbarClient.showArmorDurabilityPercent()
					? remaining + " (" + percent + "%)"
					: String.valueOf(remaining);
				graphics.text(minecraft.font, durabilityText, x + 22, rowY + 4, 0xE0E0E0, false);
			} else {
				graphics.text(minecraft.font, Component.translatable("text.extar_hotbar.none"), x + 22, rowY + 4, 0xA0A0A0, false);
			}
		}
	}

	private void renderFoodPanel(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, Player player) {
		int x = Extar_hotbarClient.getPanelX() + 148;
		int y = Extar_hotbarClient.getPanelY();
		graphics.fill(x - 3, y - 3, x + 190, y + 106, 0x90000000);
		graphics.text(minecraft.font, Component.translatable("text.extar_hotbar.food_panel"), x, y, 0xFFFFFF, false);

		Set<String> shown = new LinkedHashSet<>();
		int row = 0;
		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			FoodProperties food = stack.get(DataComponents.FOOD);
			if (stack.isEmpty() || food == null) {
				continue;
			}

			String key = stack.getItem().toString();
			if (!shown.add(key)) {
				continue;
			}

			int rowY = y + 14 + row * 14;
			graphics.fill(x, rowY - 1, x + 18, rowY + 17, 0x55202020);
			extractSlot(graphics, x + 1, rowY, deltaTracker, player, stack, 2000 + row);
			String stats = "H:" + food.nutrition() + " S:" + String.format("%.1f", food.saturation());
			graphics.text(minecraft.font, stats, x + 22, rowY + 4, 0xE0E0E0, false);

			row++;
			if (row >= 6) {
				break;
			}
		}

		if (row == 0) {
			graphics.text(minecraft.font, Component.translatable("text.extar_hotbar.none"), x, y + 18, 0xA0A0A0, false);
		}
	}
}
