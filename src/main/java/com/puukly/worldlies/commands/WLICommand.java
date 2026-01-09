// WLICommand.java
package com.puukly.worldlies.commands;

import com.puukly.worldlies.WorldLiesInterface;
import com.puukly.worldlies.gauges.Gauge;
import com.puukly.worldlies.state.StateManager;
import com.puukly.worldlies.state.WorldState;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.*;

public class WLICommand implements CommandExecutor, TabCompleter {

    private final WorldLiesInterface plugin;
    private final Map<String, PendingConfirmation> pendingConfirmations;

    public WLICommand(WorldLiesInterface plugin) {
        this.plugin = plugin;
        this.pendingConfirmations = new HashMap<>();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return cmdStatus(sender);
        }

        String subCmd = args[0].toLowerCase();

        switch (subCmd) {
            case "start":
                return cmdStart(sender);
            case "stop":
                return cmdStop(sender, args);
            case "pause":
                return cmdPause(sender);
            case "resume":
                return cmdResume(sender);
            case "status":
                return cmdStatus(sender);
            case "gauge":
                return cmdGauge(sender, args);
            case "reset":
                return cmdReset(sender, args);
            case "force":
                return cmdForce(sender, args);
            case "state":
                return cmdState(sender, args);
            case "keystone":
                return cmdKeystone(sender, args);
            case "event":
                return cmdEvent(sender, args);
            case "history":
                return cmdHistory(sender, args);
            case "debug":
                return cmdDebug(sender, args);
            default:
                sender.sendMessage("§cUnknown subcommand. Use /wli for help.");
                return false;
        }
    }

    private boolean cmdStart(CommandSender sender) {
        if (!sender.hasPermission("wli.admin.start")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (plugin.isSystemRunning()) {
            sender.sendMessage("§eSystem is already running.");
            return true;
        }

        plugin.startSystem();
        sender.sendMessage("§aWorld Logic Interface system started.");
        return true;
    }

    private boolean cmdStop(CommandSender sender, String[] args) {
        if (!sender.hasPermission("wli.admin.stop")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (!plugin.isSystemRunning()) {
            sender.sendMessage("§eSystem is not running.");
            return true;
        }

        // Require confirmation
        if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
            sender.sendMessage("§eAre you sure you want to stop the system?");
            sender.sendMessage("§eType §6/wli stop confirm §eto confirm.");
            return true;
        }

        plugin.stopSystem(true);
        sender.sendMessage("§aWorld Logic Interface system stopped.");
        return true;
    }

    private boolean cmdPause(CommandSender sender) {
        if (!sender.hasPermission("wli.admin.pause")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (!plugin.isSystemRunning()) {
            sender.sendMessage("§eSystem is not running.");
            return true;
        }

        if (plugin.isSystemPaused()) {
            sender.sendMessage("§eSystem is already paused.");
            return true;
        }

        plugin.pauseSystem();
        sender.sendMessage("§aWorld Logic Interface system paused.");
        return true;
    }

    private boolean cmdResume(CommandSender sender) {
        if (!sender.hasPermission("wli.admin.pause")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (!plugin.isSystemRunning()) {
            sender.sendMessage("§eSystem is not running.");
            return true;
        }

        if (!plugin.isSystemPaused()) {
            sender.sendMessage("§eSystem is not paused.");
            return true;
        }

        plugin.resumeSystem();
        sender.sendMessage("§aWorld Logic Interface system resumed.");
        return true;
    }

    private boolean cmdStatus(CommandSender sender) {
        if (!sender.hasPermission("wli.view")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        sender.sendMessage("§6§l=== World Lies Interface Status ===");
        sender.sendMessage("§eRunning: " + (plugin.isSystemRunning() ? "§aYes" : "§cNo"));
        sender.sendMessage("§ePaused: " + (plugin.isSystemPaused() ? "§aYes" : "§cNo"));
        sender.sendMessage("§eCurrent State: §6" + plugin.getStateManager().getCurrentState().getDisplayName());

        if (sender.hasPermission("wli.admin")) {
            sender.sendMessage("\n§6Top Gauges:");
            Map<String, Gauge> gauges = plugin.getGaugeManager().getAllGauges();

            gauges.entrySet().stream()
                    .sorted((a, b) -> Double.compare(b.getValue().getValue(), a.getValue().getValue()))
                    .limit(5)
                    .forEach(entry -> {
                        sender.sendMessage(String.format("  §e%s: §f%.2f",
                                entry.getKey(), entry.getValue().getValue()));
                    });
        }

        return true;
    }

    private boolean cmdGauge(CommandSender sender, String[] args) {
        if (!sender.hasPermission("wli.admin.gauge")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /wli gauge <view|set|top> [name] [value]");
            return true;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "view":
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /wli gauge view <name>");
                    return true;
                }
                return cmdGaugeView(sender, args[2]);
            case "set":
                if (args.length < 4) {
                    sender.sendMessage("§cUsage: /wli gauge set <name> <value>");
                    return true;
                }
                return cmdGaugeSet(sender, args[2], args[3]);
            case "top":
                return cmdGaugeTop(sender);
            default:
                sender.sendMessage("§cUnknown gauge action: " + action);
                return false;
        }
    }

    private boolean cmdGaugeView(CommandSender sender, String name) {
        Gauge gauge = plugin.getGaugeManager().getGauge(name);
        if (gauge == null) {
            sender.sendMessage("§cGauge not found: " + name);
            return true;
        }

        sender.sendMessage("§6§l=== Gauge: " + name + " ===");
        sender.sendMessage("§eCurrent Value: §f" + String.format("%.2f", gauge.getValue()));
        sender.sendMessage("§eRaw Total: §f" + String.format("%.2f", gauge.getRawTotal()));
        sender.sendMessage("§eEMA Alpha: §f" + gauge.getEmaAlpha());
        sender.sendMessage("§eMax Per Player: §f" + gauge.getMaxPerPlayer());

        return true;
    }

    private boolean cmdGaugeSet(CommandSender sender, String name, String valueStr) {
        Gauge gauge = plugin.getGaugeManager().getGauge(name);
        if (gauge == null) {
            sender.sendMessage("§cGauge not found: " + name);
            return true;
        }

        try {
            double value = Double.parseDouble(valueStr);
            gauge.setValue(value);
            sender.sendMessage("§aSet " + name + " to " + value);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid number: " + valueStr);
        }

        return true;
    }

    private boolean cmdGaugeTop(CommandSender sender) {
        sender.sendMessage("§6§l=== Top Gauges ===");

        plugin.getGaugeManager().getAllGauges().entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue().getValue(), a.getValue().getValue()))
                .limit(10)
                .forEach(entry -> {
                    sender.sendMessage(String.format("§e%s: §f%.2f",
                            entry.getKey(), entry.getValue().getValue()));
                });

        return true;
    }

    private boolean cmdReset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("wli.admin.reset")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /wli reset <gauges|gauge|deaths> [name|player]");
            return true;
        }

        String type = args[1].toLowerCase();

        switch (type) {
            case "gauges":
                plugin.getGaugeManager().resetAllGauges();
                sender.sendMessage("§aAll gauges reset.");
                return true;
            case "gauge":
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /wli reset gauge <name>");
                    return true;
                }
                plugin.getGaugeManager().resetGauge(args[2]);
                sender.sendMessage("§aGauge " + args[2] + " reset.");
                return true;
            case "deaths":
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /wli reset deaths <player|all>");
                    return true;
                }
                if (args[2].equalsIgnoreCase("all")) {
                    plugin.getDeathPenaltyManager().resetAllDeaths();
                    sender.sendMessage("§aAll death stacks reset.");
                } else {
                    Player target = Bukkit.getPlayer(args[2]);
                    if (target == null) {
                        sender.sendMessage("§cPlayer not found: " + args[2]);
                        return true;
                    }
                    plugin.getDeathPenaltyManager().resetPlayerDeaths(target.getUniqueId());
                    sender.sendMessage("§aReset death stacks for " + target.getName());
                }
                return true;
            default:
                sender.sendMessage("§cUnknown reset type: " + type);
                return false;
        }
    }

    private boolean cmdForce(CommandSender sender, String[] args) {
        if (!sender.hasPermission("wli.admin.force")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /wli force <abundant|harsh|chaotic|balanced|corrupted>");
            return true;
        }

        try {
            WorldState state = WorldState.valueOf(args[1].toUpperCase());
            plugin.getStateManager().forceState(state);
            sender.sendMessage("§aForced world state to: " + state.getDisplayName());
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cInvalid state: " + args[1]);
        }

        return true;
    }

    private boolean cmdState(CommandSender sender, String[] args) {
        if (!sender.hasPermission("wli.admin.force")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /wli state <set|lock|unlock> [state] [minutes]");
            return true;
        }

        // Simplified - just redirect to force for now
        if (args[1].equalsIgnoreCase("set")) {
            return cmdForce(sender, args);
        }

        sender.sendMessage("§cState lock/unlock not yet implemented.");
        return true;
    }

    private boolean cmdKeystone(CommandSender sender, String[] args) {
        if (!sender.hasPermission("wli.admin.keystone")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /wli keystone <locate|give|revoke> [player]");
            return true;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "locate":
                UUID holder = plugin.getKeystoneManager().getKeystoneHolder();
                if (holder != null) {
                    Player player = Bukkit.getPlayer(holder);
                    sender.sendMessage("§eKeystone held by: §6" + (player != null ? player.getName() : holder));
                } else {
                    sender.sendMessage("§eKeystone location: " + plugin.getKeystoneManager().getKeystoneLocation());
                }
                return true;
            case "give":
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /wli keystone give <player>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage("§cPlayer not found: " + args[2]);
                    return true;
                }
                plugin.getKeystoneManager().giveKeystoneToPlayer(target);
                sender.sendMessage("§aGave Keystone to " + target.getName());
                return true;
            case "revoke":
                plugin.getKeystoneManager().removeExistingKeystone();
                sender.sendMessage("§aRemoved Keystone from world.");
                return true;
            default:
                sender.sendMessage("§cUnknown keystone action: " + action);
                return false;
        }
    }

    private boolean cmdEvent(CommandSender sender, String[] args) {
        if (!sender.hasPermission("wli.admin.event")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /wli event <spawnmonument>");
            return true;
        }

        if (args[1].equalsIgnoreCase("spawnmonument")) {
            plugin.getEventManager().spawnMonument();
            sender.sendMessage("§aSpawning gauge monument...");
            return true;
        }

        sender.sendMessage("§cUnknown event: " + args[1]);
        return true;
    }

    private boolean cmdHistory(CommandSender sender, String[] args) {
        if (!sender.hasPermission("wli.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        int days = 7;
        if (args.length >= 2) {
            try {
                days = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid number: " + args[1]);
                return true;
            }
        }

        List<StateManager.StateChange> history = plugin.getStateManager().getHistory(days);

        sender.sendMessage("§6§l=== State Change History (Last " + days + " days) ===");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (StateManager.StateChange change : history) {
            String date = sdf.format(new Date(change.timestamp));
            sender.sendMessage(String.format("§e%s: §7%s §f→ §6%s",
                    date, change.from.getDisplayName(), change.to.getDisplayName()));
        }

        return true;
    }

    private boolean cmdDebug(CommandSender sender, String[] args) {
        if (!sender.hasPermission("wli.debug")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length < 2) {
            boolean current = plugin.getConfig().getBoolean("logging.debug_mode", false);
            sender.sendMessage("§eDebug mode is currently: " + (current ? "§aON" : "§cOFF"));
            return true;
        }

        boolean enable = args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("true");
        plugin.getConfig().set("logging.debug_mode", enable);
        plugin.saveConfig();

        sender.sendMessage("§aDebug mode " + (enable ? "enabled" : "disabled"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("start", "stop", "pause", "resume", "status",
                    "gauge", "reset", "force", "state", "keystone", "event", "history", "debug"));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "gauge":
                    completions.addAll(Arrays.asList("view", "set", "top"));
                    break;
                case "reset":
                    completions.addAll(Arrays.asList("gauges", "gauge", "deaths"));
                    break;
                case "force":
                case "state":
                    completions.addAll(Arrays.asList("abundant", "harsh", "chaotic", "balanced", "corrupted"));
                    break;
                case "keystone":
                    completions.addAll(Arrays.asList("locate", "give", "revoke"));
                    break;
                case "event":
                    completions.add("spawnmonument");
                    break;
                case "debug":
                    completions.addAll(Arrays.asList("on", "off"));
                    break;
            }
        }

        return completions;
    }

    private static class PendingConfirmation {
        long timestamp;
        String action;
    }
}