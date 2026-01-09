package com.puukly.worldlies;

import com.puukly.worldlies.commands.*;
import com.puukly.worldlies.config.ConfigManager;
import com.puukly.worldlies.gauges.GaugeManager;
import com.puukly.worldlies.keystone.KeystoneManager;
import com.puukly.worldlies.listeners.*;
import com.puukly.worldlies.state.StateManager;
import com.puukly.worldlies.death.DeathPenaltyManager;
import com.puukly.worldlies.events.EventManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class WorldLiesInterface extends JavaPlugin {

  private static WorldLiesInterface instance;
  private ConfigManager configManager;
  private GaugeManager gaugeManager;
  private StateManager stateManager;
  private KeystoneManager keystoneManager;
  private DeathPenaltyManager deathPenaltyManager;
  private EventManager eventManager;
  private boolean systemRunning = false;
  private boolean systemPaused = false;

  @Override
  public void onEnable() {
    instance = this;

    // Save default config
    saveDefaultConfig();

    // Initialize managers
    configManager = new ConfigManager(this);
    gaugeManager = new GaugeManager(this);
    stateManager = new StateManager(this);
    keystoneManager = new KeystoneManager(this);
    deathPenaltyManager = new DeathPenaltyManager(this);
    eventManager = new EventManager(this);

    // Load data
    configManager.loadConfig();
    gaugeManager.loadGauges();
    stateManager.loadState();
    keystoneManager.loadKeystone();

    // Register listeners
    registerListeners();

    // Register commands
    registerCommands();

    // Start system if configured to auto-start
    if (getConfig().getBoolean("auto_start", false)) {
      startSystem();
    }

    getLogger().info("WorldLiesInterface has been enabled!");
  }

  @Override
  public void onDisable() {
    // Stop system
    if (systemRunning) {
      stopSystem(false);
    }

    // Save all data
    gaugeManager.saveGauges();
    stateManager.saveState();
    keystoneManager.saveKeystone();
    deathPenaltyManager.savePenalties();

    getLogger().info("WorldLiesInterface has been disabled!");
  }

  private void registerListeners() {
    getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
    getServer().getPluginManager().registerEvents(new EntityDeathListener(this), this);
    getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
    getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
    getServer().getPluginManager().registerEvents(new ChunkListener(this), this);
    getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
    getServer().getPluginManager().registerEvents(new PlayerMoveListener(this), this);
    getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
    getServer().getPluginManager().registerEvents(new KeystoneListener(this), this);
  }

  private void registerCommands() {
    WLICommand mainCommand = new WLICommand(this);
    getCommand("wli").setExecutor(mainCommand);
    getCommand("wli").setTabCompleter(mainCommand);
  }

  public void startSystem() {
    if (systemRunning) {
      return;
    }

    systemRunning = true;
    systemPaused = false;

    // Start gauge evaluation task (every 5 minutes = 6000 ticks)
    getServer().getScheduler().runTaskTimer(this, () -> {
      if (!systemPaused && systemRunning) {
        stateManager.evaluateState();
      }
    }, 6000L, 6000L);

    // Start death penalty update task (every second)
    getServer().getScheduler().runTaskTimer(this, () -> {
      if (!systemPaused && systemRunning) {
        deathPenaltyManager.updateAllPlayers();
      }
    }, 20L, 20L);

    // Start keystone glow task (every tick for smooth effect)
    getServer().getScheduler().runTaskTimer(this, () -> {
      if (systemRunning) {
        keystoneManager.updateKeystoneEffects();
      }
    }, 1L, 1L);

    getLogger().info("World Logic Interface system started");

    // Optional broadcast
    if (getConfig().getBoolean("broadcasts.system_start", true)) {
      getServer().broadcastMessage(
              configManager.getMessage("system.start", "§eThe world awakens. Its balance is now watching.")
      );
    }
  }

  public void pauseSystem() {
    if (!systemRunning || systemPaused) {
      return;
    }

    systemPaused = true;
    getLogger().info("World Logic Interface system paused");

    if (getConfig().getBoolean("broadcasts.system_pause", false)) {
      getServer().broadcastMessage(
              configManager.getMessage("system.pause", "§7The world grows still...")
      );
    }
  }

  public void resumeSystem() {
    if (!systemRunning || !systemPaused) {
      return;
    }

    systemPaused = false;
    getLogger().info("World Logic Interface system resumed");

    if (getConfig().getBoolean("broadcasts.system_resume", false)) {
      getServer().broadcastMessage(
              configManager.getMessage("system.resume", "§eThe world stirs once more.")
      );
    }
  }

  public void stopSystem(boolean broadcast) {
    if (!systemRunning) {
      return;
    }

    systemRunning = false;
    systemPaused = false;

    // Cancel all scheduled tasks
    getServer().getScheduler().cancelTasks(this);

    // Clear visual effects
    stateManager.clearAllEffects();
    deathPenaltyManager.clearAllVisuals();

    getLogger().info("World Logic Interface system stopped");

    if (broadcast && getConfig().getBoolean("broadcasts.system_stop", false)) {
      getServer().broadcastMessage(
              configManager.getMessage("system.stop", "§7The world returns to silence.")
      );
    }
  }

  public static WorldLiesInterface getInstance() {
    return instance;
  }

  public ConfigManager getConfigManager() {
    return configManager;
  }

  public GaugeManager getGaugeManager() {
    return gaugeManager;
  }

  public StateManager getStateManager() {
    return stateManager;
  }

  public KeystoneManager getKeystoneManager() {
    return keystoneManager;
  }

  public DeathPenaltyManager getDeathPenaltyManager() {
    return deathPenaltyManager;
  }

  public EventManager getEventManager() {
    return eventManager;
  }

  public boolean isSystemRunning() {
    return systemRunning;
  }

  public boolean isSystemPaused() {
    return systemPaused;
  }

  public void log(Level level, String message) {
    if (getConfig().getBoolean("logging.debug_mode", false) || level.intValue() >= Level.INFO.intValue()) {
      getLogger().log(level, message);
    }
  }
}