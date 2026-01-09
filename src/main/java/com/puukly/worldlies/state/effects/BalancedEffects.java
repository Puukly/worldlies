package com.puukly.worldlies.state.effects;

import com.puukly.worldlies.WorldLiesInterface;

public class BalancedEffects implements EffectApplier {
    private final WorldLiesInterface plugin;

    public BalancedEffects(WorldLiesInterface plugin) {
        this.plugin = plugin;
    }

    @Override
    public void applyEffects() {
        // Balanced state has minimal effects
    }

    @Override
    public void removeEffects() {
        // Nothing to remove
    }
}