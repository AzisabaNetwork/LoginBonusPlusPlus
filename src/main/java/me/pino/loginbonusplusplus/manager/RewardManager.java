package me.pino.loginbonusplusplus.manager;

import me.pino.loginbonusplusplus.util.ItemStackSerializer;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RewardManager {

    private final JavaPlugin plugin;
    private File file;
    private FileConfiguration config;

    public RewardManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        file = new File(plugin.getDataFolder(), "rewards.yml");

        if (!file.exists()) {
            plugin.saveResource("rewards.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(file);
        plugin.getLogger().info("Loaded rewards configuration");
    }

    public List<String> getBaseRewards(int day) {
        List<String> rewards = new ArrayList<>();

        String path = "base." + day;
        if (config.contains(path)) {
            rewards = config.getStringList(path);
        }

        return rewards;
    }

    public List<ItemStack> getBaseRewardItems(int day) {
        List<String> rewardStrings = getBaseRewards(day);
        List<ItemStack> items = new ArrayList<>();

        for (String base64String : rewardStrings) {
            try {
                ItemStack item = ItemStackSerializer.itemFromBase64(base64String);
                if (item != null) {
                    items.add(item);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error processing reward: " + base64String + " - " + e.getMessage());
            }
        }

        return items;
    }

    public void saveDayRewards(int day, List<ItemStack> items) {
        List<String> base64Rewards = new ArrayList<>();

        for (ItemStack item : items) {
            if (item != null) {
                String base64 = ItemStackSerializer.itemToBase64(item);
                base64Rewards.add(base64);
            }
        }

        String path = "base." + day;
        config.set(path, base64Rewards);

        try {
            config.save(file);
            plugin.getLogger().info("Saved rewards for day " + day);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save rewards for day " + day + ": " + e.getMessage());
        }
    }

    public void reload() {
        load();
    }

    public List<ItemStack> getStreakRewards(int streak) {
        List<ItemStack> items = new ArrayList<>();

        String path = "streak." + streak;
        if (config.contains(path)) {
            List<String> base64Strings = config.getStringList(path);
            for (String base64String : base64Strings) {
                try {
                    ItemStack item = ItemStackSerializer.itemFromBase64(base64String);
                    if (item != null) {
                        items.add(item);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error processing streak reward: " + base64String + " - " + e.getMessage());
                }
            }
        }

        return items;
    }

    public List<ItemStack> getSpecialRewards(int day) {
        List<ItemStack> items = new ArrayList<>();

        String path = "special." + day;
        if (config.contains(path)) {
            List<String> base64Strings = config.getStringList(path);
            for (String base64String : base64Strings) {
                try {
                    ItemStack item = ItemStackSerializer.itemFromBase64(base64String);
                    if (item != null) {
                        items.add(item);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error processing special reward: " + base64String + " - " + e.getMessage());
                }
            }
        }

        return items;
    }

    public ItemStack getDayIcon(int day) {

        String materialPath = "base." + day + ".icon-material";

        if (config.contains(materialPath)) {
            try {
                return new ItemStack(Material.valueOf(
                        config.getString(materialPath)
                ));
            } catch (Exception ignored) {}
        }

        // 既存Base64方式
        String base64Path = "base." + day + ".icon";
        if (config.contains(base64Path)) {
            try {
                return ItemStackSerializer.itemFromBase64(
                        config.getString(base64Path)
                );
            } catch (Exception ignored) {}
        }

        return null;
    }
}
