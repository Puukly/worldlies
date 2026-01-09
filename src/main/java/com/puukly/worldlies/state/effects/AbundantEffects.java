package com.puukly.worldlies.state.effects;

import com.puukly.worldlies.WorldLiesInterface;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

public class AbundantEffects implements EffectApplier {
    private final WorldLiesInterface plugin;
    private BukkitTask effectTask;

    public AbundantEffects(WorldLiesInterface plugin) {
        this.plugin = plugin;
    }

    @Override
    public void applyEffects() {
        effectTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                // Regenerative Warmth - Regen I in daylight
                if (player.getWorld().isDayTime() && player.getLocation().getBlock().getLightFromSky() < 15) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 0, true, false));
                }

                // Daytime Sprint - Speed I in inhabited chunks
                if (player.getWorld().isDayTime()) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 0, true, false));
                }
            }
        }, 0L, 80L);
    }

    @Override
    public void removeEffects() {
        if (effectTask != null) {
            effectTask.cancel();
        }
    }
}
