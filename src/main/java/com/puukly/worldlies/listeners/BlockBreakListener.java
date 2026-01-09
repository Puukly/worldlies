package com.puukly.worldlies.listeners;

import com.puukly.worldlies.WorldLiesInterface;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class BlockBreakListener implements Listener {
    private final WorldLiesInterface plugin;

    public BlockBreakListener(WorldLiesInterface plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!plugin.isSystemRunning() || plugin.isSystemPaused()) {
            return;
        }

        Material type = event.getBlock().getType();

        // Mining pressure
        if (isOre(type)) {
            plugin.getGaugeManager().incrementGauge("mining_pressure", 2.0, event.getPlayer().getUniqueId());
        } else if (isStone(type)) {
            plugin.getGaugeManager().incrementGauge("mining_pressure", 0.5, event.getPlayer().getUniqueId());
        }

        // Construction pressure (breaking rare blocks)
        if (isRareBlock(type)) {
            plugin.getGaugeManager().incrementGauge("construction_pressure", 1.0, event.getPlayer().getUniqueId());
        }
    }

    private boolean isOre(Material type) {
        return type.name().contains("_ORE") ||
                type == Material.ANCIENT_DEBRIS ||
                type == Material.RAW_IRON_BLOCK ||
                type == Material.RAW_COPPER_BLOCK ||
                type == Material.RAW_GOLD_BLOCK;
    }

    private boolean isStone(Material type) {
        return type == Material.STONE ||
                type == Material.DEEPSLATE ||
                type == Material.COBBLESTONE ||
                type == Material.ANDESITE ||
                type == Material.DIORITE ||
                type == Material.GRANITE;
    }

    private boolean isRareBlock(Material type) {
        return type == Material.OBSIDIAN ||
                type == Material.BEACON ||
                type == Material.SPAWNER ||
                type == Material.ENCHANTING_TABLE ||
                type == Material.END_PORTAL_FRAME;
    }
}