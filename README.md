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
| `/dogs tp` | Teleport your tamed dogs to you. |
| `/dogs defense [on\|off]` | Toggle defense mode or explicitly turn it on/off. Dogs target non-passive players within 12 blocks of you. |

The plugin's commands are available to all players by default through the `dogcontroller.use` permission.

## Creating a downloadable release

GitHub does not make a plugin JAR available just because the source code is pushed. This repository is configured to build and attach the JAR to a GitHub Release when you push a version tag:

```bash
git tag v1.0.0
git push origin v1.0.0
```

When the workflow finishes, the JAR will be available on the [Releases page](https://github.com/Nyxxiedev/DogController/releases/latest) with the versioned name `DogController-v1.0.0.jar`. Use a new tag for each release, such as `v1.0.1`.

## Build from source

Requirements: JDK 25 and Maven.

```bash
mvn package
```

The JAR is created under `target/`. The workflow renames it for release downloads; local Maven builds retain the Maven artifact name.
