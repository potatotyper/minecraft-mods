# Central Waypoints

Global shared waypoints for Fabric servers on Minecraft 26.1.2.

Players can create, view, rename, and remove named waypoints that are visible to everyone. Players can also request a waypoint compass item and get live coordinate feedback in the actionbar while holding it.

## Commands

- `/waypoint add <name>`
	- Saves your current position and dimension as a global waypoint.
- `/waypoint set <name>`
	- Creates or updates a waypoint to your current position and dimension.
- `/waypoint list`
	- Lists all global waypoints.
- `/waypoint view <name>`
	- Shows full waypoint details (coordinates, dimension, creator, timestamp).
- `/waypoint remove <name>`
	- Removes a global waypoint.
- `/waypoint rename <oldName> <newName>`
	- Renames a global waypoint.
- `/waypoint compass add <name>`
	- Gives you a compass pointing to the waypoint.
	- While held, actionbar text shows target coordinates, dimension, and distance.

## Waypoint Name Rules

- 1 to 32 characters
- Allowed characters: `A-Z`, `a-z`, `0-9`, `_`, `-`
- Names are matched case-insensitively

## Storage

Waypoints are saved to:

- `config/central-waypoints.json`

## License

This project is available under the CC0 license.
