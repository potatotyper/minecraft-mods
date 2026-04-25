
# Minecraft Server Invite

DM me on LinkedIn to join and see my SMP Minecraft server. We make fun builds, including trying to rebuild offices we worked in before.

No mods needed to join, all you need is minecraft version 26.1.2. The server-side mods I have created can be used without installing anything. This repository also includes optional client-side utility mods you can install locally if you want some extra features.

---

# Mods in this repository

### Central Waypoints

**Description:**
Global shared waypoints for Fabric servers. Players can create, view, rename, and remove named waypoints visible to everyone. Players can also request a waypoint compass item and get live coordinate feedback in the actionbar while holding it.

**How to Use:**
All commands are available in-game. Waypoints are global and visible to all players.

**Commands:**
- `/cwaypoint add <name>`
	- Save your current position and dimension as a global waypoint.
- `/cwaypoint set <name> <x> <y> <z>`
	- Create or update a waypoint at the specified coordinates in your current dimension.
- `/cwaypoint list`
	- List all global waypoints.
- `/cwaypoint view <name>`
	- Show full waypoint details (coordinates, dimension, creator, timestamp).
- `/cwaypoint remove <name>`
	- Remove a global waypoint.
- `/cwaypoint rename <oldName> <newName>`
	- Rename a global waypoint.
- `/cwaypoint compass get <name>`
	- Gives you a compass pointing to the waypoint. While held, the actionbar shows target info.
- `/cwaypoint compass remove`
	- Removes all waypoint compasses from your inventory.

**Waypoint Name Rules:**
- 1 to 32 characters
- Allowed: `A-Z`, `a-z`, `0-9`, `_`, `-`
- Case-insensitive

**Storage:**
Waypoints are saved to `config/central-waypoints.json`.

---

### Extra Hotbar

**Description:**
A Fabric client mod that adds a second hotbar display, hotbar row swapping, slot locks, and compact HUD panels for armor and food inventory info.

**How to Use:**
Install this locally on the client. It is optional and is not required to join the server.

**Default Keybinds:**
- `R`
	- Swap between the visible hotbar and the configured secondary inventory row.
- `H`
	- Cycle HUD panels through Off, Armor, Food, and Both.
- `K`
	- Lock or unlock the currently selected hotbar slot.
- Double-tap `1`-`9`
	- Swap only that matching hotbar slot with the secondary row.

**Notes:**
- Full-row swaps skip locked slots.
- The secondary row can be configured as inventory row `1`-`3`.
- Settings are saved to `config/extar_hotbar.json`.

---

### No Elytra Mod

**Description:**
A Fabric mod to disable Elytra usage in specific dimensions. Elytra is automatically unequipped or dropped when entering a restricted dimension.

**How to Use:**
Commands require operator permissions. Dimension names can be given with or without the `minecraft:` namespace.

**Commands:**
- `/elytra allow <dimension>`
	- Allow equipping Elytra in the specified dimension.
		- Example: `/elytra allow minecraft:the_end` or `/elytra allow the_end`
- `/elytra deny <dimension>`
	- Deny equipping Elytra in the specified dimension.
		- Example: `/elytra deny minecraft:overworld`
- `/elytra list`
	- Show currently allowed dimensions for Elytra use.

**Notes:**
- Changes are saved and applied immediately to online players.

---

### Server Auto Click

**Description:**
A Fabric server mod that lets players use timed auto-attack and auto-consume commands without needing the mod client-side.

**How to Use:**
Players use the following commands in-game:

**Commands:**
- `/autoattack <interval> <duration>`
	- Attack every `<interval>` seconds for `<duration>` seconds.
- `/autoattack <interval> infinite`
	- Attack every `<interval>` seconds until turned off.
- `/autoconsume <duration>`
	- Use held item repeatedly for `<duration>` seconds.
- `/autoconsume infinite`
	- Use held item until turned off.
- `/autoattack off`
	- Stop auto-attacking.
- `/autoconsume off`
	- Stop auto-consuming.

**Parameters:**
- `<interval>`: Attack interval in seconds (0.5 to 60).
- `<duration>`: Duration in seconds (0.5 to 600), or `infinite`.

**Examples:**
- `/autoattack 1 15` — Attacks once per second for 15 seconds.
- `/autoattack 0.5 infinite` — Attacks every 0.5 seconds until stopped.
- `/autoconsume 8` — Uses held item for 8 seconds.
- `/autoconsume infinite` — Keeps using the held item until stopped.

---

## Links

- https://modrinth.com/mod/disable-elytra-fabric
- https://modrinth.com/mod/server-auto-click
- https://modrinth.com/mod/central-waypoints
- https://modrinth.com/mod/extra-hotbar
