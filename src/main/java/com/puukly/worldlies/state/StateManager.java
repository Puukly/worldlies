package com.puukly.worldlies.state;

import com.puukly.worldlies.WorldLiesInterface;
import org.bukkit.boss.BossBar;

import java.util.List;

public class StateManager {
    public StateManager(WorldLiesInterface plugin) {
        // constructor with plugin parameter
    }

    public WorldState getCurrentState() {
        // return current state
        return null;
    }

    public void forceState(WorldState state) {
        // force a specific state
    }

    public List<StateChange> getHistory(int days) {
        // return state change history
        return List.of();
    }

    public BossBar getStateBossBar() {
        // return boss bar
        return null;
    }

    public void loadState() {
        // load from config
    }

    public void saveState() {
        // save to config
    }

    public void evaluateState() {
        // evaluate current state
    }

    public void clearAllEffects() {
        // clear all active effects
    }

    // Inner class
    public static class StateChange {
        public final long timestamp;
        public final WorldState from;
        public final WorldState to;

        public StateChange(long timestamp, WorldState from, WorldState to) {
            this.timestamp = timestamp;
            this.from = from;
            this.to = to;
        }
    }
}
