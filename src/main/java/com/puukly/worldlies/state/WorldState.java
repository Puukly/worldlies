package com.puukly.worldlies.state;

import org.bukkit.boss.BarColor;

public enum WorldState {
    ABUNDANT("Gold Horizon", BarColor.YELLOW),
    HARSH("Ironbound", BarColor.RED),
    CHAOTIC("Fracture", BarColor.PURPLE),
    BALANCED("Equilibrium", BarColor.GREEN),
    CORRUPTED("End-Shadow", BarColor.PINK);

    private final String displayName;
    private final BarColor barColor;

    WorldState(String displayName, BarColor barColor) {
        this.displayName = displayName;
        this.barColor = barColor;
    }

    public String getDisplayName() {
        return displayName;
    }

    public BarColor getBarColor() {
        return barColor;
    }
}