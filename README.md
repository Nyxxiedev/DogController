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

Requirements: JDK 25 and Maven.

```bash
mvn package
```

The JAR is created under `target/`. The workflow renames it for release downloads; local Maven builds retain the Maven artifact name.
