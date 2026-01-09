package com.puukly.worldlies.listeners;

import com.puukly.worldlies.WorldLiesInterface;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public class ChunkListener implements Listener {
    private final WorldLiesInterface plugin;

    public ChunkListener(WorldLiesInterface plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!plugin.isSystemRunning() || plugin.isSystemPaused()) {
            return;
        }

        // Exploration pressure for newly generated chunks
        if (event.isNewChunk()) {
            plugin.getGaugeManager().incrementGauge("exploration_pressure", 1.0);
        }
    }
}
