package com.puukly.worldlies.listeners;

import com.puukly.worldlies.WorldLiesInterface;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerMoveListener implements Listener {
    private final WorldLiesInterface plugin;
    private final Map<UUID, Long> lastMovement;

    public PlayerMoveListener(WorldLiesInterface plugin) {
        this.plugin = plugin;
        this.lastMovement = new HashMap<>();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!plugin.isSystemRunning() || plugin.isSystemPaused()) {
            return;
        }

        // Track exploration (throttled to prevent spam)
        UUID uuid = event.getPlayer().getUniqueId();
        long now = System.currentTimeMillis();
        Long last = lastMovement.get(uuid);

        if (last == null || now - last > 30000) { // Every 30 seconds
            double distance = event.getFrom().distance(event.getTo());
            if (distance > 10) {
                plugin.getGaugeManager().incrementGauge("exploration_pressure", 0.1, uuid);
                lastMovement.put(uuid, now);
            }
        }
    }
}