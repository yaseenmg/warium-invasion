# Contributing

Contributions are welcome: bug reports, balance ideas, translations and pull requests.

## Development setup
- JDK 17
- `./gradlew build` produces the jar in `build/libs/`
- `./gradlew runClient` / `./gradlew runServer` launch a dev environment
- Warium (`crusty_chunks`) is a runtime dependency that is **not** bundled in this repo.
  Place its jar in the `run/mods` folder to test the spawning logic.

## Guidelines
- Keep the mod ID `warium_invasion` stable. Changing it breaks existing configs and worlds.
- Do not add any Warium assets or code to this repository.
- Keep new behaviour configurable in `WariumInvasion.Settings`.
- Open an issue before large changes.

## Pull requests
1. Fork the repo and create a branch.
2. Make your change and test it in-game on Minecraft 1.20.1 / Forge 47.
3. Describe what changed and why in the PR.
