package com.puukly.worldlies.listeners;

import com.puukly.worldlies.WorldLiesInterface;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {
    private final WorldLiesInterface plugin;

    public PlayerJoinListener(WorldLiesInterface plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Add player to state bossbar
        if (plugin.getStateManager().getStateBossBar() != null) {
            plugin.getStateManager().getStateBossBar().addPlayer(player);
        }

        // Population pressure
        if (plugin.isSystemRunning() && !plugin.isSystemPaused()) {
            plugin.getGaugeManager().incrementGauge("population_pressure", 1.0);
        }

        // Load death penalty data
        plugin.getDeathPenaltyManager().loadPenalties();
    }
}