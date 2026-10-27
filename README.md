# DogController

DogController is a Minecraft plugin for giving your tamed wolves player-specific orders and protecting you from nearby players.

## Download

Download the latest release JAR from the [GitHub Releases page](https://github.com/Nyxxiedev/DogController/releases/latest). Do not download the source code ZIP files; the server needs the `.jar` plugin file.

If there are no releases yet, see [Creating a downloadable release](#creating-a-downloadable-release).

## Install

1. Download the DogController `.jar` file from a release.
2. Stop your Spigot-compatible Minecraft server.
3. Put the JAR in the server's `plugins` folder.
4. Start the server. Check `/plugins` to confirm DogController loaded.

## Compatibility

One DogController JAR supports Minecraft 1.8.8 through 26.3, on Spigot-compatible
servers (Spigot, Paper, and forks). Hex colours in config/messages render on
1.16+ and fall back to the nearest legacy colour on older servers.

Supported versions are listed in [`compat/versions.json`](compat/versions.json).
Every release is compiled against each supported Spigot API and boot-tested on
Paper for that version, including a test-only probe that checks the potion,
colour, dog-cache, and tracking-file compatibility paths.

| Era | Minecraft versions | Java runtime |
| --- | --- | --- |
| Legacy | 1.8.8, 1.9.4, 1.10.2, 1.11.2, 1.12.2 | 8 |
| Flattening | 1.13.2, 1.14.4, 1.15.2, 1.16.5 | 11 |
| Caves & modern | 1.17.1, 1.18.2, 1.19.4, 1.20.4 | 17 |
| Current | 1.20.6, 1.21.11 | 21 |
| Latest | 26.3 | 25 |

## Commands

| Command | Description |
| --- | --- |
| `/dogs attack <player>` | Order your dogs to attack a player. `agro` and `aggro` are accepted aliases. |
| `/dogs passive <player>` | Make your dogs ignore a player. |
| `/dogs neutral <player>` | Clear your explicit order for a player. |
| `/dogs tp` | Teleport your tamed dogs into a compact, spaced formation near you. They receive 3 seconds of hidden Resistance VI after teleporting. Summons have a 10-second per-player cooldown. |
| `/dogs sit` | Make your tamed dogs sit and pause attack orders. |
| `/dogs stand` | Make your tamed dogs stand again. |
| `/dogs defense [on\|off]` | Toggle defense mode or explicitly turn it on/off. Dogs target non-passive players within 12 blocks of you. |

The plugin's commands are available to all players by default through the `dogcontroller.use` permission.

Dog locations are tracked when their chunks load and saved in the plugin data folder. Dog commands process at most one source chunk per server tick across all queued requests and do not generate missing chunks. Dogs that were already in chunks when this tracking feature was added are discovered as their chunks load.

## Build from source

Requirements: JDK 17 or newer and Maven. The plugin is compiled for Java 8 so it
runs on legacy servers.

```bash
mvn package
```

The JAR is created under `target/`. The workflow renames it for release downloads; local Maven builds retain the Maven artifact name.

To verify every supported version locally, with Docker installed:

```bash
python3 scripts/verify_versions.py            # compile + boot matrices
python3 scripts/verify_versions.py --compile-only
python3 scripts/verify_versions.py --boot-only --versions 1.8.8,26.3
```

Paper builds are cached under `~/.cache/dogcontroller/paper`; server test
directories default to `/tmp/opencode/dogcontroller-compat`.
