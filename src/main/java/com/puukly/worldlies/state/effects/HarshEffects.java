package com.puukly.worldlies.state.effects;

import com.puukly.worldlies.WorldLiesInterface;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

public class HarshEffects implements EffectApplier {
    private final WorldLiesInterface plugin;
    private BukkitTask effectTask;

    public HarshEffects(WorldLiesInterface plugin) {
        this.plugin = plugin;
    }

    @Override
    public void applyEffects() {
        effectTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                // Survivor's Resolve - Resistance I without helmet
                ItemStack helmet = player.getInventory().getHelmet();
                if (helmet == null || helmet.getType() == Material.AIR) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 0, true, false));
                }

                // Stone-Limbed - Mining Fatigue I underground
                if (player.getLocation().getY() < 60) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 100, 0, true, false));
                }

                // Hunger's Grip - increased hunger
                player.setFoodLevel(Math.max(0, player.getFoodLevel() - 1));
            }
        }, 0L, 100L);
    }

    @Override
    public void removeEffects() {
        if (effectTask != null) {
            effectTask.cancel();
        }
    }
}