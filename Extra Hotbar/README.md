# Extra Hotbar

Fabric client mod that adds dual-hotbar swapping utilities and compact HUD info overlays.

## Features

- Dual hotbar swap system:
	- Swaps between your visible hotbar and a configurable secondary inventory row.
	- Supports full-row swap and single-slot swap logic.
- Hold and tap swap behavior:
	- Tap or hold the swap key for different actions (configurable).
- Per-slot double-tap swap:
	- Double-tap number keys `1`-`9` to swap only that slot.
- Slot lock support:
	- Lock specific hotbar slots to prevent them from being swapped.
- Active bar indicator:
	- Shows whether primary or secondary bar is currently active.
- Panel cycle mode:
	- Cycle HUD info display through: `Off -> Armor -> Food -> Both`.
- Armor panel info:
	- Shows equipped armor pieces and remaining durability.
- Food panel info:
	- Lists edible items currently in inventory with hunger and saturation.
- Persistent client config:
	- Settings are saved and reloaded from JSON config.

## Controls (Default Keybinds)

- `R`: Swap hotbars.
- `H`: Cycle HUD panels (`Off -> Armor -> Food -> Both`).
- `K`: Toggle lock on the currently selected hotbar slot.
- `1`-`9` double-tap: Swap a single corresponding hotbar slot.

You can rebind these in Minecraft controls.

## In-Game Behavior

- Spectator mode: swapping logic is skipped.
- Locked slots:
	- Full-row swaps skip locked slots.
	- Single-slot swaps do not run for locked slots.

## Configuration

Config file: `config/extar_hotbar.json`

Main settings:

- `secondaryInventoryRow` (`1`-`3`)
- `showSecondHotbar`
- `showActiveIndicator`
- `secondHotbarYOffset`
- `activeIndicatorYOffset`
- `enableDoubleTapSwap`
- `doubleTapWindowMs` (`100`-`1000`)
- `enableHoldSwap`
- `holdSwapMs` (`100`-`1200`)
- `holdSwapsSingleSlot`
- `panelMode` (`0`-`3`)
	- `0 = Off`, `1 = Armor`, `2 = Food`, `3 = Both`
- `panelX`
- `panelY`
- `showArmorDurabilityPercent`
- `lockedSlots` (array of slot indices `0`-`8`)
