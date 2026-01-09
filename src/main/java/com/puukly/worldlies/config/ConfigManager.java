package com.puukly.worldlies.config;

import com.worldlies.plugin.WorldLiesInterface;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final WorldLiesInterface plugin;
    private FileConfiguration config;

    public ConfigManager(WorldLiesInterface plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public String getMessage(String path, String defaultMessage) {
        return config.getString("messages." + path, defaultMessage)
                .replace("&", "§");
    }

    public FileConfiguration getConfig() {
        return config;
    }
}