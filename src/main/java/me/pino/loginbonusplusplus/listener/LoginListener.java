package me.pino.loginbonusplusplus.listener;

import me.pino.loginbonusplusplus.manager.CalendarManager;
import me.pino.loginbonusplusplus.manager.MessageManager;
import me.pino.loginbonusplusplus.manager.PlayerDataManager;
import me.pino.loginbonusplusplus.manager.StreakManager;
import me.pino.loginbonusplusplus.model.PlayerData;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalDate;

public class LoginListener implements Listener {

    private final JavaPlugin plugin;
    private final PlayerDataManager playerDataManager;
    private final StreakManager streakManager;
    private final CalendarManager calendarManager;
    private final MessageManager messageManager;

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
        }
        else if (lastLogin.plusDays(1).equals(today)) {
            data.setStreak(data.getStreak() + 1);
        }
        else {
            data.setStreak(1);
        }

        data.setLastLoginDate(today);

        playerDataManager.savePlayer(data);

        // ===== 未受取通知 =====
        int unlockDay = data.getMonthlyLoginCount();

        if (!data.hasClaimed(unlockDay)) {

            boolean sendClickable = plugin.getConfig().getBoolean("sendClickableReminder", false);

            if (sendClickable) {

                TextComponent base = new TextComponent(
                        messageManager.get("reminder-prefix")
                );

                TextComponent click = new TextComponent(
                        messageManager.get("reminder-click")
                );

                click.setClickEvent(new ClickEvent(
                        ClickEvent.Action.RUN_COMMAND,
                        "/lb"
                ));

                base.addExtra(click);

                player.spigot().sendMessage(base);

            } else {
                player.sendMessage(
                        messageManager.get("reminder-plain")
                );
            }
        }

        plugin.getLogger().info("Login processed for "
                + player.getName()
                + " | Monthly: " + data.getMonthlyLoginCount()
                + " | Streak: " + data.getStreak());
    }
}