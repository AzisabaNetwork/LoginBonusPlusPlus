package me.pino.loginbonusplusplus.command;

import me.pino.loginbonusplusplus.gui.AdminCalendarGUI;
import me.pino.loginbonusplusplus.gui.CalendarGUI;
import me.pino.loginbonusplusplus.LoginBonusPlusPlus;
import me.pino.loginbonusplusplus.manager.PlayerDataManager;
import me.pino.loginbonusplusplus.model.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class LoginBonusCommand implements CommandExecutor, TabCompleter {

    private final CalendarGUI calendarGUI;
    private final AdminCalendarGUI adminCalendarGUI;
    private final LoginBonusPlusPlus plugin;
    private final PlayerDataManager playerDataManager;

    public LoginBonusCommand(CalendarGUI calendarGUI, AdminCalendarGUI adminCalendarGUI, LoginBonusPlusPlus plugin) {
        this.calendarGUI = calendarGUI;
        this.adminCalendarGUI = adminCalendarGUI;
        this.plugin = plugin;
        this.playerDataManager = plugin.getPlayerDataManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        // Handle /lb admin
        if (args.length >= 1 && args[0].equalsIgnoreCase("admin")) {
            if (player.hasPermission("loginbonus.admin")) {
                adminCalendarGUI.open(player);
            } else {
                player.sendMessage("§cYou don't have permission to use this command.");

            }
            return true;
        }

        // =========================
        // /lb reload
        // =========================
        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {

            if (!player.hasPermission("loginbonus.admin")) {
                player.sendMessage("§cYou don't have permission.");
                return true;
            }

            plugin.reloadPlugin();
            player.sendMessage("§aLoginBonusPlusPlus reloaded!");
            return true;
        }

        // =========================
        // /lb debug add <player> <number>
        // =========================
        if (args.length >= 4 &&
            args[0].equalsIgnoreCase("debug") &&
            args[1].equalsIgnoreCase("add")) {

            if (!player.hasPermission("loginbonus.admin")) {
                player.sendMessage("§cYou don't have permission.");
                return true;
            }

            String targetName = args[2];
            int amount;

            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid number: " + args[3]);
                return true;
            }

            Player target = Bukkit.getPlayerExact(targetName);
            if (target == null) {
                player.sendMessage("§cPlayer not found: " + targetName);
                return true;
            }

            PlayerData data = playerDataManager.getPlayer(target.getUniqueId());
            
            data.setTotalLoginDays(data.getTotalLoginDays() + amount);
            data.setMonthlyLoginCount(data.getMonthlyLoginCount() + amount);
            data.setStreak(data.getStreak() + amount);
            playerDataManager.savePlayer(data);

            player.sendMessage("§aAdded " + amount + " login days to " + target.getName());
            target.sendMessage("§eYour login count was increased by " + amount);
            
            return true;
        }

        // Handle /lb (no args) - open player calendar
        calendarGUI.open(player);
        return true;
    }

    private void sendUsage(Player player) {
        player.sendMessage("§cUsage:");
        player.sendMessage("§7/lb §f- Open login bonus calendar");
        player.sendMessage("§7/lb admin §f- Open admin calendar");
        player.sendMessage("§7/lb reload §f- Reload configuration");
        player.sendMessage("§7/lb debug add <player> <amount> §f- Add login days");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        // Only complete first argument
        if (args.length == 1) {
            // Check if sender has admin permission
            if (sender instanceof Player && ((Player) sender).hasPermission("loginbonus.admin")) {
                String input = args[0].toLowerCase();
                if ("admin".startsWith(input)) {
                    completions.add("admin");
                }
                if ("reload".startsWith(input)) {
                    completions.add("reload");
                }
                if ("debug".startsWith(input)) {
                    completions.add("debug");
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("debug")) {
            // Complete second argument for debug
            if (sender.hasPermission("loginbonus.admin")) {
                String input = args[1].toLowerCase();
                if ("add".startsWith(input)) {
                    completions.add("add");
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("debug") && args[1].equalsIgnoreCase("add")) {
            // Complete player names
            if (sender.hasPermission("loginbonus.admin")) {
                String input = args[2].toLowerCase();
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    if (onlinePlayer.getName().toLowerCase().startsWith(input)) {
                        completions.add(onlinePlayer.getName());
                    }
                }
            }
        }

        return completions;
    }
}
