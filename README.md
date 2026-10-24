# Warium: Machine Uprising

[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
![Minecraft 1.20.1](https://img.shields.io/badge/Minecraft-1.20.1-blue)
![Forge 47+](https://img.shields.io/badge/Forge-47%2B-orange)

**Warium's machines fight back.** An unofficial addon for Warium (`crusty_chunks`) that sends roaming robots, air raids, and large invasion waves after the player.

It adds no new entities or assets. It uses Warium's own units and targets nearby players.

## Requirements

- Minecraft Java Edition **1.20.1**
- Minecraft Forge **47.x**
- Warium (`crusty_chunks`) **1.3.5+**
- JDK **17** to build from source

## Features

### Rare roaming machines
Every three minutes, an 8% chance (by default) is checked for a Warium ground robot to spawn 80–112 blocks away. Possible units include Scout, Rifler, Striker, Breacher, Assassin, Reaper, Mortarer, Worker, and Commander.

### Periodic air raids
Every 20 in-game days, a Raid Scout circles above the player and performs three bombing runs nearby. The aircraft leaves after 110 seconds. No chat warnings are shown.

### Major invasions
The first major invasion is scheduled for day 150 and repeats every 30 days. The default wave contains 80 units:

| Unit category | Count |
|---|---:|
| Ground robots | 30 |
| Tanks | 10 |
| Raid Scouts / aircraft units | 15 |
| Hunter flyers | 25 |

The invasion is spawned in timed batches. A 10-strike bombardment can precede the wave.

### More Warium structures
Additional world-generation placements are configured for Desert Array, Overgrown Array, Oil Well, and Sea Base. These affect **newly generated chunks only**.

### Per-player schedules
Each player has an independent schedule. Invasions do not run on Peaceful difficulty.

## Configuration

On first launch, Forge creates `config/warium_invasion-common.toml`.

| Option | Default | Purpose |
|---|---:|---|
| `enabled` | `true` | Enables or disables the mod's events |
| `airRaidIntervalDays` | `20` | Days between air raids |
| `firstMajorInvasionDays` | `150` | Day of the first major invasion |
| `majorInvasionIntervalDays` | `30` | Days between major invasions |
| `aircraftStaySeconds` | `110` | How long aircraft remain |
| `maxActiveAttackersNearPlayer` | `100` | Nearby attacker limit |
| `rareRoamerChancePerCheck` | `0.08` | Chance of a rare roaming robot |
| `enableBombs` | `true` | Enables bombing runs |
| `bombStrength` | `3.0` | Explosion strength |
| `bombsBreakBlocks` | `false` | Whether bombs can damage blocks |

Bombs do not break blocks by default. When block damage is enabled, the `mobGriefing` game rule still applies.

## Installation

1. Install Minecraft 1.20.1 with Forge 47.x.
2. Install Warium 1.3.5 or newer.
3. Put the Warium: Machine Uprising JAR in your `mods` folder.
4. For multiplayer, install the mod on the server and on clients.

The mod does not modify Warium's files and can be removed normally.

## Notes

- Warium's static base defenses are unchanged; this addon targets mobile units spawned by the mod.
- The Hunter is used as the drone-style flyer and the Raid Scout as the aircraft unit.
- Added structure placement does not affect chunks that already exist.
- Use `/locate structure crusty_chunks:desert_array` (also `overgrown_array`, `oil_well`, and `sea_base`) to find structures if those IDs are registered by your installed Warium version.
- If a world is already beyond the scheduled day when a player first joins with the mod, the first event may trigger immediately.

## Building from source

Requires JDK 17.

```bash
./gradlew build
```

On Windows:

```bat
gradlew.bat build
```

The compiled JAR is written to `build/libs/`. GitHub Actions also builds the project and publishes the JAR as a workflow artifact.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Please include Minecraft, Forge, and Warium versions, logs, and steps to reproduce any bug.

## License

Licensed under MIT; see [LICENSE](LICENSE). This is an unofficial addon and is not affiliated with the Warium developers. Warium is not included in this repository.
