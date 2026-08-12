# Apoli

**Apoli** is a data-pack-driven power system for Minecraft. It lets pack authors build custom abilities, status effects, and game mechanics — movement, combat, rendering, world interaction, and more — entirely through JSON. No Java required. It's the engine behind [Overgrown's Origins](https://github.com/0vergrown/Origins).

> **This branch:** `NeoForge-1.21.1` — NeoForge, Minecraft 1.21.1.

## Branches

This repository holds one branch per Minecraft version / mod loader combination, all kept at feature parity and sharing the same data pack format:

| Branch                                                                       | Loader   | Minecraft |
|------------------------------------------------------------------------------|----------|-----------|
| [`Fabric-1.20.1`](https://github.com/0vergrown/Apoli/tree/Fabric-1.20.1)     | Fabric   | 1.20.1    |
| [`Fabric-1.21.1`](https://github.com/0vergrown/Apoli/tree/Fabric-1.21.1)     | Fabric   | 1.21.1    |
| [`NeoForge-1.21.1`](https://github.com/0vergrown/Apoli/tree/NeoForge-1.21.1) | NeoForge | 1.21.1    |

## Features

### Power system
- 100+ built-in power types spanning movement, combat, resource & attribute manipulation, rendering/HUD, world interaction, and meta/utility behavior.
- A typed action system (entity, bi-entity, block, item, meta) and a condition system spanning 7 context types.
- Every power supports a universal `condition` field, condition inversion, and source-based suppression — temporarily disable a granted power without removing it.

### Beyond the basics
- **Data-driven skill trees** — progression paths and unlockable abilities, defined entirely in a data pack.
- **Physics-based rope & grapple system** — swing, leap, and reel, anchored to entities or blocks.
- **Entity disguise system** — transform mobs and players, fully synced in multiplayer.
- **Custom entities** — projectiles, minions, and player clones, grantable via powers.
- **Data-driven keybindings** — bind an active power to a key entirely from JSON.
- Custom recipes and loot functions that grant powers.
- A built-in expression engine for math-driven values — resource caps, attribute modifiers, damage scaling, and more, written as formulas instead of fixed numbers.

### Built for real servers
- Powers are indexed per-entity at grant/revoke time, so hot paths (tick, collision, damage) do O(1) lookups against a cached index instead of scanning every power on every check.
- Power sync to clients is chunked and compressed, so large data packs never hit Minecraft's packet string limit.
- A versioned network protocol checks client/server compatibility on join, so a mismatched build fails with a readable message instead of a silent desync.
- Backward-compatible JSON handling — fields renamed or restructured across versions still parse from their older form, so existing data packs keep working after an update.

### Compatibility
- Optional integration with Trinkets, Curios, and Accessories, unified behind one facade — use whichever accessory-slot mod the pack's modpack already has.
- Optional integration with Figura, Icarus, and Hardcore Revival. Each compiles against the mod's API but no-ops cleanly if the mod isn't installed.

## For pack authors

Powers live under `data/<namespace>/powers/<power>.json` in a data pack and get granted to entities directly, or through an [Overgrown's Origins](https://github.com/0vergrown/Origins) origin. A minimal power looks like:

```json
{
  "type": "apoli:action_on_use",
  "action": {
    "type": "apoli:effect",
    "effect": "minecraft:speed",
    "duration": 200,
    "amplifier": 1
  }
}
```

## For developers

Requires JDK 21.

```
./gradlew build
```

Use `./gradlew runClient` / `./gradlew runServer` for a dev environment. See the [NeoForge documentation](https://docs.neoforged.net/) for IDE setup.

## License

The A/O License — see [`LICENSE`](LICENSE). The canonical, always-current text lives on the Handbook: <https://0vergrown.github.io/Handbook/license>.
