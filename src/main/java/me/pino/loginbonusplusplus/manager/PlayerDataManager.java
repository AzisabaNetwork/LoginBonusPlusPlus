package me.pino.loginbonusplusplus.manager;

import me.pino.loginbonusplusplus.model.PlayerData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;


public class PlayerDataManager {

    private final JavaPlugin plugin;
    private File file;
    private FileConfiguration config;
    private final Map<UUID, PlayerData> cache;

    public PlayerDataManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.cache = new ConcurrentHashMap<>();
    }

    public void load() {

        file = new File(plugin.getDataFolder(), "players.yml");

        if (!file.exists()) {
            plugin.saveResource("players.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(file);

        for (String uuidString : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidString);
                PlayerData data = loadPlayerData(uuid);
                cache.put(uuid, data);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in players.yml: " + uuidString);
            }
        }

        plugin.getLogger().info("Loaded " + cache.size() + " player data entries");
    }

    public PlayerData getPlayer(UUID uuid) {

        PlayerData data = cache.get(uuid);
        if (data != null) {
            return data;
        }

        data = loadPlayerData(uuid);
        if (data == null) {
            data = new PlayerData(uuid);
        }

        cache.put(uuid, data);
        return data;
    }

    public void saveAll() {
        for (PlayerData data : cache.values()) {
            // config オブジェクトに書き込む（まだファイルには保存しない）
            String path = data.getUuid().toString();
            config.set(path + ".claimed", new ArrayList<>(data.getClaimedDays()));
            config.set(path + ".streak", data.getStreak());
            if (data.getLastLoginDate() != null) {
                config.set(path + ".lastLogin", data.getLastLoginDate().toString());
            } else {
                config.set(path + ".lastLogin", null);
            }
            config.set(path + ".lastLoginMonth", data.getLastLoginMonth());
            config.set(path + ".total", data.getTotalLoginDays());
            config.set(path + ".monthly", data.getMonthlyLoginCount());
        }

        // 一括で1回だけ保存（アトミック書き込み）
        saveConfigAtomically();
        plugin.getLogger().info("Saved " + cache.size() + " player data entries");
    }

    public void savePlayer(PlayerData data) {
        String path = data.getUuid().toString();
        config.set(path + ".claimed", new ArrayList<>(data.getClaimedDays()));
        config.set(path + ".streak", data.getStreak());
        if (data.getLastLoginDate() != null) {
            config.set(path + ".lastLogin", data.getLastLoginDate().toString());
        } else {
            config.set(path + ".lastLogin", null);
        }
        config.set(path + ".lastLoginMonth", data.getLastLoginMonth());
        config.set(path + ".total", data.getTotalLoginDays());
        config.set(path + ".monthly", data.getMonthlyLoginCount());
        config.set(path + ".freezeTickets", data.getFreezeTickets());

        saveConfigAtomically();
    }

    // 一時ファイル経由でアトミックに保存する
    private synchronized void saveConfigAtomically() {
        File tempFile = new File(file.getParentFile(), "players.yml.tmp");
        File backupFile = new File(file.getParentFile(), "players.yml.bak");
        try {
            config.save(tempFile);
            if (file.exists()) {
                Files.copy(file.toPath(), backupFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
            Files.move(tempFile.toPath(), file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save player data: " + e.getMessage());
            tempFile.delete();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save player data (config): " + e.getMessage());
            tempFile.delete();
        }
    }

    private PlayerData loadPlayerData(UUID uuid) {

        String path = uuid.toString();

        if (!config.contains(path)) {
            return null;
        }

        PlayerData data = new PlayerData(uuid);

        data.setFreezeTickets(config.getInt(path + ".freezeTickets", 0));

        // claimedDays
        List<Integer> claimedDays = config.getIntegerList(path + ".claimed");
        for (int day : claimedDays) {
            data.addClaimedDay(day);
        }

        // streak
        data.setStreak(config.getInt(path + ".streak", 0));

        // lastLoginDate (String → LocalDate)
        String dateString = config.getString(path + ".lastLogin");
        if (dateString != null) {
            try {
                data.setLastLoginDate(LocalDate.parse(dateString));
            } catch (Exception ignored) {
                plugin.getLogger().warning("Invalid date format for " + path);
            }
        }

        // lastLoginMonth
        data.setLastLoginMonth(
                config.getInt(path + ".lastLoginMonth",
                        LocalDate.now().getMonthValue())
        );

        // totalLoginDays
        data.setTotalLoginDays(config.getInt(path + ".total", 0));

        // monthlyLoginCount
        data.setMonthlyLoginCount(config.getInt(path + ".monthly", 0));

        return data;
    }
}