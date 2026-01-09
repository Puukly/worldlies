package com.puukly.worldlies.events;

import com.puukly.worldlies.WorldLiesInterface;
import com.puukly.worldlies.gauges.Gauge;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class EventManager {

    private final WorldLiesInterface plugin;

    public EventManager(WorldLiesInterface plugin) {
        this.plugin = plugin;
    }

    public void spawnMonument() {
        World world = Bukkit.getWorlds().get(0); // Main world
        Location spawnLoc = world.getSpawnLocation();

        // Build a simple pillar structure
        buildMonumentStructure(spawnLoc);

        // Spawn frozen armor stand
        Location standLoc = spawnLoc.clone().add(0, 2, 0);
        ArmorStand stand = (ArmorStand) world.spawnEntity(standLoc, EntityType.ARMOR_STAND);
        stand.setCustomName("§6§lThe Observer");
        stand.setCustomNameVisible(true);
        stand.setGravity(false);
        stand.setInvulnerable(true);
        stand.setAI(false);

        // Create lore books for each gauge
        createGaugeLoreBooks(spawnLoc);

        // Particle effects
        spawnMonumentParticles(spawnLoc);

        // Sound effect
        world.playSound(spawnLoc, Sound.BLOCK_END_PORTAL_SPAWN, 2.0f, 0.8f);

        Bukkit.broadcastMessage("§6§l=== A Monument Rises ===");
        Bukkit.broadcastMessage("§eThe world's memory manifests at spawn.");
    }

    private void buildMonumentStructure(Location center) {
        World world = center.getWorld();

        // Base platform
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                Location loc = center.clone().add(x, -1, z);
                loc.getBlock().setType(Material.BLACKSTONE);
            }
        }

        // Pillars at corners
        int[] corners = {-2, 2};
        for (int x : corners) {
            for (int z : corners) {
                for (int y = 0; y < 4; y++) {
                    Location loc = center.clone().add(x, y, z);
                    loc.getBlock().setType(Material.POLISHED_BLACKSTONE_BRICKS);
                }
            }
        }

        // Central pedestal
        for (int y = 0; y < 3; y++) {
            Location loc = center.clone().add(0, y, 0);
            loc.getBlock().setType(Material.CRYING_OBSIDIAN);
        }

        // Lecterns for books
        createLecternRing(center);
    }

    private void createLecternRing(Location center) {
        World world = center.getWorld();
        double radius = 3.5;
        int gaugeCount = plugin.getGaugeManager().getAllGauges().size();

        int i = 0;
        for (Map.Entry<String, Gauge> entry : plugin.getGaugeManager().getAllGauges().entrySet()) {
            double angle = (2 * Math.PI * i) / gaugeCount;
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);

            Location lecternLoc = center.clone().add(x, 1, z);
            lecternLoc.getBlock().setType(Material.LECTERN);

            // Place book on lectern with gauge lore
            // Note: In actual implementation, you'd need to use BlockState

            i++;
        }
    }

    private void createGaugeLoreBooks(Location center) {
        Map<String, String> gaugeLore = Map.ofEntries(
                Map.entry("mining_pressure", "The veins beneath the earth whisper of labors past. The more we mine, the more the world shifts."),
                Map.entry("conflict_pressure", "Clashes of arms leave their mark. Each skirmish etches the world deeper into unrest."),
                Map.entry("death_pressure", "Lives lost weigh heavy; scars of the fallen guide the currents of fate."),
                Map.entry("settlement_pressure", "Villages grow, beds are laid, hearths lit. The world rewards those who build."),
                Map.entry("disturbance", "Portals pulse, the weave of dimensions trembles. Keep watch over these rifts."),
                Map.entry("exploration_pressure", "Paths are trodden, maps are drawn. Knowledge of the land swells and shapes the world."),
                Map.entry("magic_pressure", "Arcane energies surge and twist. The more magic wielded, the more unstable the land."),
                Map.entry("trade_pressure", "Coins exchanged, goods bartered. Prosperity brings both order and temptation."),
                Map.entry("construction_pressure", "Structures rise, monuments stand. Each new creation leaves a lasting impression."),
                Map.entry("farming_pressure", "Fields flourish, harvests yield, life spreads. The earth responds to caretakers."),
                Map.entry("redstone_pressure", "Cogs and circuits pulse, hidden gears move unseen. Mechanisms leave a subtle mark on reality."),
                Map.entry("population_pressure", "The footsteps of many weigh heavily on the world. The land remembers those who dwell within it."),
                Map.entry("ritual_pressure", "Rituals echo through the land; beacons shine as signs of power and intent.")
        );

        // Drop books near the monument
        World world = center.getWorld();
        int i = 0;
        for (Map.Entry<String, Gauge> entry : plugin.getGaugeManager().getAllGauges().entrySet()) {
            String gaugeName = entry.getKey();
            Gauge gauge = entry.getValue();

            ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
            BookMeta meta = (BookMeta) book.getItemMeta();

            meta.setTitle("§6" + formatGaugeName(gaugeName));
            meta.setAuthor("The Observer");

            String loreText = gaugeLore.getOrDefault(gaugeName, "The world watches this metric closely.");
            String currentValue = String.format("Current: %.2f", gauge.getValue());

            meta.addPage(loreText + "\n\n§8" + currentValue);

            book.setItemMeta(meta);

            // Drop near lecterns
            double angle = (2 * Math.PI * i) / plugin.getGaugeManager().getAllGauges().size();
            Location bookLoc = center.clone().add(3 * Math.cos(angle), 2, 3 * Math.sin(angle));
            world.dropItem(bookLoc, book);

            i++;
        }
    }

    private String formatGaugeName(String name) {
        return Arrays.stream(name.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .reduce((a, b) -> a + " " + b)
                .orElse(name);
    }

    private void spawnMonumentParticles(Location center) {
        World world = center.getWorld();

        // Continuous particle effect task
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Spiral particles
            for (int i = 0; i < 360; i += 15) {
                double radians = Math.toRadians(i);
                double x = 2 * Math.cos(radians);
                double z = 2 * Math.sin(radians);

                Location particleLoc = center.clone().add(x, 2 + Math.sin(radians), z);
                world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
            }
        }, 0L, 10L);
    }

    public void createLoreBook(Location location, String title, String author, List<String> pages) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();

        meta.setTitle(title);
        meta.setAuthor(author);

        for (String page : pages) {
            meta.addPage(page);
        }

        book.setItemMeta(meta);

        location.getWorld().dropItem(location, book);
    }

    public void spawnLoreChest(Location location) {
        Block block = location.getBlock();
        block.setType(Material.CHEST);

        // Add Fragment A book
        createLoreBook(location.add(0, 1, 0),
                "Fragment A",
                "Unknown",
                Arrays.asList(
                        "We called it an echo at first. A small tremor beneath the mines, the twitch of a boulder.",
                        "When the pickers came in force, veins of ore rose like fever dreams toward the surface. We thought luck had turned.",
                        "Then the cattle grew thin. The weather learned to be patient. Some nights the compass pointed at the wrong stars.",
                        "Do not mistake coincidence for will. If the ground listens, what does it wish to hear?"
                )
        );
    }
}