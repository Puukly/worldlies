package com.puukly.worldlies.keystone;

import com.puukly.worldlies.WorldLiesInterface;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.*;

public class ContainmentZone {

    private final WorldLiesInterface plugin;
    private final Location center;
    private final int radius;
    private final int countdownSeconds;
    private UUID claimingPlayer;
    private int countdown;
    private boolean active;
    private BossBar bossBar;

    public ContainmentZone(WorldLiesInterface plugin, Location center, int radius, int countdownSeconds) {
        this.plugin = plugin;
        this.center = center;
        this.radius = radius;
        this.countdownSeconds = countdownSeconds;
        this.countdown = 0;
        this.active = true;

        initializeBossBar();
    }

    private void initializeBossBar() {
        bossBar = Bukkit.createBossBar(
                "§c§lKeystone Contested",
                BarColor.RED,
                BarStyle.SOLID
        );
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, (task) -> {
            if (!active) {
                task.cancel();
                return;
            }

            update();
        }, 0L, 20L);
    }

    public void update() {
        List<Player> playersInZone = getPlayersInZone();

        // Update bossbar for nearby players
        bossBar.removeAll();
        for (Player player : playersInZone) {
            bossBar.addPlayer(player);
        }

        // Spawn particles
        spawnParticles();

        if (playersInZone.size() == 0) {
            // No players - reset
            resetClaim();
        } else if (playersInZone.size() == 1) {
            // One player - start or continue countdown
            Player player = playersInZone.get(0);

            if (claimingPlayer == null || !claimingPlayer.equals(player.getUniqueId())) {
                // New player started claiming
                claimingPlayer = player.getUniqueId();
                countdown = countdownSeconds;
            }

            countdown--;

            // Update bossbar
            bossBar.setTitle("§e§lClaiming Keystone — " + countdown + "s");
            bossBar.setColor(BarColor.YELLOW);
            bossBar.setProgress((double) countdown / countdownSeconds);

            // Send actionbar
            player.sendActionBar("§eClaiming Keystone — " + countdown + " seconds");

            if (countdown <= 0) {
                // Claim successful!
                onClaimSuccess(player);
            }
        } else {
            // Multiple players - contested
            resetClaim();
            bossBar.setTitle("§c§lKeystone Contested — Players Nearby: " + playersInZone.size());
            bossBar.setColor(BarColor.RED);
            bossBar.setProgress(1.0);
        }
    }

    private List<Player> getPlayersInZone() {
        List<Player> players = new ArrayList<>();

        for (Player player : center.getWorld().getPlayers()) {
            if (player.getLocation().distance(center) <= radius) {
                // Ignore spectators and vanished admins
                if (player.getGameMode() != GameMode.SPECTATOR) {
                    players.add(player);
                }
            }
        }

        return players;
    }

    private void spawnParticles() {
        // Ring of particles around keystone
        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 16) {
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            Location particleLoc = new Location(center.getWorld(), x, center.getY(), z);

            center.getWorld().spawnParticle(
                    Particle.END_ROD,
                    particleLoc,
                    1,
                    0, 0.5, 0,
                    0
            );
        }
    }

    private void resetClaim() {
        claimingPlayer = null;
        countdown = countdownSeconds;
    }

    private void onClaimSuccess(Player player) {
        // Make keystone pickable for this player
        active = false;
        bossBar.removeAll();

        String message = plugin.getConfigManager()
                .getMessage("keystone.claimed", "§a§l%player% has claimed the Concordance Keystone!")
                .replace("%player%", player.getName());

        Bukkit.broadcastMessage(message);

        center.getWorld().playSound(center, Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 1.0f);
    }

    public boolean isExpired() {
        return !active;
    }

    public Location getLocation() {
        return center;
    }
}