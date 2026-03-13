package me.pino.loginbonusplusplus.command;

import me.pino.loginbonusplusplus.gui.AdminCalendarGUI;
import me.pino.loginbonusplusplus.gui.CalendarGUI;
import me.pino.loginbonusplusplus.LoginBonusPlusPlus;
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

    public LoginBonusCommand(CalendarGUI calendarGUI, AdminCalendarGUI adminCalendarGUI, LoginBonusPlusPlus plugin) {
        this.plugin = plugin;
        this.calendarGUI = calendarGUI;
        this.adminCalendarGUI = adminCalendarGUI;
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

        // Handle /lb (no args) - open player calendar
        calendarGUI.open(player);
        return true;
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
            }
        }

        return completions;
    }
}
