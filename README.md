
# Minecraft Server Invite

DM me on LinkedIn to join and see my SMP Minecraft server. We make fun builds, including trying to rebuild offices we worked in before.

No mods needed to join, all you need is minecraft version 26.1.2. All the mods I have created and are being used on the server are server-side, and you can use them without installing anything!

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
