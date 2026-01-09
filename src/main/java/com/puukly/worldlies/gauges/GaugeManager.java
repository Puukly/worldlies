package com.puukly.worldlies.gauges;

import com.puukly.worldlies.WorldLiesInterface;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class GaugeManager {

    private final WorldLiesInterface plugin;
    private final Map<String, Gauge> gauges;
    private final Map<String, Map<UUID, Double>> playerContributions;
    private File dataFile;

    public GaugeManager(WorldLiesInterface plugin) {
        this.plugin = plugin;
        this.gauges = new ConcurrentHashMap<>();
        this.playerContributions = new ConcurrentHashMap<>();
        this.dataFile = new File(plugin.getDataFolder(), "gauges.yml");

        initializeGauges();
    }

    private void initializeGauges() {
        // Initialize all gauges from config
        ConfigurationSection gaugeConfig = plugin.getConfig().getConfigurationSection("gauges");
        if (gaugeConfig == null) {
            plugin.log(Level.WARNING, "No gauges section in config!");
            return;
        }

        for (String gaugeName : gaugeConfig.getKeys(false)) {
            ConfigurationSection gc = gaugeConfig.getConfigurationSection(gaugeName);
            if (gc != null) {
                double emaAlpha = gc.getDouble("ema_alpha", 0.1);
                Map<String, Double> thresholds = new HashMap<>();

                ConfigurationSection thresholdSection = gc.getConfigurationSection("thresholds");
                if (thresholdSection != null) {
                    for (String state : thresholdSection.getKeys(false)) {
                        thresholds.put(state, thresholdSection.getDouble(state));
                    }
                }

                double maxPerPlayer = gc.getDouble("max_per_player_per_day", 10000);

                gauges.put(gaugeName, new Gauge(gaugeName, emaAlpha, thresholds, maxPerPlayer));
                playerContributions.put(gaugeName, new ConcurrentHashMap<>());
            }
        }

        plugin.log(Level.INFO, "Initialized " + gauges.size() + " gauges");
    }

    public void incrementGauge(String gaugeName, double amount) {
        incrementGauge(gaugeName, amount, null);
    }

    public void incrementGauge(String gaugeName, double amount, UUID playerUUID) {
        if (!plugin.isSystemRunning() || plugin.isSystemPaused()) {
            return;
        }

        Gauge gauge = gauges.get(gaugeName);
        if (gauge == null) {
            return;
        }

        // Check per-player cap
        if (playerUUID != null) {
            Map<UUID, Double> contributions = playerContributions.get(gaugeName);
            if (contributions != null) {
                double currentContribution = contributions.getOrDefault(playerUUID, 0.0);
                if (currentContribution >= gauge.getMaxPerPlayer()) {
                    return; // Player has hit their cap
                }

                double allowedAmount = Math.min(amount, gauge.getMaxPerPlayer() - currentContribution);
                contributions.put(playerUUID, currentContribution + allowedAmount);
                amount = allowedAmount;
            }
        }

        gauge.addRawValue(amount);
        updateEMA(gauge, amount);

        plugin.log(Level.FINE, "Gauge " + gaugeName + " incremented by " + amount);
    }

    private void updateEMA(Gauge gauge, double newValue) {
        double alpha = gauge.getEmaAlpha();
        double currentValue = gauge.getValue();
        double newEMA = alpha * newValue + (1 - alpha) * currentValue;
        gauge.setValue(newEMA);
    }

    public double getNormalizedGauge(String gaugeName, String stateName) {
        Gauge gauge = gauges.get(gaugeName);
        if (gauge == null) {
            return 0.0;
        }

        Double threshold = gauge.getThresholds().get(stateName);
        if (threshold == null) {
            return 0.0;
        }

        return Math.min(1.0, gauge.getValue() / threshold);
    }

    public Gauge getGauge(String name) {
        return gauges.get(name);
    }

    public Map<String, Gauge> getAllGauges() {
        return new HashMap<>(gauges);
    }

    public void resetGauge(String gaugeName) {
        Gauge gauge = gauges.get(gaugeName);
        if (gauge != null) {
            gauge.reset();
            playerContributions.get(gaugeName).clear();
            plugin.log(Level.INFO, "Reset gauge: " + gaugeName);
        }
    }

    public void resetAllGauges() {
        for (Gauge gauge : gauges.values()) {
            gauge.reset();
        }
        for (Map<UUID, Double> contributions : playerContributions.values()) {
            contributions.clear();
        }
        plugin.log(Level.INFO, "Reset all gauges");
    }

    public void softResetGauges() {
        // Accelerated decay - reduces all values by 50% per tick for 10 seconds
        int duration = 200; // 10 seconds
        plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= duration) {
                    return;
                }

                for (Gauge gauge : gauges.values()) {
                    gauge.setValue(gauge.getValue() * 0.99);
                }

                ticks++;
            }
        }, 0L, 1L);

        plugin.log(Level.INFO, "Soft reset initiated for all gauges");
    }

    public void saveGauges() {
        YamlConfiguration config = new YamlConfiguration();

        for (Map.Entry<String, Gauge> entry : gauges.entrySet()) {
            String name = entry.getKey();
            Gauge gauge = entry.getValue();

            config.set(name + ".value", gauge.getValue());
            config.set(name + ".raw_total", gauge.getRawTotal());
            config.set(name + ".last_update", System.currentTimeMillis());

            // Save player contributions
            Map<UUID, Double> contributions = playerContributions.get(name);
            if (contributions != null && !contributions.isEmpty()) {
                for (Map.Entry<UUID, Double> contrib : contributions.entrySet()) {
                    config.set(name + ".contributions." + contrib.getKey().toString(), contrib.getValue());
                }
            }
        }

        try {
            config.save(dataFile);
            plugin.log(Level.INFO, "Saved gauge data");
        } catch (IOException e) {
            plugin.log(Level.SEVERE, "Failed to save gauge data: " + e.getMessage());
        }
    }

    public void loadGauges() {
        if (!dataFile.exists()) {
            plugin.log(Level.INFO, "No gauge data file found, starting fresh");
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);

        for (String gaugeName : gauges.keySet()) {
            Gauge gauge = gauges.get(gaugeName);

            if (config.contains(gaugeName + ".value")) {
                gauge.setValue(config.getDouble(gaugeName + ".value"));
                gauge.setRawTotal(config.getDouble(gaugeName + ".raw_total", 0));

                // Load player contributions
                ConfigurationSection contribSection = config.getConfigurationSection(gaugeName + ".contributions");
                if (contribSection != null) {
                    Map<UUID, Double> contributions = playerContributions.get(gaugeName);
                    for (String uuidStr : contribSection.getKeys(false)) {
                        try {
                            UUID uuid = UUID.fromString(uuidStr);
                            double value = contribSection.getDouble(uuidStr);
                            contributions.put(uuid, value);
                        } catch (IllegalArgumentException e) {
                            plugin.log(Level.WARNING, "Invalid UUID in contributions: " + uuidStr);
                        }
                    }
                }
            }
        }

        plugin.log(Level.INFO, "Loaded gauge data");
    }

    public void decayPlayerContributions(double decayRate) {
        // Reset or decay player contributions (called daily)
        for (Map<UUID, Double> contributions : playerContributions.values()) {
            for (UUID uuid : contributions.keySet()) {
                double current = contributions.get(uuid);
                contributions.put(uuid, current * decayRate);
            }
        }
    }
}