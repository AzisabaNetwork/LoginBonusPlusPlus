package me.pino.loginbonusplusplus.manager;

import me.pino.loginbonusplusplus.model.PlayerData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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

    public void savePlayer(PlayerData data) {

        String path = data.getUuid().toString();

        // claimedDays
        config.set(path + ".claimed", new ArrayList<>(data.getClaimedDays()));

        // streak
        config.set(path + ".streak", data.getStreak());

        // lastLoginDate (LocalDate → String)
        if (data.getLastLoginDate() != null) {
            config.set(path + ".lastLogin", data.getLastLoginDate().toString());
        } else {
            config.set(path + ".lastLogin", null);
        }

        // lastLoginMonth
        config.set(path + ".lastLoginMonth", data.getLastLoginMonth());

        // totalLoginDays
        config.set(path + ".total", data.getTotalLoginDays());

        // monthlyLoginCount
        config.set(path + ".monthly", data.getMonthlyLoginCount());

        try {
            config.save(file);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save player data for "
                    + path + ": " + e.getMessage());
        }
    }

    public void saveAll() {
        for (PlayerData data : cache.values()) {
            savePlayer(data);
        }
        plugin.getLogger().info("Saved " + cache.size() + " player data entries");
    }

    private PlayerData loadPlayerData(UUID uuid) {

        String path = uuid.toString();

        if (!config.contains(path)) {
            return null;
        }

        PlayerData data = new PlayerData(uuid);

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