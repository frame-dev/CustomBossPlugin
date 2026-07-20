# CustomBossPlugin

A Spigot plugin for creating, editing, spawning, and managing custom Minecraft bosses from in-game commands.

## Features

- Create custom bosses with an ID, entity type, health value, and display name.
- Spawn bosses at your current location or at explicit world coordinates.
- Configure boss bars and action bar health displays.
- Add potion effects to bosses.
- Configure custom loot drops from held items or materials.
- Save boss definitions to `plugins/CustomBossPlugin/bosses.yml`.
- Reload boss definitions without restarting the server.

## Requirements

- Java 21
- Maven
- A Spigot-compatible Minecraft server using API version `1.20` or newer

## Build

```sh
mvn clean package
```

The built plugin jar is created in `target/`.

## Installation

1. Build the plugin.
2. Copy the generated jar from `target/` into your server's `plugins/` folder.
3. Restart the server.
4. Use `/boss` in-game to view available commands.

## Permission

| Permission | Default | Description |
| --- | --- | --- |
| `customboss.admin` | `op` | Allows access to all custom boss commands. |

## Commands

The main command is `/boss`. Aliases are `/customboss` and `/bosses`.

| Command | Description |
| --- | --- |
| `/boss create <id> <entity> <health> <name>` | Create a new boss definition. |
| `/boss edit <id> name <name>` | Change a boss display name. |
| `/boss edit <id> health <amount>` | Change a boss health value. |
| `/boss edit <id> type <entity>` | Change a boss entity type. |
| `/boss edit <id> bossbar <true|false>` | Enable or disable boss bar display. |
| `/boss edit <id> actionbar <true|false>` | Enable or disable action bar display. |
| `/boss edit <id> potion add <type> <seconds|-1> <level> [ambient] [particles] [icon]` | Add a potion effect. Use `-1` for infinite duration. |
| `/boss edit <id> potion remove <type>` | Remove a potion effect. |
| `/boss edit <id> potion clear` | Remove all potion effects. |
| `/boss edit <id> potion list` | List potion effects. |
| `/boss edit <id> loot addhand` | Add the item in your main hand to the loot table. |
| `/boss edit <id> loot add <material> [amount]` | Add a material stack to the loot table. |
| `/boss edit <id> loot remove <index>` | Remove loot by list index. |
| `/boss edit <id> loot clear` | Clear the loot table. |
| `/boss edit <id> loot list` | List configured loot. |
| `/boss enable <id> <bossbar|actionbar>` | Enable a display feature. |
| `/boss disable <id> <bossbar|actionbar>` | Disable a display feature. |
| `/boss spawn <id>` | Spawn a boss at your location. |
| `/boss spawnat <id> <world> <x> <y> <z>` | Spawn a boss at specific coordinates. Supports `~` relative coordinates. |
| `/boss delete <id>` | Delete a boss definition. |
| `/boss list` | List all boss definitions. |
| `/boss info <id>` | Show details for a boss. |
| `/boss save` | Save boss definitions. |
| `/boss reload` | Reload boss definitions from disk. |

## Examples

```text
/boss create undead_king ZOMBIE 200 &4Undead King
/boss edit undead_king bossbar true
/boss edit undead_king actionbar true
/boss edit undead_king potion add strength -1 2
/boss edit undead_king loot add DIAMOND 3
/boss spawn undead_king
```

## Data

Boss definitions are stored in `plugins/CustomBossPlugin/bosses.yml`. The plugin saves changes automatically after edits, and `/boss reload` reloads that file while the server is running.
