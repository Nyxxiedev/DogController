package com.DogController.project;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.PluginCommand;
import com.DogController.project.managers.PluginManager;
import com.DogController.project.managers.DogControlManager;
import com.DogController.project.commands.DogCommand;
import com.DogController.project.listeners.PlayerListener;

public class DogController extends JavaPlugin {
    
    @Override
    public void onEnable() {
        
        // Initialize managers
        PluginManager.getInstance().initialize();

        DogControlManager dogControlManager = new DogControlManager();
        getServer().getPluginManager().registerEvents(new PlayerListener(dogControlManager), this);

        DogCommand dogCommand = new DogCommand(dogControlManager);
        PluginCommand dogsCommand = getCommand("dogs");
        if (dogsCommand == null) {
            throw new IllegalStateException("The dogs command is missing from plugin.yml");
        }
        dogsCommand.setExecutor(dogCommand);
        dogsCommand.setTabCompleter(dogCommand);

        getServer().getScheduler().runTaskTimer(this, dogControlManager::updateTargets, 1L, 10L);

        getLogger().info(getDescription().getName() + " has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info(getDescription().getName() + " has been disabled!");
    }
    
}
