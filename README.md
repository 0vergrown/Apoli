# Apoli

**Apoli** is a data-pack-driven power system for Minecraft. It gives pack authors the tools to create custom abilities, status effects, game mechanics, movement, combat, rendering, and world interactions, all through JSON. No Java required. Apoli powers [Overgrown's Origins](https://github.com/0vergrown/Origins).

## Branches

Each Minecraft version and mod loader combination has its own branch. All branches share the same data pack format and are kept in feature parity.

| Branch                                                                       | Loader   | Minecraft |
|------------------------------------------------------------------------------|----------|-----------|
| [`Fabric-1.20.1`](https://github.com/0vergrown/Apoli/tree/Fabric-1.20.1)     | Fabric   | 1.20.1    |
| [`Fabric-1.21.1`](https://github.com/0vergrown/Apoli/tree/Fabric-1.21.1)     | Fabric   | 1.21.1    |
| [`NeoForge-1.21.1`](https://github.com/0vergrown/Apoli/tree/NeoForge-1.21.1) | NeoForge | 1.21.1    |

## Features

### Power system

- Over 100 built-in power types covering movement, combat, resource and attribute manipulation, rendering and HUD elements, world interaction, and utility behavior.
- A typed action system covering entities, bi-entities, blocks, items, and meta actions, alongside conditions for seven different context types.
- Every power supports a universal `condition` field, condition inversion, and source-based suppression. This lets you temporarily disable a granted power without removing it.

### Additional systems

- **Data-driven skill trees:** Create progression paths and unlockable abilities entirely through data packs.
- **Physics-based ropes and grappling:** Swing, leap, and reel in while anchored to entities or blocks.
- **Entity disguises:** Transform mobs and players, with full multiplayer synchronization.
- **Custom entities:** Create projectiles, minions, and player clones that can be granted through powers.
- **Data-driven keybindings:** Assign keys to active powers using JSON.
- Custom recipes and loot functions that grant powers.
- A built-in expression engine for formula-based values, including resource caps, attribute modifiers, and damage scaling.

### Server performance and networking

- Powers are indexed per entity when granted or revoked. Tick, collision, and damage checks use the cached index for O(1) lookups instead of scanning every power.
- Power synchronization is chunked and compressed, allowing large data packs to sync without hitting Minecraft's packet string limit.
- A versioned network protocol checks client/server compatibility when players join. Mismatched builds produce a readable error message instead of silently desynchronizing.
- JSON handling remains backward-compatible across versions. Renamed or restructured fields still accept their older formats, so existing data packs continue to work after updates.

### Compatibility

- Optional integration with Trinkets, Curios, and Accessories. A shared facade handles accessory slots, allowing packs to use whichever of these mods their modpack includes.
- Optional integration with Figura, Icarus, and Hardcore Revival. Apoli compiles against each mod's API and safely does nothing when the mod isn't installed.

## For pack authors

Powers are defined as JSON files under `data/<namespace>/powers/<power>.json` in a data pack. You can grant them directly to entities or through an [Overgrown's Origins](https://github.com/0vergrown/Origins) origin.

Here's a minimal example of a power that applies Speed when used:

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
````

### For developers

Apoli requires JDK 17.

Build the project with:

```
./gradlew build
```

To launch a development environment, use `./gradlew runClient` or `./gradlew runServer`. For IDE setup, see the [Fabric documentation](https://docs.fabricmc.net).

### License

Apoli is licensed under the A/O License. See `LICENSE` for the repository copy, or visit the Handbook for the [always-current license text](https://0vergrown.github.io/Handbook/license).