# Server Auto Click

A Fabric client mod for Minecraft 26.1.2 that allows players to automatically click at a set interval using in-game commands.

## Features

- Trigger automatic clicking with customizable intervals.
- Choose between `left` and `right` click actions.
- Supported intervals range from 0.5 seconds up to 60 seconds.

## Usage

Use the `/autoclick` client command in-game to start or configure the auto-clicker:

```
/autoclick <interval> <button>
```

- `<interval>`: Time between clicks in seconds (must be between 0.5 and 60).
- `<button>`: The mouse button to click (`left` or `right`).

**Examples:**
- `/autoclick 1 left` - Auto-clicks the left mouse button every 1 second.
- `/autoclick 0.5 right` - Auto-clicks the right mouse button every half second.

## Setup & Building

For setup instructions, please see the [Fabric Documentation page](https://docs.fabricmc.net/) related to your IDE.

## License

This project is available under the CC0 license. Feel free to learn from it and incorporate it into your own projects.
