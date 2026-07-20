package ch.framedev.customBossPluginMaven;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class CustomBossPlugin extends JavaPlugin {

    private BossManager bossManager;

    @Override
    public void onEnable() {
        bossManager = new BossManager(this);
        bossManager.initialize();

        PluginCommand bossCommand = getCommand("boss");

        if (bossCommand == null) {
            getLogger().severe(
                    "The boss command is missing from plugin.yml."
            );

            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        BossCommand commandExecutor = new BossCommand(bossManager);

        bossCommand.setExecutor(commandExecutor);
        bossCommand.setTabCompleter(commandExecutor);

        getLogger().info("CustomBossPlugin has been enabled.");
    }

    @Override
    public void onDisable() {
        if (bossManager != null) {
            bossManager.shutdown();
            bossManager.saveBosses();
        }

        getLogger().info("CustomBossPlugin has been disabled.");
    }

    public BossManager getBossManager() {
        return bossManager;
    }
}
