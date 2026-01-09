package com.puukly.worldlies.death;

import com.puukly.worldlies.WorldLiesInterface;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class DeathPenaltyManager {

    private final WorldLiesInterface plugin;
    private final Map<UUID, PlayerDeathData> deathData;
    private File dataFile;

    public DeathPenaltyManager(WorldLiesInterface plugin) {
        this.plugin = plugin;
        this.deathData = new ConcurrentHashMap<>();
        this.dataFile = new File(plugin.getDataFolder(), "deaths.yml");
    }

    public void onPlayerDeath(Player player, boolean isPvP) {
        if (!plugin.getConfig().getBoolean("death_penalties.enabled", true)) {
            return;
        }

        UUID uuid = player.getUniqueId();
        PlayerDeathData data = deathData.computeIfAbsent(uuid, k -> new PlayerDeathData());

        // Check new player protection
        int protectedDeaths = plugin.getConfig().getInt("death_penalties.new_player_protection.first_deaths_reduced", 2);
        double reductionFactor = plugin.getConfig().getDouble("death_penalties.new_player_protection.reduction_factor", 0.5);

        boolean isProtected = data.getTotalDeaths() < protectedDeaths;

        // Add death to history
        data.addDeath(System.currentTimeMillis(), isPvP);

        // Calculate stacks
        long windowMs = plugin.getConfig().getLong("death_penalties.stacking.rolling_window_minutes", 90) * 60000;
        int currentStacks = data.getDeathsInWindow(windowMs);

        // Apply XP loss
        double xpLossPercent = plugin.getConfig().getDouble("death_penalties.xp_loss_percent", 70);
        if (plugin.getConfig().getBoolean("death_penalties.stacking.enabled", true)) {
            double increasePerStack = plugin.getConfig().getDouble("death_penalties.stacking.xp_loss_increase_per_stack", 5);
            xpLossPercent += increasePerStack * currentStacks;
        }

        if (isProtected) {
            xpLossPercent *= reductionFactor;
        }

        int currentXP = player.getTotalExperience();
        int xpToRemove = (int) (currentXP * (xpLossPercent / 100.0));
        player.setTotalExperience(Math.max(0, currentXP - xpToRemove));

        // Apply respawn debuffs
        if (plugin.getConfig().getBoolean("death_penalties.respawn_debuff.enabled", true)) {
            applyRespawnDebuffs(player, currentStacks, isProtected);
        }

        // Apply max health reduction
        if (plugin.getConfig().getBoolean("death_penalties.stacking.enabled", true)) {
            applyMaxHealthReduction(player, currentStacks, isProtected);
        }

        // Update death pressure gauge
        plugin.getGaugeManager().incrementGauge("death_pressure", 1.0, uuid);

        // Show warnings
        updateVisualWarnings(player, currentStacks);

        // Check for exhaustion
        checkExhaustion(player, currentStacks);

        if (plugin.getConfig().getBoolean("logging.log_death_penalties", true)) {
            plugin.log(Level.INFO, player.getName() + " died (stacks: " + currentStacks + ", protected: " + isProtected + ")");
        }
    }

    private void applyRespawnDebuffs(Player player, int stacks, boolean isProtected) {
        int baseDuration = plugin.getConfig().getInt("death_penalties.respawn_debuff.duration_seconds", 180) * 20;
        int durationIncrease = plugin.getConfig().getInt("death_penalties.stacking.debuff_duration_increase_per_stack", 30) * 20;

        int duration = baseDuration + (durationIncrease * stacks);
        if (isProtected) {
            duration /= 2;
        }

        int slownessLevel = plugin.getConfig().getInt("death_penalties.respawn_debuff.slowness_level", 2) - 1;
        int miningFatigueLevel = plugin.getConfig().getInt("death_penalties.respawn_debuff.mining_fatigue_level", 2) - 1;
        int weaknessLevel = plugin.getConfig().getInt("death_penalties.respawn_debuff.weakness_level", 1) - 1;
        int hungerLevel = plugin.getConfig().getInt("death_penalties.respawn_debuff.hunger_level", 1) - 1;

        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, slownessLevel, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, duration, miningFatigueLevel, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration, weaknessLevel, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, duration, hungerLevel, false, true));
    }

    private void applyMaxHealthReduction(Player player, int stacks, boolean isProtected) {
        double reductionPerStack = plugin.getConfig().getDouble("death_penalties.stacking.max_health_reduction_per_stack", 2.0);

        if (isProtected) {
            reductionPerStack /= 2;
        }

        double totalReduction = reductionPerStack * stacks;
        double baseHealth = Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getBaseValue();
        double newMaxHealth = Math.max(2.0, baseHealth - totalReduction); // Minimum 1 heart

        Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(newMaxHealth);
    }

    private void checkExhaustion(Player player, int stacks) {
        if (!plugin.getConfig().getBoolean("death_penalties.exhaustion.enabled", true)) {
            return;
        }

        double currentMaxHealth = Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getBaseValue();

        if (currentMaxHealth <= 2.0) { // 1 heart or less
            // Ban player temporarily
            int banMinutes = plugin.getConfig().getInt("death_penalties.exhaustion.ban_duration_minutes", 60);
            String banMessage = plugin.getConfig().getString("death_penalties.exhaustion.ban_message",
                    "Your body can no longer withstand the world's rejection. Rest, then return.");

            Date unbanDate = new Date(System.currentTimeMillis() + (banMinutes * 60000L));
            player.ban(banMessage, unbanDate, "WorldLiesInterface");

            // Reset their stacks for when they return
            PlayerDeathData data = deathData.get(player.getUniqueId());
            if (data != null) {
                data.clearRecentDeaths();
            }

            // Reset max health
            Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(20.0);

            player.kickPlayer(banMessage);

            plugin.log(Level.WARNING, player.getName() + " exhausted - banned for " + banMinutes + " minutes");
        }
    }

    public void updateAllPlayers() {
        if (!plugin.isSystemRunning() || plugin.isSystemPaused()) {
            return;
        }

        long windowMs = plugin.getConfig().getLong("death_penalties.stacking.rolling_window_minutes", 90) * 60000;

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            PlayerDeathData data = deathData.get(uuid);

            if (data != null) {
                int stacks = data.getDeathsInWindow(windowMs);

                // Update visual warnings
                updateVisualWarnings(player, stacks);

                // Decay old deaths
                data.cleanOldDeaths(windowMs);

                // Restore max health if stacks decreased
                restoreMaxHealth(player, stacks);
            }
        }
    }

    private void restoreMaxHealth(Player player, int currentStacks) {
        double reductionPerStack = plugin.getConfig().getDouble("death_penalties.stacking.max_health_reduction_per_stack", 2.0);
        double expectedMaxHealth = 20.0 - (reductionPerStack * currentStacks);
        double currentMaxHealth = Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getBaseValue();

        if (currentMaxHealth < expectedMaxHealth) {
            Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(Math.min(20.0, expectedMaxHealth));
        }
    }

    private void updateVisualWarnings(Player player, int stacks) {
        if (!plugin.getConfig().getBoolean("death_penalties.visual_warnings.enabled", true)) {
            return;
        }

        long windowMs = plugin.getConfig().getLong("death_penalties.stacking.rolling_window_minutes", 90) * 60000;
        int maxStacks = getMaxStacksBeforeExhaustion(player);

        double ratio = (double) stacks / maxStacks;

        String warningMessage = "";
        Sound warningSound = null;

        double cautionThreshold = plugin.getConfig().getDouble("death_penalties.visual_warnings.caution_threshold", 0.5);
        double dangerThreshold = plugin.getConfig().getDouble("death_penalties.visual_warnings.danger_threshold", 0.75);
        double criticalThreshold = plugin.getConfig().getDouble("death_penalties.visual_warnings.critical_threshold", 0.9);

        if (ratio >= criticalThreshold) {
            warningMessage = plugin.getConfigManager().getMessage("death_warnings.critical", "§4§lCritical: NEXT DEATH = Exhaustion");
            warningSound = Sound.BLOCK_ANVIL_BREAK;

            // Spawn damage particles
            player.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, player.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0);
        } else if (ratio >= dangerThreshold) {
            long timeToDecay = getTimeUntilStackDecay(player);
            String timeStr = formatTime(timeToDecay);
            warningMessage = plugin.getConfigManager()
                    .getMessage("death_warnings.danger", "§cDanger: Scar Stacks %current%/%max% — Recover in %time%")
                    .replace("%current%", String.valueOf(stacks))
                    .replace("%max%", String.valueOf(maxStacks))
                    .replace("%time%", timeStr);
            warningSound = Sound.BLOCK_ANVIL_PLACE;
        } else if (ratio >= cautionThreshold) {
            long timeToDecay = getTimeUntilStackDecay(player);
            String timeStr = formatTime(timeToDecay);
            warningMessage = plugin.getConfigManager()
                    .getMessage("death_warnings.caution", "§eWarning: Scar Stacks %current%/%max% — Recover in %time%")
                    .replace("%current%", String.valueOf(stacks))
                    .replace("%max%", String.valueOf(maxStacks))
                    .replace("%time%", timeStr);
            warningSound = Sound.BLOCK_NOTE_BLOCK_PLING;
        }

        if (!warningMessage.isEmpty()) {
            player.sendActionBar(warningMessage);

            if (warningSound != null && Math.random() < 0.05) { // Only play sound occasionally
                player.playSound(player.getLocation(), warningSound, 0.5f, 1.0f);
            }
        }
    }

    private int getMaxStacksBeforeExhaustion(Player player) {
        double reductionPerStack = plugin.getConfig().getDouble("death_penalties.stacking.max_health_reduction_per_stack", 2.0);
        return (int) (18.0 / reductionPerStack); // 20 health - 2 minimum = 18 / reduction
    }

    private long getTimeUntilStackDecay(Player player) {
        PlayerDeathData data = deathData.get(player.getUniqueId());
        if (data == null || data.getDeathHistory().isEmpty()) {
            return 0;
        }

        long windowMs = plugin.getConfig().getLong("death_penalties.stacking.rolling_window_minutes", 90) * 60000;
        long oldestDeath = data.getDeathHistory().get(0);
        long decayTime = oldestDeath + windowMs;

        return Math.max(0, decayTime - System.currentTimeMillis());
    }

    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;

        return String.format("%02d:%02d", minutes, seconds);
    }

    public void resetPlayerDeaths(UUID uuid) {
        deathData.remove(uuid);

        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(20.0);
        }
    }

    public void resetAllDeaths() {
        deathData.clear();

        for (Player player : Bukkit.getOnlinePlayers()) {
            Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(20.0);
        }
    }

    public void clearAllVisuals() {
        // Clear action bars by sending empty message
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendActionBar("");
        }
    }

    public PlayerDeathData getDeathData(UUID uuid) {
        return deathData.get(uuid);
    }

    public void savePenalties() {
        YamlConfiguration config = new YamlConfiguration();

        for (Map.Entry<UUID, PlayerDeathData> entry : deathData.entrySet()) {
            String key = entry.getKey().toString();
            PlayerDeathData data = entry.getValue();

            config.set(key + ".total_deaths", data.getTotalDeaths());
            config.set(key + ".death_history", data.getDeathHistory());
        }

        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.log(Level.SEVERE, "Failed to save death penalty data: " + e.getMessage());
        }
    }

    public void loadPenalties() {
        if (!dataFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);

        for (String uuidStr : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                PlayerDeathData data = new PlayerDeathData();

                data.setTotalDeaths(config.getInt(uuidStr + ".total_deaths", 0));
                List<Long> history = config.getLongList(uuidStr + ".death_history");
                for (Long timestamp : history) {
                    data.getDeathHistory().add(timestamp);
                }

                deathData.put(uuid, data);
            } catch (IllegalArgumentException e) {
                plugin.log(Level.WARNING, "Invalid UUID in death data: " + uuidStr);
            }
        }
    }
}