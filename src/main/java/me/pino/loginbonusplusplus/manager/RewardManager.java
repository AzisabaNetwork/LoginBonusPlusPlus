package me.pino.loginbonusplusplus.manager;

import me.pino.loginbonusplusplus.util.ItemStackSerializer;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RewardManager {

    private final JavaPlugin plugin;
    private File file;
    private FileConfiguration config;
    private File streakFile;
    private FileConfiguration streakConfig;
    private File claimableIconFile;
    private FileConfiguration claimableIconConfig;

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

        // Load streak rewards from separate file
        streakFile = new File(plugin.getDataFolder(), "streakrewards.yml");
        if (!streakFile.exists()) {
            try {
                streakFile.createNewFile();
                streakConfig = YamlConfiguration.loadConfiguration(streakFile);
                // Save empty file
                streakConfig.save(streakFile);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to create streakrewards.yml: " + e.getMessage());
                streakConfig = new YamlConfiguration();
            }
        } else {
            streakConfig = YamlConfiguration.loadConfiguration(streakFile);
        }
        claimableIconFile = new File(plugin.getDataFolder(), "claimableicons.yml");
        if (!claimableIconFile.exists()) {
            try { claimableIconFile.createNewFile(); } catch (Exception ignored) {}
        }
        claimableIconConfig = YamlConfiguration.loadConfiguration(claimableIconFile);
    }

    public ItemStack getClaimableIcon(int day) {
        String path = "day." + day;
        if (!claimableIconConfig.contains(path)) return null;
        try {
            String base64 = claimableIconConfig.getString(path);
            if (base64 != null && base64.startsWith("rO0AB")) {
                return ItemStackSerializer.itemFromBase64(base64);
            }
        } catch (Exception ignored) {}
        return null;
    }

    public void saveClaimableIcon(int day, ItemStack icon) {
        try {
            claimableIconConfig.set("day." + day, ItemStackSerializer.itemToBase64(icon));
            claimableIconConfig.save(claimableIconFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save claimable icon for day " + day + ": " + e.getMessage());
        }
    }

    public void clearClaimableIcon(int day) {
        claimableIconConfig.set("day." + day, null);
        try {
            claimableIconConfig.save(claimableIconFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to clear claimable icon for day " + day + ": " + e.getMessage());
        }
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
        if (streakConfig.contains(path)) {
            List<String> base64Strings = streakConfig.getStringList(path);
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

    public void saveStreakRewards(int streak, List<ItemStack> rewards) {
        String path = "streak." + streak;
        
        if (rewards.isEmpty()) {
            streakConfig.set(path, null);
        } else {
            List<String> base64Strings = new ArrayList<>();
            for (ItemStack item : rewards) {
                try {
                    String base64 = ItemStackSerializer.itemToBase64(item);
                    base64Strings.add(base64);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to serialize streak reward: " + e.getMessage());
                }
            }
            streakConfig.set(path, base64Strings);
        }
        
        try {
            streakConfig.save(streakFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save streak rewards: " + e.getMessage());
        }
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

        // カスタムモデルデータ付きアイコン
        String iconPath = "base." + day + ".icon";
        if (config.contains(iconPath)) {
            try {
                // Base64シリアライズされたItemStackを優先
                List<String> iconList = config.getStringList(iconPath);
                if (!iconList.isEmpty()) {
                    String base64Data = iconList.get(0);
                    if (base64Data.startsWith("rO0AB")) {
                        return ItemStackSerializer.itemFromBase64(base64Data);
                    }
                }
            } catch (Exception ignored) {}
        }

        // シンプルなマテリアル指定
        String materialPath = "base." + day + ".icon-material";
        if (config.contains(materialPath)) {
            try {
                Material material = Material.valueOf(config.getString(materialPath));
                ItemStack item = new ItemStack(material);
                
                // カスタムモデルデータを設定
                String customModelPath = "base." + day + ".custom-model-data";
                if (config.contains(customModelPath)) {
                    int customModelData = config.getInt(customModelPath);
                    org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.setCustomModelData(customModelData);
                        item.setItemMeta(meta);
                    }
                }
                
                return item;
            } catch (Exception ignored) {}
        }

        // 既存Base64方式（後方互換）
        String base64Path = "base." + day + ".icon";
        if (config.contains(base64Path) && config.getStringList(base64Path).isEmpty()) {
            try {
                return ItemStackSerializer.itemFromBase64(
                        config.getString(base64Path)
                );
            } catch (Exception ignored) {}
        }

        return null;
    }

    public org.bukkit.configuration.file.FileConfiguration getConfig() {
        return config;
    }

    public org.bukkit.configuration.file.FileConfiguration getStreakConfig() {
        return streakConfig;
    }
}
