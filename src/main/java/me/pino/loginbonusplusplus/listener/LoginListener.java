package me.pino.loginbonusplusplus.listener;

import me.pino.loginbonusplusplus.manager.CalendarManager;
import me.pino.loginbonusplusplus.manager.MessageManager;
import me.pino.loginbonusplusplus.manager.PlayerDataManager;
import me.pino.loginbonusplusplus.manager.StreakManager;
import me.pino.loginbonusplusplus.model.PlayerData;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalDate;

public class LoginListener implements Listener {

    private final JavaPlugin plugin;
    private final PlayerDataManager playerDataManager;
    private final StreakManager streakManager;
    private final CalendarManager calendarManager;
    private final MessageManager messageManager;

    // 補填券の識別名（config.ymlで変更可能）
    private static final String FREEZE_ITEM_NAME = "§b§lログイン補填券";

    public LoginListener(JavaPlugin plugin,
                         PlayerDataManager playerDataManager,
                         StreakManager streakManager,
                         CalendarManager calendarManager,
                         MessageManager messageManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
        this.streakManager = streakManager;
        this.calendarManager = calendarManager;
        this.messageManager = messageManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerData data = playerDataManager.getPlayer(player.getUniqueId());

        LocalDate today = LocalDate.now();
        LocalDate lastLogin = data.getLastLoginDate();

        // ===== 同日ログイン防止 =====
        if (lastLogin != null && lastLogin.equals(today)) {
            return;
        }

        // ===== 月リセット =====
        if (data.getLastLoginMonth() != today.getMonthValue()) {
            data.setMonthlyLoginCount(0);
            data.clearClaimedDays();
            data.setLastLoginMonth(today.getMonthValue());
            plugin.getLogger().info("Monthly reset for " + player.getName());
        }

        // ===== 月ログイン回数加算 =====
        data.setMonthlyLoginCount(data.getMonthlyLoginCount() + 1);

        // ===== 連続ログイン処理 =====
        if (lastLogin == null) {
            data.setStreak(1);
        } else if (lastLogin.plusDays(1).equals(today)) {
            // 連続ログイン継続
            data.setStreak(data.getStreak() + 1);
        } else {
            long daysMissed = java.time.temporal.ChronoUnit.DAYS.between(lastLogin, today);

            if (daysMissed == 2 && data.getFreezeTickets() > 0) {
                data.consumeFreezeTicket();
                data.setStreak(data.getStreak() + 1);
                player.sendMessage("§b補填券§rを自動消費してstreakを維持しました！");
                player.sendMessage("§7streak: §e" + data.getStreak() + "日 §7/ 残り: §e" + data.getFreezeTickets() + "枚");
            } else {
                if (daysMissed > 2 && data.getFreezeTickets() > 0) {
                    player.sendMessage("§c2日以上空いたため補填券は使用できませんでした。");
                }
                int oldStreak = data.getStreak();
                data.setStreak(1);
                if (oldStreak >= 3) {
                    player.sendMessage("§c連続ログインが途切れました (streak " + oldStreak + "日 → 1日)");
                }
            }
        }

        data.setLastLoginDate(today);
        playerDataManager.savePlayer(data);

        // ===== 未受取通知 =====
        int unlockDay = data.getMonthlyLoginCount();
        if (!data.hasClaimed(unlockDay)) {
            boolean sendClickable = plugin.getConfig().getBoolean("sendClickableReminder", false);
            if (sendClickable) {
                TextComponent base = new TextComponent("§e未受取の報酬があります！ ");
                TextComponent click = new TextComponent("§a§n[ここをクリックしてカレンダーを開く]");
                click.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/lb"));
                base.addExtra(click);
                player.spigot().sendMessage(base);
            } else {
                player.sendMessage("§e未受取の報酬があります！/lb コマンドでカレンダーを開いてください。");
            }
        }

        plugin.getLogger().info("Login processed for "
                + player.getName()
                + " | Monthly: " + data.getMonthlyLoginCount()
                + " | Streak: " + data.getStreak());
    }

    // 補填券を持っているか確認
    private boolean hasFreezeItem(Player player) {
        String name = plugin.getConfig().getString("freeze-item.name", FREEZE_ITEM_NAME);
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;
            if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) continue;
            if (item.getItemMeta().getDisplayName().equals(name)) return true;
        }
        return false;
    }

    // 補填券を1枚消費
    private void consumeFreezeItem(Player player) {
        String name = plugin.getConfig().getString("freeze-item.name", FREEZE_ITEM_NAME);
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() == Material.AIR) continue;
            if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) continue;
            if (!item.getItemMeta().getDisplayName().equals(name)) continue;

            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().setItem(i, null);
            }
            return;
        }
    }
}