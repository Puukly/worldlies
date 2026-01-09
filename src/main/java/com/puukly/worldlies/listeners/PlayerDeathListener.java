package com.puukly.worldlies.listeners;

import com.puukly.worldlies.WorldLiesInterface;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathListener implements Listener {
    private final WorldLiesInterface plugin;

    public PlayerDeathListener(WorldLiesInterface plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.isSystemRunning()) {
            return;
        }

        Player player = event.getEntity();
        boolean isPvP = player.getKiller() != null;

        // Apply death penalties
        plugin.getDeathPenaltyManager().onPlayerDeath(player, isPvP);

        // Check if player has keystone
        plugin.getKeystoneManager().onPlayerDeath(player);
    }
}