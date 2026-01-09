package com.puukly.worldlies.listeners;

import com.puukly.worldlies.WorldLiesInterface;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

public class InventoryListener implements Listener {
    private final WorldLiesInterface plugin;

    public InventoryListener(WorldLiesInterface plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();
        if (item == null) {
            return;
        }

        // Prevent keystone from being placed in containers
        if (plugin.getKeystoneManager().isKeystone(item)) {
            if (event.getClickedInventory() != null &&
                    event.getClickedInventory().getType() != InventoryType.PLAYER) {
                event.setCancelled(true);

                if (event.getWhoClicked() instanceof Player) {
                    Player player = (Player) event.getWhoClicked();
                    player.sendMessage("§cThe Keystone cannot be stored in containers!");
                }
            }
        }

        // Track construction (placing rare blocks)
        if (!plugin.isSystemRunning() || plugin.isSystemPaused()) {
            return;
        }

        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();

            if (isConstructionMaterial(item.getType())) {
                plugin.getGaugeManager().incrementGauge("construction_pressure", 0.5, player.getUniqueId());
            }
        }
    }

    private boolean isConstructionMaterial(Material type) {
        return type == Material.DIAMOND_BLOCK ||
                type == Material.EMERALD_BLOCK ||
                type == Material.NETHERITE_BLOCK ||
                type == Material.BEACON ||
                type == Material.CONDUIT;
    }
}