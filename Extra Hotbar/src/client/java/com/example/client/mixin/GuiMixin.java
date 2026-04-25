package com.example.client.mixin;

import com.example.client.Extar_hotbarClient;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
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

	private boolean extarHotbar$poseShifted = false;

	@Inject(method = "extractHotbarAndDecorations", at = @At("HEAD"))
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
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, hotbarX, hotbarY, 182, 22);

		int seed = 1;
		int rowBase = Extar_hotbarClient.getSecondaryInventorySourceBase();
		for (int slot = 0; slot < 9; slot++) {
			int slotX = hotbarX + 3 + slot * 20;
			int slotY = hotbarY + 3;
			ItemStack stack = player.getInventory().getItem(rowBase + slot);

			extractSlot(graphics, slotX, slotY, deltaTracker, player, stack, seed++);
			if (Extar_hotbarClient.isSlotLocked(slot)) {
				graphics.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, 0x55AA3333);
			}
			graphics.text(minecraft.font, String.valueOf(slot + 1), slotX + 1, slotY - 8, 0xFFFFFF, false);
		}
	}

	@Inject(
		method = "extractHotbarAndDecorations",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/Gui;extractItemHotbar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
			shift = At.Shift.AFTER
		)
	)
	private void extarHotbar$shiftHudUp(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		if (Extar_hotbarClient.showSecondHotbar()) {
			int shift = Math.max(0, Extar_hotbarClient.getSecondHotbarYOffset());
			graphics.pose().translate(0.0f, (float) -shift);
			this.extarHotbar$poseShifted = true;
		}
	}

	@Inject(method = "extractHotbarAndDecorations", at = @At("TAIL"))
	private void extarHotbar$renderPanels(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		Player player = getCameraPlayer();
		if (player == null || player.isSpectator()) {
			return;
		}

		// Pop pose translation if it was pushed so panels render at absolute positions
		if (this.extarHotbar$poseShifted) {
			int shift = Math.max(0, Extar_hotbarClient.getSecondHotbarYOffset());
			graphics.pose().translate(0.0f, (float) shift);
			this.extarHotbar$poseShifted = false;
		}

		int width = graphics.guiWidth();
		int height = graphics.guiHeight();

		if (Extar_hotbarClient.showActiveIndicator()) {
			String text = Extar_hotbarClient.isSecondaryRowActive()
				? Component.translatable("text.extar_hotbar.active_secondary").getString()
				: Component.translatable("text.extar_hotbar.active_primary").getString();
			int textWidth = minecraft.font.width(text);
			int x = width / 2 - (textWidth / 2);
			int y = height - Extar_hotbarClient.getActiveIndicatorYOffset();
			graphics.text(minecraft.font, text, x, y, 0xFFFFFF, true);
		}

		// Position panels at exact bottom left
		int baseX = 2;
		int baseY = height - 64;
		if (Extar_hotbarClient.showArmorPanel()) {
			renderArmorPanel(graphics, deltaTracker, player, baseX, baseY);
		}
		if (Extar_hotbarClient.showFoodPanel()) {
			renderFoodPanel(graphics, deltaTracker, player, baseX + 18, baseY);
		}
	}

	private void renderArmorPanel(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, Player player, int x, int y) {
		EquipmentSlot[] slots = new EquipmentSlot[] {
			EquipmentSlot.HEAD,
			EquipmentSlot.CHEST,
			EquipmentSlot.LEGS,
			EquipmentSlot.FEET
		};

		// Subtle HUD background
		graphics.fill(x - 1, y - 1, x + 17, y + 64, 0x44000000);

		for (int i = 0; i < slots.length; i++) {
			int rowY = y + i * 16;
			ItemStack stack = player.getItemBySlot(slots[i]);
			extractSlot(graphics, x, rowY, deltaTracker, player, stack, 1000 + i);
		}
	}

	private void renderFoodPanel(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, Player player, int x, int y) {
		List<ItemStack> foodStacks = getTopFoodStacks(player, 4);

		// Subtle HUD background
		graphics.fill(x - 1, y - 1, x + 17, y + 64, 0x44000000);

		for (int i = 0; i < 4; i++) {
			int rowY = y + i * 16;
			if (i < foodStacks.size()) {
				extractSlot(graphics, x, rowY, deltaTracker, player, foodStacks.get(i), 2000 + i);
			}
		}
	}

	private List<ItemStack> getTopFoodStacks(Player player, int maxItems) {
		Map<String, ItemStack> bestByItem = new HashMap<>();
		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			FoodProperties food = stack.get(DataComponents.FOOD);
			if (stack.isEmpty() || food == null) {
				continue;
			}

			String key = stack.getItem().toString();
			ItemStack existing = bestByItem.get(key);
			if (existing == null || stack.getCount() > existing.getCount()) {
				bestByItem.put(key, stack);
			}
		}

		List<ItemStack> foods = new ArrayList<>(bestByItem.values());
		foods.sort(
			Comparator
				.comparingInt(ItemStack::getCount)
				.reversed()
				.thenComparingInt(stack -> {
					FoodProperties food = stack.get(DataComponents.FOOD);
					return food == null ? 0 : food.nutrition();
				})
				.reversed()
		);

		if (foods.size() > maxItems) {
			return new ArrayList<>(foods.subList(0, maxItems));
		}
		return foods;
	}
}
