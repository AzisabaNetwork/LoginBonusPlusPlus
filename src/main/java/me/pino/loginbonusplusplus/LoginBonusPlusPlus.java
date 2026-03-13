package me.pino.loginbonusplusplus;

import me.pino.loginbonusplusplus.command.LoginBonusCommand;
import me.pino.loginbonusplusplus.gui.DayRewardEditGUI;
import me.pino.loginbonusplusplus.listener.LoginListener;
import me.pino.loginbonusplusplus.manager.*;
import org.bukkit.plugin.java.JavaPlugin;
import me.pino.loginbonusplusplus.gui.CalendarGUI;
import me.pino.loginbonusplusplus.gui.AdminCalendarGUI;
import me.pino.loginbonusplusplus.listener.CalendarClickListener;
import me.pino.loginbonusplusplus.listener.AdminCalendarClickListener;
import me.pino.loginbonusplusplus.manager.CalendarManager;
import me.pino.loginbonusplusplus.manager.MessageManager;

import java.io.File;

public class LoginBonusPlusPlus extends JavaPlugin {

    private PlayerDataManager playerDataManager;
    private RewardManager rewardManager;
    private StreakManager streakManager;
    private MessageManager messageManager;

    @Override
    public void onEnable() {
        getLogger().info("LoginBonusPlusPlus has been enabled!");

        // データフォルダ作成
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // リソース保存（初回のみ）
        saveResource("config.yml", false);
        saveResource("rewards.yml", false);
        saveResource("players.yml", false);
        saveResource("messages.yml", false);

        // ===== Managers =====
        playerDataManager = new PlayerDataManager(this);
        rewardManager = new RewardManager(this);
        streakManager = new StreakManager();
        CalendarManager calendarManager = new CalendarManager();
        messageManager = new MessageManager(this);
        //MessageManager messageManager = new MessageManager(this);

        playerDataManager.load();
        rewardManager.load();

        // ===== GUIs =====
        CalendarGUI calendarGUI =
                new CalendarGUI(this, playerDataManager, rewardManager);

        AdminCalendarGUI adminCalendarGUI =
                new AdminCalendarGUI(this, rewardManager);

        DayRewardEditGUI dayRewardEditGUI =
                new DayRewardEditGUI(rewardManager, adminCalendarGUI, messageManager, this);

        // ===== Commands =====
        LoginBonusCommand command =
                new LoginBonusCommand(calendarGUI, adminCalendarGUI, this);

        getCommand("lb").setExecutor(command);
        getCommand("lb").setTabCompleter(command);



        // ===== Listeners =====
        getServer().getPluginManager().registerEvents(
                new LoginListener(this, playerDataManager, streakManager, calendarManager, messageManager),
                this
        );

        getServer().getPluginManager().registerEvents(
                new CalendarClickListener(this, playerDataManager, rewardManager, messageManager),
                this
        );

        getServer().getPluginManager().registerEvents(
                new AdminCalendarClickListener(rewardManager, dayRewardEditGUI, adminCalendarGUI, messageManager),
                this
        );

        getServer().getPluginManager().registerEvents(
                dayRewardEditGUI,
                this
        );

        getLogger().info("LoginBonusPlusPlus fully initialized!");
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) {
            playerDataManager.saveAll();
        }
        getLogger().info("LoginBonusPlusPlus has been disabled!");
    }

    public void reloadPlugin() {
        reloadConfig();
        rewardManager.reload();
        messageManager.reload();
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }
}
