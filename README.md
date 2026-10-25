# Warium: Machine Uprising

[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
![Minecraft 1.20.1](https://img.shields.io/badge/Minecraft-1.20.1-blue)
![Forge 47+](https://img.shields.io/badge/Forge-47%2B-orange)

**Warium's machines fight back.** An addon for Warium (`crusty_chunks`) that sends rare roaming robots, air raids and full-scale invasion waves after the player.

It adds no new entities or assets. It spawns Warium's own units on a schedule and aims them at you.

## Requirements
- Minecraft **1.20.1**
- Forge **47+**
- Warium (`crusty_chunks`) **1.3.5+**

## Features

### Rare roaming machines
Every 3 minutes there is a chance (8% by default) that a single Warium ground robot spawns 80–112 blocks from you, day or night. Possible units: Scout, Rifler, Striker, Breacher, Assassin, Reaper, Mortarer, Worker, Commander.

### Periodic air raids
Every 20 in-game days a Raid Scout circles above you, carries out 3 bombing runs near your position, then leaves after 110 seconds. No chat warnings.

### Major invasions
The first invasion starts on day 150 and repeats every 30 days. Each one sends 80 units in waves over about 40 seconds, preceded by a 10-strike bombardment.

| Unit | Count |
|---|---|
| Ground robots (random mix) | 30 |
| Tanks (Decimator / Eradicator / Prototype hulls) | 10 |
| Raid Scouts | 15 |
| Hunters (drone-style flyers) | 25 |

### More Warium structures
Extra worldgen placement makes Desert Array, Overgrown Array, Oil Well and Sea Base more common in **newly generated chunks**.

### Per-player schedules
Each player has an independent schedule. Nothing runs on Peaceful difficulty.

## Configuration
`config/warium_invasion-common.toml` is created on first launch.

| Option | Default | Range |
|---|---|---|
| `enabled` | `true` | |
| `airRaidIntervalDays` | `20` | 1–365 |
| `firstMajorInvasionDays` | `150` | 1–1000 |
| `majorInvasionIntervalDays` | `30` | 1–365 |
| `aircraftStaySeconds` | `110` | 30–600 |
| `maxActiveAttackersNearPlayer` | `100` | 1–160 |
| `rareRoamerChancePerCheck` | `0.08` | 0.0–1.0 |
| `enableBombs` | `true` | |
| `bombStrength` | `3.0` | 1.0–8.0 |
| `bombsBreakBlocks` | `false` | |

Bombs do not break blocks by default. When `bombsBreakBlocks` is on, the `mobGriefing` game rule still applies.

## Installation
1. Install Minecraft 1.20.1 with Forge 47.x and Warium 1.3.5+.
2. Put the mod jar in your `mods` folder.
3. On multiplayer, install on the server and the clients.

The mod can be removed at any time. It does not modify Warium's files.

## Notes
- Warium's static base defenses are unchanged. Only the mobile units spawned by this mod hunt the player.
- Warium has no separate spider, helicopter or drone entities. **Hunter** is used as the drone-style unit and **Raid Scout** as the aircraft.
- New structure placement does not affect chunks that were already generated.
- Locate structures with `/locate structure crusty_chunks:desert_array` (also `overgrown_array`, `oil_well`, `sea_base`).
- If a world is already past day 20 / day 150 when a player first joins with the mod, the first air raid and invasion start immediately.

## Building from source
```
./gradlew build
```
Requires JDK 17. The jar is written to `build/libs/`.

## Contributing
See [CONTRIBUTING.md](CONTRIBUTING.md).

## License
[MIT](LICENSE). This is an unofficial addon and is not affiliated with the Warium developers. Warium is not included in this repository.
