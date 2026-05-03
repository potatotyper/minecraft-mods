# Server Auto Click

A Fabric server mod for Minecraft 26.1.2 that lets players use timed auto-attack and auto-consume commands without installing the mod client-side.

## Features

- Server-side commands, so players do not need the mod installed on their client.
- `/autoattack` attacks at a configurable interval for a configured duration and shows an action-bar ON indicator while active.
- `/autoconsume` starts consuming held food when possible instead of repeatedly right-clicking every tick.

## Usage

Use these server commands in-game:

```
/autoattack on <interval> <duration>
/autoattack on <interval> infinite
/autoconsume on <duration>
/autoconsume on infinite
```

- `<interval>`: Attack interval in seconds (0.5 to 60).
- `<duration>`: How long to run in seconds (0.5 to 600), or use `infinite`.
- The old forms without `on` still work: `/autoattack <interval> <duration>` and `/autoconsume <duration>`.
- Auto consume checks food in your main hand first, then your off hand.

Optional stop commands:

```
/autoattack off
/autoconsume off
```

**Examples:**
- `/autoattack on 1 15` - Attacks once per second for 15 seconds.
- `/autoattack on 0.5 infinite` - Attacks every 0.5 seconds until turned off.
- `/autoconsume on 8` - Consumes held food for 8 seconds when hunger allows.
- `/autoconsume on infinite` - Keeps consuming held food until turned off.
