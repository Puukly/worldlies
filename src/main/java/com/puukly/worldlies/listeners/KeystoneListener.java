package com.puukly.worldlies.listeners;

import com.puukly.worldlies.WorldLiesInterface;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

public class KeystoneListener implements Listener {
    private final WorldLiesInterface plugin;

    public KeystoneListener(WorldLiesInterface plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        ItemStack item = event.getItem().getItemStack();

        if (plugin.getKeystoneManager().isKeystone(item)) {
            plugin.getKeystoneManager().onPlayerPickup(player, item);
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();

        if (plugin.getKeystoneManager().isKeystone(item)) {
            // Allow dropping but create containment zone
            plugin.getKeystoneManager().dropKeystoneAtLocation(event.getItemDrop().getLocation());
        }
    }
}