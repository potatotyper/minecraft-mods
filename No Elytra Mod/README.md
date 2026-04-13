# no-elytra-mod
A **Fabric** mod for 26.1.* to disable the elytra in specific dimensions.

The mod makes sure that the elytra is inequippable in specific dimensions.
When switching from an enabled dimension to disabled dimension the eltyra is returned to inventory.
If inventory is full, elytra will be dropped in the new dimension.

## Commands

- `/elytra allow <dimension>` — Allow equipping Elytra in the specified dimension. Example: `/elytra allow minecraft:the_end` or `/elytra allow the_end`.
- `/elytra deny <dimension>` — Deny equipping Elytra in the specified dimension. Example: `/elytra deny minecraft:overworld`.
- `/elytra list` — Show the currently allowed dimensions for Elytra use.

Notes:
- Commands require operator-level permissions (server/operator access).
- Dimension identifiers may be provided with or without the `minecraft:` namespace; e.g. `the_end` or `minecraft:the_end`.
- Changes via commands persist to the mod config and are applied immediately to online players.

