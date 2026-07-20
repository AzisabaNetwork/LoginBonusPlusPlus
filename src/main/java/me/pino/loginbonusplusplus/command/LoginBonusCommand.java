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
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LoginBonusCommand implements CommandExecutor, TabCompleter {

    private final CalendarGUI calendarGUI;
    private final AdminCalendarGUI adminCalendarGUI;
    private final LoginBonusPlusPlus plugin;
    private final PlayerDataManager playerDataManager;
    private final java.util.Map<UUID, Long> pendingResetConfirmations = new java.util.HashMap<>();
    private static final long CONFIRM_TIMEOUT_MS = 30_000L;

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
            if (!player.hasPermission("loginbonus.admin")) {
                player.sendMessage("§cYou don't have permission to use this command.");
                return true;
            }

            // Handle /lb admin reset-month
            if (args.length >= 2 && args[1].equalsIgnoreCase("reset-month")) {
                return handleResetMonthCommand(player, args);
            }

            // Handle /lb admin resetdata <player>
            if (args.length >= 3 && args[1].equalsIgnoreCase("resetdata")) {
                return handleResetDataCommand(player, args);
            }

            // Handle /lb admin resetall
            if (args.length >= 2 && args[1].equalsIgnoreCase("resetall")) {
                return handleResetAllCommand(player, args);
            }

            adminCalendarGUI.open(player);
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
        // /lb config
        // =========================
        if (args.length >= 1 && args[0].equalsIgnoreCase("config")) {

            if (!player.hasPermission("loginbonus.admin")) {
                player.sendMessage("§cYou don't have permission.");
                return true;
            }

            player.sendMessage("§6=== LoginBonusPlusPlus Config ===");
            player.sendMessage("§eClickable Reminder: §f" + plugin.getConfig().getBoolean("sendClickableReminder", false));
            player.sendMessage("§eDatabase Host: §f" + plugin.getConfig().getString("database.host", "localhost"));
            player.sendMessage("§eDatabase Name: §f" + plugin.getConfig().getString("database.name", "loginbonus"));
            player.sendMessage("§eSounds Enabled: §f" + plugin.getConfig().getBoolean("sounds.enabled", false));
            player.sendMessage("§eClaim Sound: §f" + plugin.getConfig().getString("sounds.claim.sound", "ENTITY_PLAYER_LEVELUP"));
            player.sendMessage("§eClaim Sound Enabled: §f" + plugin.getConfig().getBoolean("sounds.claim.enabled", false));
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

            // lastLoginDateを設定してmissed表示を防止
            if (data.getLastLoginDate() == null) {
                data.setLastLoginDate(LocalDate.now());
            }

            playerDataManager.savePlayer(data);

            player.sendMessage("§aAdded " + amount + " login days to " + target.getName());
            target.sendMessage("§eYour login count was increased by " + amount);

            return true;
        }

        // =========================
        // /lb debug reset <player>
        // =========================
        if (args.length >= 3 &&
                args[0].equalsIgnoreCase("debug") &&
                args[1].equalsIgnoreCase("reset")) {

            if (!player.hasPermission("loginbonus.admin")) {
                player.sendMessage("§cYou don't have permission.");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                player.sendMessage("§cPlayer not found: " + args[2]);
                return true;
            }

            PlayerData data = playerDataManager.getPlayer(target.getUniqueId());

            // Reset all data
            data.getClaimedDays().clear();
            data.setStreak(0);
            data.setTotalLoginDays(0);
            data.setMonthlyLoginCount(0);
            data.setLastLoginDate(null);
            data.setLastLoginMonth(0);

            playerDataManager.savePlayer(data);

            player.sendMessage("§aReset login data for " + target.getName());
            target.sendMessage("§cYour login data has been reset by an admin.");

            return true;
        }

        // =========================
        // /lb debug set day <player> <day>
        // =========================
        if (args.length >= 5 &&
                args[0].equalsIgnoreCase("debug") &&
                args[1].equalsIgnoreCase("set") &&
                args[2].equalsIgnoreCase("day")) {

            if (!player.hasPermission("loginbonus.admin")) {
                player.sendMessage("§cYou don't have permission.");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[3]);
            if (target == null) {
                player.sendMessage("§cPlayer not found: " + args[4]);
                return true;
            }

            int day;
            try {
                day = Integer.parseInt(args[4]);
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid number: " + args[4]);
                return true;
            }

            if (day < 1 || day > 31) {
                player.sendMessage("§cDay must be between 1 and 31.");
                return true;
            }

            PlayerData data = playerDataManager.getPlayer(target.getUniqueId());

            // Set monthly login count (unlockDay depends on this)
            data.setMonthlyLoginCount(day);

            playerDataManager.savePlayer(data);

            player.sendMessage("§aSet " + target.getName() + "'s unlock day to " + day);
            target.sendMessage("§eYour unlock day was set to " + day + " by admin.");

            return true;
        }

        // =========================
        // /lb check <player>
        // =========================
        if (args.length >= 2 && args[0].equalsIgnoreCase("check")) {

            Player target = Bukkit.getPlayerExact(args[1]);
            if (!player.hasPermission("loginbonus.admin")) {
                player.sendMessage("§cYou don't have permission.");
                return true;
            }
            if (target == null) {
                player.sendMessage("§cPlayer not found: " + args[1]);
                return true;
            }

            PlayerData data = playerDataManager.getPlayer(target.getUniqueId());

            player.sendMessage("§6=== " + target.getName() + "'s Login Data ===");
            player.sendMessage("§eMonthly Logins: §f" + data.getMonthlyLoginCount());
            player.sendMessage("§eLogin Streak: §f" + data.getStreak() + " days");
            player.sendMessage("§eTotal Login Days: §f" + data.getTotalLoginDays());
            player.sendMessage("§eLast Login: §f" + (data.getLastLoginDate() != null ? data.getLastLoginDate() : "Never"));
            player.sendMessage("§eClaimed Days: §f" + data.getClaimedDays().size() + " days");

            return true;
        }

        // =========================
        // /lb debug sendmessage
        // =========================
        if (args.length >= 2 &&
                args[0].equalsIgnoreCase("debug") &&
                args[1].equalsIgnoreCase("sendmessage")) {

            if (!player.hasPermission("loginbonus.admin")) {
                player.sendMessage("§cYou don't have permission.");
                return true;
            }

            net.md_5.bungee.api.chat.TextComponent base = new net.md_5.bungee.api.chat.TextComponent("§e未受取の報酬があります！ ");

            net.md_5.bungee.api.chat.TextComponent click = new net.md_5.bungee.api.chat.TextComponent("§a§n[ここをクリックしてカレンダーを開く]");
            click.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                    net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND,
                    "/lb"
            ));

            base.addExtra(click);
            player.spigot().sendMessage(base);

            player.sendMessage("§aTest message sent!");
            return true;
        }

        // =========================
        // /lb debug set month <player> <month>
        // =========================
        if (args.length >= 4 &&
                args[0].equalsIgnoreCase("debug") &&
                args[1].equalsIgnoreCase("set") &&
                args[2].equalsIgnoreCase("month")) {

            if (!player.hasPermission("loginbonus.admin")) {
                player.sendMessage("§cYou don't have permission.");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[3]);
            if (target == null) {
                player.sendMessage("§cPlayer not found: " + args[3]);
                return true;
            }

            int month;
            try {
                month = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid month number: " + args[3]);
                return true;
            }

            if (month < 1 || month > 12) {
                player.sendMessage("§cMonth must be between 1 and 12.");
                return true;
            }

            PlayerData data = playerDataManager.getPlayer(target.getUniqueId());

            // 強制的に月を変更（テスト用）
            data.setLastLoginMonth(month);
            playerDataManager.savePlayer(data);

            player.sendMessage("§aSet " + target.getName() + "'s month to " + month + " (for testing)");
            target.sendMessage("§eYour month was set to " + month + " by admin for testing.");

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
        player.sendMessage("§7/lb admin resetdata <player> §f- Reset player login data (要確認)");

        if (player.hasPermission("loginbonus.admin")) {
            player.sendMessage("§7/lb admin reset-month §f- Reset monthly data for all players");
            player.sendMessage("§7/lb admin resetall §f- 全プレイヤーのデータを完全リセット (要確認)");
            player.sendMessage("§7/lb reload §f- Reload configuration");
            player.sendMessage("§7/lb config §f- Show configuration info");
            player.sendMessage("§7/lb check <player> §f- Check player login data");
            player.sendMessage("§7/lb debug add <player> <amount> §f- Add login days");
            player.sendMessage("§7/lb debug reset <player> §f- Reset player data");
            player.sendMessage("§7/lb debug set day <player> <day> §f- Set unlock day");
            player.sendMessage("§7/lb debug set month <player> <month> §f- Set month (testing)");
        }
    }

    private boolean handleResetMonthCommand(Player player, String[] args) {
        // 全プレイヤーの月データをリセット
        int currentMonth = LocalDate.now().getMonthValue();
        int resetCount = 0;

        // オンラインプレイヤーを処理
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            PlayerData data = playerDataManager.getPlayer(onlinePlayer.getUniqueId());
            data.setMonthlyLoginCount(0);
            data.clearClaimedDays();
            data.setLastLoginMonth(currentMonth);
            playerDataManager.savePlayer(data);
            resetCount++;

            onlinePlayer.sendMessage("§eYour monthly login data has been reset by admin.");
        }

        player.sendMessage("§aMonthly reset completed for " + resetCount + " online players!");
        plugin.getLogger().info("Admin " + player.getName() + " performed monthly reset for " + resetCount + " players");

        return true;
    }

    private boolean handleResetDataCommand(Player player, String[] args) {
        // /lb admin resetdata confirm <player>
        if (args.length >= 4 && args[2].equalsIgnoreCase("confirm")) {
            Player target = Bukkit.getPlayerExact(args[3]);
            if (target == null) {
                player.sendMessage("§cPlayer not found: " + args[3]);
                return true;
            }

            Long requestedAt = pendingResetConfirmations.get(player.getUniqueId());
            if (requestedAt == null) {
                player.sendMessage("§c確認待ちのリセット要求がありません。先に /lb admin resetdata " + target.getName() + " を実行してください。");
                return true;
            }
            if (System.currentTimeMillis() - requestedAt > CONFIRM_TIMEOUT_MS) {
                pendingResetConfirmations.remove(player.getUniqueId());
                player.sendMessage("§c確認がタイムアウトしました。もう一度 /lb admin resetdata " + target.getName() + " を実行してください。");
                return true;
            }
            playerDataManager.backupToTimestampedFile();
            // ===== 実際のリセット処理 =====
            PlayerData data = playerDataManager.getPlayer(target.getUniqueId());
            data.getClaimedDays().clear();
            data.setStreak(0);
            data.setTotalLoginDays(0);
            data.setMonthlyLoginCount(0);
            data.setLastLoginDate(null);
            data.setLastLoginMonth(0);
            data.setFreezeTickets(0);

            playerDataManager.savePlayer(data);
            pendingResetConfirmations.remove(player.getUniqueId());

            player.sendMessage("§a" + target.getName() + " のログイン情報をリセットしました。");
            target.sendMessage("§cあなたのログイン情報は管理者によってリセットされました。");
            plugin.getLogger().info("Admin " + player.getName() + " reset login data for " + target.getName());
            return true;
        }

        // /lb admin resetdata <player>（1回目：確認要求）
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            player.sendMessage("§cPlayer not found: " + args[2]);
            return true;
        }

        pendingResetConfirmations.put(player.getUniqueId(), System.currentTimeMillis());

        player.sendMessage("§c§l警告: " + target.getName() + " のログイン情報を完全にリセットします。");
        player.sendMessage("§7この操作は取り消せません。(streak・月間ログイン・受取済み日・補填券が全て消去されます)");

        TextComponent confirmMsg = new TextComponent("§a§l[クリックで確定]");
        confirmMsg.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                "/lb admin resetdata confirm " + target.getName()));
        player.spigot().sendMessage(confirmMsg);

        player.sendMessage("§730秒以内に確定してください。");
        return true;
    }

    private boolean handleResetAllCommand(Player player, String[] args) {
        // /lb admin resetall confirm
        if (args.length >= 3 && args[2].equalsIgnoreCase("confirm")) {
            Long requestedAt = pendingResetConfirmations.get(player.getUniqueId());
            if (requestedAt == null) {
                player.sendMessage("§c確認待ちの全員リセット要求がありません。先に /lb admin resetall を実行してください。");
                return true;
            }
            if (System.currentTimeMillis() - requestedAt > CONFIRM_TIMEOUT_MS) {
                pendingResetConfirmations.remove(player.getUniqueId());
                player.sendMessage("§c確認がタイムアウトしました。もう一度 /lb admin resetall を実行してください。");
                return true;
            }

            // ===== 全員リセット実行 =====
            playerDataManager.resetAllPlayersData();
            pendingResetConfirmations.remove(player.getUniqueId());

            player.sendMessage("§a全プレイヤーのログインデータをリセットしました。");
            plugin.getLogger().warning("Admin " + player.getName() + " performed a FULL RESET of ALL player data.");

            // オンラインプレイヤー全員に通知
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.sendMessage("§cあなたのログインボーナスデータは管理者によって全てリセットされました。");
            }
            return true;
        }

        // /lb admin resetall（1回目：確認要求）
        pendingResetConfirmations.put(player.getUniqueId(), System.currentTimeMillis());

        player.sendMessage("§4§l⚠ 警告 ⚠");
        player.sendMessage("§c§l全プレイヤーのログインデータを完全にリセットします。");
        player.sendMessage("§7オンライン・オフライン問わず、streak・月間ログイン・受取済み日・補填券が§c§l全消去§r§7されます。");
        player.sendMessage("§7この操作は取り消せません。実行前に自動でバックアップが作成されます。");

        TextComponent confirmMsg = new TextComponent("§a§l[クリックで確定]");
        confirmMsg.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                "/lb admin resetall confirm"));
        player.spigot().sendMessage(confirmMsg);

        player.sendMessage("§730秒以内に確定してください。");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        // Only complete first argument
        if (args.length == 1) {
            if (sender instanceof Player && ((Player) sender).hasPermission("loginbonus.admin")) {
                String input = args[0].toLowerCase();
                if ("admin".startsWith(input)) {
                    completions.add("admin");
                }
                if ("reload".startsWith(input)) {
                    completions.add("reload");
                }
                if ("config".startsWith(input)) {
                    completions.add("config");
                }
                if ("debug".startsWith(input)) {
                    completions.add("debug");
                }
            }
            String input = args[0].toLowerCase();
            if ("check".startsWith(input)) {
                completions.add("check");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            if (sender.hasPermission("loginbonus.admin")) {
                String input = args[1].toLowerCase();
                if ("reset-month".startsWith(input)) {
                    completions.add("reset-month");
                }
                if ("resetdata".startsWith(input)) {
                    completions.add("resetdata");
                }
                if ("resetall".startsWith(input)) {
                    completions.add("resetall");
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("check")) {
            if (sender.hasPermission("loginbonus.admin")) {
                String input = args[1].toLowerCase();
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    if (onlinePlayer.getName().toLowerCase().startsWith(input)) {
                        completions.add(onlinePlayer.getName());
                    }
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("debug")) {
            if (sender.hasPermission("loginbonus.admin")) {
                String input = args[1].toLowerCase();
                if ("add".startsWith(input)) {
                    completions.add("add");
                }
                if ("reset".startsWith(input)) {
                    completions.add("reset");
                }
                if ("set".startsWith(input)) {
                    completions.add("set");
                }
                if ("month".startsWith(input)) {
                    completions.add("month");
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("debug")) {
            if (sender.hasPermission("loginbonus.admin")) {
                if (args[1].equalsIgnoreCase("set")) {
                    String input = args[2].toLowerCase();
                    if ("day".startsWith(input)) {
                        completions.add("day");
                    } else if ("month".startsWith(input)) {
                        completions.add("month");
                    }
                    if ("sendmessage".startsWith(input)) {
                        completions.add("sendmessage");
                    }
                } else {
                    String input = args[2].toLowerCase();
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        if (onlinePlayer.getName().toLowerCase().startsWith(input)) {
                            completions.add(onlinePlayer.getName());
                        }
                    }
                }
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("debug")) {
            if (sender.hasPermission("loginbonus.admin")) {
                if (args[1].equalsIgnoreCase("add")) {
                    String input = args[3].toLowerCase();
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        if (onlinePlayer.getName().toLowerCase().startsWith(input)) {
                            completions.add(onlinePlayer.getName());
                        }
                    }
                } else if (args[1].equalsIgnoreCase("set") && (args[2].equalsIgnoreCase("day") || args[2].equalsIgnoreCase("month"))) {
                    String input = args[3].toLowerCase();
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        if (onlinePlayer.getName().toLowerCase().startsWith(input)) {
                            completions.add(onlinePlayer.getName());
                        }
                    }
                }
            }
        } else if (args.length == 5 && args[0].equalsIgnoreCase("debug") && args[1].equalsIgnoreCase("set") && args[2].equalsIgnoreCase("day")) {
            if (sender.hasPermission("loginbonus.admin")) {
                String input = args[4].toLowerCase();
                for (int i = 1; i <= 31; i++) {
                    if (String.valueOf(i).startsWith(input)) {
                        completions.add(String.valueOf(i));
                    }
                }
            }
        } else if (args.length == 5 && args[0].equalsIgnoreCase("debug") && args[1].equalsIgnoreCase("set") && args[2].equalsIgnoreCase("month")) {
            if (sender.hasPermission("loginbonus.admin")) {
                String input = args[4].toLowerCase();
                for (int i = 1; i <= 12; i++) {
                    if (String.valueOf(i).startsWith(input)) {
                        completions.add(String.valueOf(i));
                    }
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("resetdata")) {
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