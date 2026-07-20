# CustomBossPlugin

A Spigot plugin for creating, editing, spawning, and managing custom Minecraft bosses from in-game commands.

## Features

- Create custom bosses with an ID, entity type, health value, and display name.
- Spawn bosses at your current location or at explicit world coordinates.
- Configure boss bars and action bar health displays.
- Add potion effects to bosses.
- Configure custom loot drops from held items or materials.
- Configure equipment, entity behavior flags, persistence, and dropped XP.
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
| `/boss edit <id> bossbar <true/false>` | Enable or disable boss bar display. |
| `/boss edit <id> actionbar <true/false>` | Enable or disable action bar display. |
| `/boss edit <id> potion add <type> <seconds or -1> <level> [ambient] [particles] [icon]` | Add a potion effect. Use `-1` for infinite duration. |
| `/boss edit <id> potion remove <type>` | Remove a potion effect. |
| `/boss edit <id> potion clear` | Remove all potion effects. |
| `/boss edit <id> potion list` | List potion effects. |
| `/boss edit <id> loot addhand` | Add the item in your main hand to the loot table. |
| `/boss edit <id> loot add <material> [amount]` | Add a material stack to the loot table. |
| `/boss edit <id> loot remove <index>` | Remove loot by list index. |
| `/boss edit <id> loot clear` | Clear the loot table. |
| `/boss edit <id> loot list` | List configured loot. |
| `/boss edit <id> behavior <name> <true/false>` | Set behavior flags such as glowing, silent, AI, persistence, or invulnerability. |
| `/boss edit <id> behavior list` | List behavior flags. |
| `/boss edit <id> equipment set <slot> <material> [amount]` | Set a boss equipment slot from a material. |
| `/boss edit <id> equipment sethand <slot>` | Set a boss equipment slot from the item in your main hand. |
| `/boss edit <id> equipment clear <slot/all>` | Clear one equipment slot or all equipment. |
| `/boss edit <id> equipment list` | List configured equipment. |
| `/boss edit <id> xp <amount or -1>` | Set dropped experience. Use `-1` for default entity XP. |
| `/boss enable <id> <bossbar/actionbar>` | Enable a display feature. |
| `/boss disable <id> <bossbar/actionbar>` | Disable a display feature. |
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
/boss edit undead_king behavior glowing true
/boss edit undead_king equipment sethand mainhand
/boss edit undead_king xp 50
/boss edit undead_king potion add strength -1 2
/boss edit undead_king loot add DIAMOND 3
/boss spawn undead_king
```

## Data

Boss definitions are stored in `plugins/CustomBossPlugin/bosses.yml`. The plugin saves changes automatically after edits, and `/boss reload` reloads that file while the server is running.

## API Usage From Another Plugin

Other plugins can create their own boss classes by extending `BaseBoss`, then register those classes through `CustomBossPlugin`.

Add the dependency to the other plugin's `plugin.yml`:

```yml
depend:
  - CustomBossPlugin
```

Create a boss class in the other plugin:

```java
import ch.framedev.customBossPluginMaven.BaseBoss;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ExternalBoss extends BaseBoss {

    public ExternalBoss(JavaPlugin plugin) {
        super(
                plugin,
                "&4External Boss",
                "external_boss",
                200.0,
                EntityType.ZOMBIE
        );

        setBossBar(true);
        setActionBar(true);
        setGlowing(true);
        setDroppedExperience(50);
        setMainHand(new ItemStack(Material.IRON_SWORD));
        setHelmet(new ItemStack(Material.GOLDEN_HELMET));
        addLoot(new ItemStack(Material.DIAMOND, 3));
        addPotionEffect(
                new PotionEffect(
                        PotionEffectType.STRENGTH,
                        20 * 60,
                        1
                )
        );
    }

    @Override
    public void onSpawn(LivingEntity entity) {
        entity.getWorld().strikeLightningEffect(entity.getLocation());
    }

    @Override
    public void onTick(LivingEntity entity) {
        // Runs while the boss is tracked by CustomBossPlugin.
    }

    @Override
    public void onDeath(EntityDeathEvent event) {
        event.getEntity().getWorld().createExplosion(
                event.getEntity().getLocation(),
                0.0F,
                false,
                false
        );
    }
}
```

Register the boss from the other plugin's `onEnable`:

```java
import ch.framedev.customBossPluginMaven.CustomBossPlugin;
import org.bukkit.plugin.java.JavaPlugin;

CustomBossPlugin customBossPlugin = JavaPlugin.getPlugin(CustomBossPlugin.class);

customBossPlugin.registerBoss(new ExternalBoss(this));
```

Registered bosses can also be spawned or removed through the plugin API:

```java
ExternalBoss boss = new ExternalBoss(this);

customBossPlugin.registerBoss(boss);
customBossPlugin.spawnBoss(boss, location);
customBossPlugin.unregisterBoss(boss);
```

External boss IDs are registered with the owning plugin namespace, for example `OtherPlugin:external_boss` becomes `otherplugin:external_boss`. That keeps bosses from different plugins from overwriting each other.

`BaseBoss` supports the same core options as command-created bosses, plus API-only extension points:

| Feature | API |
| --- | --- |
| Display name | `setDisplayName(...)` or `setName(...)` |
| Health | `setHealth(...)` |
| Entity type | `setEntityType(...)` |
| Boss bar | `setBossBar(...)` |
| Action bar | `setActionBar(...)` |
| Potion effects | `addPotionEffect(...)`, `removePotionEffect(...)`, `clearPotionEffects(...)` |
| Loot drops | `addLoot(...)`, `removeLoot(...)`, `clearLootTable(...)` |
| Equipment | `setHelmet(...)`, `setChestplate(...)`, `setLeggings(...)`, `setBoots(...)`, `setMainHand(...)`, `setOffHand(...)` |
| Entity behavior | `setGlowing(...)`, `setInvulnerable(...)`, `setSilent(...)`, `setAi(...)`, `setCanPickupItems(...)` |
| Persistence | `setPersistent(...)`, `setRemoveWhenFarAway(...)` |
| Death XP | `setDroppedExperience(...)` |
| Lifecycle hooks | `onSpawn(...)`, `onTick(...)`, `onDeath(...)` |
