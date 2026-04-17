# Server Auto Click

A Fabric server mod for Minecraft 26.1.2 that lets players use timed auto-attack and auto-consume commands without installing the mod client-side.

## Features

- Server-side commands, so players do not need the mod installed on their client.
- `/autoattack` attacks at a configurable interval for a configured duration.
- `/autoconsume` repeatedly uses the held item (right-click behavior) for a configured duration.

## Usage

Use these server commands in-game:

```
/autoattack <interval> <duration>
/autoconsume <duration>
```
Duration can be infinite.

- `<interval>`: Attack interval in seconds (0.5 to 60).
- `<duration>`: How long to run in seconds (0.5 to 600).

Optional stop commands:

```
/autoattack off
/autoconsume off
```

**Examples:**
- `/autoattack 1 15` - Attacks once per second for 15 seconds.
- `/autoconsume 8` - Uses held item repeatedly for 8 seconds.
