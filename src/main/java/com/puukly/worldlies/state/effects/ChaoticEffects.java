package com.puukly.worldlies.state.effects;

import com.puukly.worldlies.WorldLiesInterface;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.Random;

public class ChaoticEffects implements EffectApplier {
    private final WorldLiesInterface plugin;
    private BukkitTask effectTask;
    private final Random random = new Random();

    public ChaoticEffects(WorldLiesInterface plugin) {
        this.plugin = plugin;
    }

    @Override
    public void applyEffects() {
        int interval = plugin.getConfig().getInt("states.chaotic.effects.random_effect_interval_seconds", 60) * 20;

        effectTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                // Unstable Vitality - random potion effects
                if (random.nextDouble() < 0.3) {
                    applyRandomEffect(player);
                }

                // Slipstream - random teleport
                double teleportChance = plugin.getConfig().getDouble("states.chaotic.effects.random_teleport_chance", 0.05);
                if (player.isSprinting() && random.nextDouble() < teleportChance) {
                    randomTeleport(player);
                }
            }
        }, 0L, interval);
    }

    private void applyRandomEffect(Player player) {
        PotionEffectType[] effects = {
                PotionEffectType.SPEED, PotionEffectType.SLOWNESS,
                PotionEffectType.STRENGTH, PotionEffectType.WEAKNESS,
                PotionEffectType.JUMP_BOOST, PotionEffectType.NAUSEA,
                PotionEffectType.REGENERATION, PotionEffectType.POISON,
                PotionEffectType.RESISTANCE, PotionEffectType.FIRE_RESISTANCE
        };

        PotionEffectType effect = effects[random.nextInt(effects.length)];
        int duration = 100 + random.nextInt(400); // 5-25 seconds
        int amplifier = random.nextInt(2); // 0-1

        player.addPotionEffect(new PotionEffect(effect, duration, amplifier, true, true));
    }

    private void randomTeleport(Player player) {
        Location loc = player.getLocation();
        double distance = 5 + random.nextDouble() * 10;
        double angle = random.nextDouble() * Math.PI * 2;

        Location newLoc = loc.clone();
        newLoc.add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
        newLoc.setY(player.getWorld().getHighestBlockYAt(newLoc));

        player.teleport(newLoc);
    }

    @Override
    public void removeEffects() {
        if (effectTask != null) {
            effectTask.cancel();
        }
    }
}