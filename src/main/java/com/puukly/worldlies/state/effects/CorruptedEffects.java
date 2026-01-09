package com.puukly.worldlies.state.effects;

import com.puukly.worldlies.WorldLiesInterface;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

public class CorruptedEffects implements EffectApplier {
    private final WorldLiesInterface plugin;
    private BukkitTask effectTask;

    public CorruptedEffects(WorldLiesInterface plugin) {
        this.plugin = plugin;
    }

    @Override
    public void applyEffects() {
        effectTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                // Shadow Mark - Darkness at night without light
                if (!player.getWorld().isDayTime()) {
                    boolean hasLight = false;
                    for (ItemStack item : player.getInventory().getContents()) {
                        if (item != null && isLightSource(item.getType())) {
                            hasLight = true;
                            break;
                        }
                    }

                    if (!hasLight) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 100, 0, true, false));
                    }
                }
            }
        }, 0L, 100L);
    }

    private boolean isLightSource(Material material) {
        return material == Material.TORCH ||
                material == Material.LANTERN ||
                material == Material.SOUL_LANTERN ||
                material == Material.GLOWSTONE ||
                material == Material.SEA_LANTERN;
    }

    @Override
    public void removeEffects() {
        if (effectTask != null) {
            effectTask.cancel();
        }
    }
}