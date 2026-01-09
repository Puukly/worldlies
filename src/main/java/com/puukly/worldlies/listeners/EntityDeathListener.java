package com.puukly.worldlies.listeners;

import com.puukly.worldlies.WorldLiesInterface;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class EntityDeathListener implements Listener {
    private final WorldLiesInterface plugin;

    public EntityDeathListener(WorldLiesInterface plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!plugin.isSystemRunning() || plugin.isSystemPaused()) {
            return;
        }

        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();

        if (killer != null) {
            // Conflict pressure
            if (entity instanceof Player) {
                // PvP kill
                plugin.getGaugeManager().incrementGauge("conflict_pressure", 5.0, killer.getUniqueId());
            } else {
                // Mob kill
                plugin.getGaugeManager().incrementGauge("conflict_pressure", 0.5, killer.getUniqueId());
            }
        }
    }
}