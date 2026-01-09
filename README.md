# World Lies Interface - Minecraft 1.21.1 Plugin

A comprehensive Minecraft plugin where the world dynamically reacts to player behavior through hidden gauges and world states.

## 🎮 Features Implemented

### ✅ Core Systems
- **13 Hidden Gauges** tracking player behavior:
  - Mining Pressure
  - Conflict Pressure
  - Death Pressure
  - Settlement Pressure
  - Disturbance
  - Exploration Pressure
  - Magic Pressure
  - Trade Pressure
  - Construction Pressure
  - Farming Pressure
  - Redstone Pressure
  - Population Pressure
  - Ritual/Beacon Pressure

- **5 World States** with unique effects:
  - **Abundant (Gold Horizon)** - Increased resources, faster growth
  - **Harsh (Ironbound)** - Survival challenges, scarce resources
  - **Chaotic (Fracture)** - Random effects and unpredictability
  - **Balanced (Equilibrium)** - Vanilla-like baseline
  - **Corrupted (End-Shadow)** - Nether/End elements bleeding into overworld

### ✅ State System
- **Exponential Moving Average (EMA)** smoothing to prevent gauge spikes
- **Hysteresis** to prevent state flapping
- **Cooldown system** between state changes
- **State scoring** based on weighted gauge values
- **Automatic state evaluation** every 5 minutes
- **Visual bossbar** showing current world state

### ✅ Death Penalty System
- **Stacking death penalties** within rolling time window
- **XP loss** (configurable, increases per stack)
- **Respawn debuffs** (Slowness, Mining Fatigue, Weakness, Hunger)
- **Temporary max health reduction** per death stack
- **Visual warnings** (actionbar, titles, particles, sounds)
- **Exhaustion threshold** - automatic temp ban at critical health
- **New player protection** (reduced penalties for first deaths)

### ✅ Concordance Keystone
- **Unique single-instance item** (only one exists on server)
- **Glowing effect** on holder (permanent, non-removable)
- **Containment zones** when dropped (10-second claim countdown)
- **Contested mechanics** (multiple players prevent claiming)
- **Cannot be stored in containers** (chests, ender chests, etc.)
- **Transfers on death** with automatic zone creation

### ✅ Admin Commands
Full command suite with permissions:
- `/wli start` - Start the system
- `/wli stop [confirm]` - Stop the system (requires confirmation)
- `/wli pause` - Pause gauge tracking
- `/wli resume` - Resume from pause
- `/wli status` - View system status and top gauges
- `/wli gauge <view|set|top>` - Manage gauges
- `/wli reset <gauges|gauge|deaths>` - Reset systems
- `/wli force <state>` - Force a world state
- `/wli keystone <locate|give|revoke>` - Manage keystone
- `/wli event spawnmonument` - Spawn gauge monument
- `/wli history [days]` - View state change history
- `/wli debug <on|off>` - Toggle debug mode

### ✅ Monument System
- **Visual monument** at spawn with gauge information
- **Lore books** for each gauge with flavor text
- **Frozen Observer entity** (armor stand)
- **Particle effects** (spiral END_ROD particles)

### ✅ State Effects
Each state applies unique buffs and debuffs:

**Abundant:**
- Regeneration I in daylight
- Speed I in inhabited chunks
- Haste I for surface mining

**Harsh:**
- Resistance I without helmet
- Mining Fatigue I underground
- Increased hunger drain

**Chaotic:**
- Random potion effects every 60s
- Random teleportation when sprinting (5% chance)

**Corrupted:**
- Darkness effect at night without light source

**Balanced:**
- Minimal effects (vanilla-like)

### ✅ Data Persistence
- Gauge values and player contributions
- Current world state
- State change history
- Death penalty stacks
- Keystone location/holder

## 🛠️ Building the Plugin

### Requirements
- Java 21
- Maven 3.6+
- Git

### Build Steps

```bash
# Navigate to plugin directory
cd WorldLiesInterface

# Build with Maven
mvn clean package

# The compiled JAR will be in:
# target/WorldLiesInterface-1.0.0.jar
```

## 📦 Installation

1. Build the plugin (see above) or download the JAR
2. Place `WorldLiesInterface-1.0.0.jar` in your server's `plugins/` folder
3. Start your Paper/Spigot 1.21.1 server
4. The plugin will generate a `config.yml` in `plugins/WorldLiesInterface/`
5. Configure as desired and `/wli start` to begin

## ⚙️ Configuration

The `config.yml` is extensively documented with all options. Key sections:

### System Settings
```yaml
auto_start: false  # Auto-start on server boot
save_interval_minutes: 10
```

### Gauge Configuration
Each gauge has:
- `ema_alpha` - Smoothing factor (0-1)
- `max_per_player_per_day` - Per-player contribution cap
- `thresholds` - State-specific thresholds

### State Configuration
Each state has:
- `entry_cooldown_seconds` - Cooldown before re-entering
- `duration_seconds` - Max duration (-1 = infinite)
- `score_weights` - Gauge weights for scoring
- `effects` - State-specific effects

### Death Penalties
```yaml
death_penalties:
  enabled: true
  xp_loss_percent: 70
  stacking:
    enabled: true
    rolling_window_minutes: 90
    max_health_reduction_per_stack: 2
  exhaustion:
    enabled: true
    ban_duration_minutes: 60
```

### Keystone Settings
```yaml
keystone:
  enabled: true
  containment_radius: 25
  claim_countdown_seconds: 10
```

## 🎯 Usage Guide

### For Server Admins

**Starting the System:**
```
/wli start
```

**Monitoring Gauges:**
```
/wli status          # View current state and top gauges
/wli gauge top       # View all gauges sorted
/wli gauge view mining_pressure  # View specific gauge details
```

**Managing State:**
```
/wli force abundant  # Force Abundant state
/wli history 7       # View last 7 days of state changes
```

**Keystone Management:**
```
/wli keystone locate  # Find keystone location/holder
/wli keystone give PlayerName  # Give keystone to player
/wli keystone revoke  # Remove keystone from world
```

**Maintenance:**
```
/wli pause           # Pause during events
/wli resume          # Resume after pause
/wli reset gauges    # Reset all gauges
/wli reset deaths PlayerName  # Reset death stacks
```

### For Players

**Understanding the World:**
- Watch the bossbar at the top of your screen for current world state
- Different states provide different buffs and challenges
- Your actions contribute to gauges that influence state changes
- Coordinate with other players to push the world toward desired states

**Death System:**
- Deaths stack within 90 minutes (default)
- Each death reduces max health temporarily
- Visual warnings appear as you approach exhaustion
- At exhaustion (1 heart remaining), you're temp-banned for 1 hour
- First 2 deaths have reduced penalties (new player protection)

**Concordance Keystone:**
- Ultra-rare unique item (only one exists)
- Holder glows permanently
- Cannot be hidden in chests
- When dropped, creates a contested zone
- Must claim alone for 10 seconds to pick up

## 📊 Gauge Mechanics

### How Gauges Work

1. **Player actions trigger gauge increments**
   - Mining ore: +2.0 to Mining Pressure
   - PvP kill: +5.0 to Conflict Pressure
   - Trading with villagers: +1.0 to Trade Pressure
   - etc.

2. **EMA smoothing prevents spikes**
   - New value = alpha × increment + (1-alpha) × old_value
   - Lower alpha = slower response, more stable

3. **Per-player caps prevent abuse**
   - Each player has a daily cap per gauge
   - Prevents single player from dominating

4. **Normalized values drive state scoring**
   - Gauge value / threshold = normalized (0-1)
   - Each state has custom thresholds

### State Evaluation Algorithm

```
Every 5 minutes:
1. For each state, calculate score:
   score = Σ(weight[gauge] × normalized[gauge])

2. Find highest scoring state

3. Check transition requirements:
   - New score > current score + hysteresis_margin
   - Cooldown expired for new state

4. If pass, transition to new state
```

## 🔧 Advanced Configuration

### Tuning Gauge Sensitivity

For **faster state changes**, decrease EMA alpha and thresholds:
```yaml
mining_pressure:
  ema_alpha: 0.2  # Responds faster (was 0.1)
  thresholds:
    abundant: 1000  # Easier to reach (was 2000)
```

For **more stable world**, increase hysteresis:
```yaml
state_evaluation:
  hysteresis_margin: 0.25  # Harder to change (was 0.15)
```

### Customizing State Effects

**Disable specific effects:**
```yaml
states:
  chaotic:
    effects:
      random_teleport_chance: 0  # Disable teleportation
```

**Adjust death penalties:**
```yaml
death_penalties:
  stacking:
    max_health_reduction_per_stack: 1.0  # Less harsh (was 2.0)
  new_player_protection:
    first_deaths_reduced: 5  # More protection (was 2)
```

## 🐛 Troubleshooting

### Plugin Not Starting
- Check console for errors
- Verify Java 21+ is installed
- Ensure Paper/Spigot 1.21.1 compatibility

### Gauges Not Tracking
- Confirm system is running: `/wli status`
- Check if system is paused
- Verify `auto_start: true` or manually `/wli start`

### States Not Changing
- Check gauge values: `/wli gauge top`
- Review thresholds in config.yml
- Increase debug logging: `/wli debug on`
- Check state cooldowns: `/wli history`

### Death Penalties Too Harsh
- Reduce `max_health_reduction_per_stack`
- Increase `rolling_window_minutes`
- Increase `new_player_protection.first_deaths_reduced`
- Or disable: `death_penalties.enabled: false`

## 📝 Permissions

- `wli.use` - Basic commands (default: true)
- `wli.view` - View status (default: true)
- `wli.admin` - All admin commands (default: op)
  - `wli.admin.start` - Start system
  - `wli.admin.stop` - Stop system
  - `wli.admin.pause` - Pause system
  - `wli.admin.reset` - Reset gauges/deaths
  - `wli.admin.gauge` - View/modify gauges
  - `wli.admin.force` - Force states
  - `wli.admin.keystone` - Manage keystone
  - `wli.admin.event` - Spawn monuments
- `wli.debug` - Debug commands (default: op)

## 🎨 Lore Integration

The plugin includes immersive lore books that can be found in monuments:

- **Fragment A** - First hints about the world's responsiveness
- **Observations of the Way** - Scholar's notes on patterns
- **Chamber Record** - The Observers' documentation

Use `/wli event spawnmonument` to create a monument with all lore books.

## 📈 Performance Notes

- Gauge updates: O(1) per player action
- State evaluation: O(states × gauges) every 5 minutes
- Death tracking: O(players) every second (only when active)
- Keystone effects: O(players) every tick (lightweight glow check)

**Recommended for servers:** Up to 100 players without issues

## 🤝 Contributing

This plugin is feature-complete based on the design document. Potential additions:

- Concordance Event (multi-phase endgame)
- Observer Chamber structure generation
- Additional world states
- More sophisticated state effects (ore generation, mob AI modifications)
- Integration with economy plugins
- Web dashboard for gauge monitoring

## 📄 License

This plugin was created as a comprehensive implementation of the "World Is Lying" design document.

## 🔗 Support

For issues or questions:
1. Check this README's troubleshooting section
2. Review the config.yml comments
3. Enable debug mode: `/wli debug on`
4. Check server logs in `logs/latest.log`

---

**Version:** 1.0.0  
**Minecraft:** 1.21.1 (Paper/Spigot)  
**Java:** 21+
