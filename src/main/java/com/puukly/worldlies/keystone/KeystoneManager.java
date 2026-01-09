package com.puukly.worldlies.keystone;

import com.puukly.worldlies.WorldLiesInterface;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class KeystoneManager {

    private final WorldLiesInterface plugin;
    private final NamespacedKey keystoneKey;
    private UUID keystoneHolder;
    private Location keystoneLocation;
    private final Map<Location, ContainmentZone> containmentZones;
    private File dataFile;

    public KeystoneManager(WorldLiesInterface plugin) {
        this.plugin = plugin;
        this.keystoneKey = new NamespacedKey(plugin, "concordance_keystone");
        this.containmentZones = new HashMap<>();
        this.dataFile = new File(plugin.getDataFolder(), "keystone.yml");
    }

    public ItemStack createKeystone() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();

        String name = plugin.getConfig().getString("keystone.name", "§6§lConcordance Keystone");
        meta.setDisplayName(name);

        List<String> lore = plugin.getConfig().getStringList("keystone.lore");
        meta.setLore(lore);

        meta.getPersistentDataContainer().set(keystoneKey, PersistentDataType.BYTE, (byte) 1);
        meta.setEnchantmentGlintOverride(true);

        item.setItemMeta(meta);
        return item;
    }

    public boolean isKeystone(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        return item.getItemMeta().getPersistentDataContainer()
                .has(keystoneKey, PersistentDataType.BYTE);
    }

    public void giveKeystoneToPlayer(Player player) {
        // Ensure only one keystone exists
        if (keystoneExists()) {
            removeExistingKeystone();
        }

        ItemStack keystone = createKeystone();
        player.getInventory().addItem(keystone);
        keystoneHolder = player.getUniqueId();
        keystoneLocation = null;

        broadcastKeystoneTransfer(player.getName());
        plugin.log(Level.INFO, "Keystone given to " + player.getName());
    }

    public void dropKeystoneAtLocation(Location location) {
        if (keystoneExists()) {
            removeExistingKeystone();
        }

        ItemStack keystone = createKeystone();
        Item droppedItem = location.getWorld().dropItem(location, keystone);
        droppedItem.setPickupDelay(Integer.MAX_VALUE); // Unpickable initially

        keystoneHolder = null;
        keystoneLocation = location;

        // Create containment zone
        createContainmentZone(location);

        plugin.log(Level.INFO, "Keystone dropped at " + location);
    }

    private void createContainmentZone(Location location) {
        int radius = plugin.getConfig().getInt("keystone.containment_radius", 25);
        int countdown = plugin.getConfig().getInt("keystone.claim_countdown_seconds", 200);

        ContainmentZone zone = new ContainmentZone(plugin, location, radius, countdown);
        containmentZones.put(location, zone);
        zone.start();
    }

    public void updateKeystoneEffects() {
        // Apply glow effect to holder
        if (keystoneHolder != null) {
            Player holder = Bukkit.getPlayer(keystoneHolder);
            if (holder != null && holder.isOnline()) {
                if (!holder.isGlowing()) {
                    holder.setGlowing(true);
                }
            }
        }

        // Update containment zones
        for (ContainmentZone zone : new ArrayList<>(containmentZones.values())) {
            zone.update();
            if (zone.isExpired()) {
                containmentZones.remove(zone.getLocation());
            }
        }
    }

    public boolean keystoneExists() {
        // Check if player has it
        if (keystoneHolder != null) {
            Player player = Bukkit.getPlayer(keystoneHolder);
            if (player != null && player.isOnline()) {
                for (ItemStack item : player.getInventory().getContents()) {
                    if (isKeystone(item)) {
                        return true;
                    }
                }
            }
        }

        // Check if dropped in world
        if (keystoneLocation != null && keystoneLocation.getWorld() != null) {
            for (Item item : keystoneLocation.getWorld().getEntitiesByClass(Item.class)) {
                if (isKeystone(item.getItemStack())) {
                    return true;
                }
            }
        }

        return false;
    }

    public void removeExistingKeystone() {
        // Remove from player
        if (keystoneHolder != null) {
            Player player = Bukkit.getPlayer(keystoneHolder);
            if (player != null) {
                for (int i = 0; i < player.getInventory().getSize(); i++) {
                    ItemStack item = player.getInventory().getItem(i);
                    if (isKeystone(item)) {
                        player.getInventory().setItem(i, null);
                    }
                }
                player.setGlowing(false);
            }
        }

        // Remove from world
        if (keystoneLocation != null && keystoneLocation.getWorld() != null) {
            for (Item item : keystoneLocation.getWorld().getEntitiesByClass(Item.class)) {
                if (isKeystone(item.getItemStack())) {
                    item.remove();
                }
            }
        }

        keystoneHolder = null;
        keystoneLocation = null;
    }

    public void onPlayerDeath(Player player) {
        if (keystoneHolder != null && keystoneHolder.equals(player.getUniqueId())) {
            // Drop keystone at death location
            Location deathLoc = player.getLocation();
            dropKeystoneAtLocation(deathLoc);
            player.setGlowing(false);
        }
    }

    public void onPlayerPickup(Player player, ItemStack item) {
        if (isKeystone(item)) {
            keystoneHolder = player.getUniqueId();
            keystoneLocation = null;
            broadcastKeystoneTransfer(player.getName());
        }
    }

    private void broadcastKeystoneTransfer(String playerName) {
        if (plugin.getConfig().getBoolean("broadcasts.keystone_transfer", true)) {
            String message = plugin.getConfigManager()
                    .getMessage("keystone.transfer", "§6The light chooses another bearer.");
            Bukkit.broadcastMessage(message);
        }
    }

    public UUID getKeystoneHolder() {
        return keystoneHolder;
    }

    public Location getKeystoneLocation() {
        return keystoneLocation;
    }

    public void saveKeystone() {
        YamlConfiguration config = new YamlConfiguration();

        if (keystoneHolder != null) {
            config.set("holder", keystoneHolder.toString());
        }

        if (keystoneLocation != null) {
            config.set("location.world", keystoneLocation.getWorld().getName());
            config.set("location.x", keystoneLocation.getX());
            config.set("location.y", keystoneLocation.getY());
            config.set("location.z", keystoneLocation.getZ());
        }

        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.log(Level.SEVERE, "Failed to save keystone data: " + e.getMessage());
        }
    }

    public void loadKeystone() {
        if (!dataFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);

        if (config.contains("holder")) {
            try {
                keystoneHolder = UUID.fromString(config.getString("holder"));
            } catch (IllegalArgumentException e) {
                plugin.log(Level.WARNING, "Invalid UUID in keystone data");
            }
        }

        if (config.contains("location.world")) {
            World world = Bukkit.getWorld(config.getString("location.world"));
            if (world != null) {
                keystoneLocation = new Location(
                        world,
                        config.getDouble("location.x"),
                        config.getDouble("location.y"),
                        config.getDouble("location.z")
                );
            }
        }
    }
}