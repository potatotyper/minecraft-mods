# Central Waypoints

Global shared waypoints for Fabric servers on Minecraft 26.1.2.

Players can create, view, rename, and remove named waypoints that are visible to everyone. Players can also request a waypoint compass item and get live coordinate feedback in the actionbar while holding it.

## Commands

- `/cwaypoint add <name>`
	- Saves your current position and dimension as a global waypoint.
- `/cwaypoint set <name> <x> <y> <z>`
	- Creates or updates a waypoint to the specified coordinates in your current dimension.
- `/cwaypoint list`
	- Lists all global waypoints.
- `/cwaypoint view <name>`
	- Shows full waypoint details (coordinates, dimension, creator, timestamp).
- `/cwaypoint remove <name>`
	- Removes a global waypoint.
- `/cwaypoint rename <oldName> <newName>`
	- Renames a global waypoint.
- `/cwaypoint compass get <name>`
	- Gives you a compass pointing to the waypoint.
	- Compass name includes the waypoint name and coordinates.
	- While held, actionbar text shows target coordinates, dimension, and distance.
- `/cwaypoint compass remove`
	- Removes all waypoint compasses from your inventory (hotbar, main inventory, offhand).

## Waypoint Name Rules

- 1 to 32 characters
- Allowed characters: `A-Z`, `a-z`, `0-9`, `_`, `-`
- Names are matched case-insensitively

## Storage

Waypoints are saved to:

- `config/central-waypoints.json`

## License

This project is available under the CC0 license.
