package com.puukly.worldlies.listeners;

import com.puukly.worldlies.WorldLiesInterface;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class PlayerInteractListener implements Listener {
    private final WorldLiesInterface plugin;

    public PlayerInteractListener(WorldLiesInterface plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!plugin.isSystemRunning() || plugin.isSystemPaused()) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        Material type = block.getType();

        // Disturbance - portal usage
        if (type == Material.NETHER_PORTAL || type == Material.END_PORTAL) {
            plugin.getGaugeManager().incrementGauge("disturbance", 1.0, event.getPlayer().getUniqueId());
        }

        // Magic pressure
        if (type == Material.ENCHANTING_TABLE) {
            plugin.getGaugeManager().incrementGauge("magic_pressure", 1.0, event.getPlayer().getUniqueId());
        }

        // Ritual pressure
        if (type == Material.BEACON) {
            plugin.getGaugeManager().incrementGauge("ritual_pressure", 2.0, event.getPlayer().getUniqueId());
        }

        // Redstone pressure
        if (isRedstoneComponent(type)) {
            plugin.getGaugeManager().incrementGauge("redstone_pressure", 0.5, event.getPlayer().getUniqueId());
        }

        // Farming pressure
        if (isCrop(type)) {
            plugin.getGaugeManager().incrementGauge("farming_pressure", 0.2, event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!plugin.isSystemRunning() || plugin.isSystemPaused()) {
            return;
        }

        // Trade pressure
        if (event.getRightClicked() instanceof Villager) {
            plugin.getGaugeManager().incrementGauge("trade_pressure", 1.0, event.getPlayer().getUniqueId());
            plugin.getGaugeManager().incrementGauge("settlement_pressure", 0.5, event.getPlayer().getUniqueId());
        }
    }

    private boolean isRedstoneComponent(Material type) {
        return type == Material.REDSTONE ||
                type == Material.REPEATER ||
                type == Material.COMPARATOR ||
                type == Material.REDSTONE_TORCH ||
                type == Material.PISTON ||
                type == Material.STICKY_PISTON ||
                type == Material.HOPPER ||
                type == Material.DROPPER ||
                type == Material.DISPENSER;
    }

    private boolean isCrop(Material type) {
        return type == Material.WHEAT ||
                type == Material.CARROTS ||
                type == Material.POTATOES ||
                type == Material.BEETROOTS ||
                type == Material.COCOA ||
                type == Material.SWEET_BERRY_BUSH;
    }
}
