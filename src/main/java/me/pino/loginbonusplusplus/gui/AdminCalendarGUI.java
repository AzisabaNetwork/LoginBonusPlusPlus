package me.pino.loginbonusplusplus.gui;

import me.pino.loginbonusplusplus.manager.RewardManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class AdminCalendarGUI {

    private final JavaPlugin plugin;
    private final RewardManager rewardManager;

    public AdminCalendarGUI(JavaPlugin plugin, RewardManager rewardManager) {
        this.plugin = plugin;
        this.rewardManager = rewardManager;
    }

    public void open(Player player) {
        // Create 54-slot inventory
        Inventory inventory = Bukkit.createInventory(null, 54, "§cAdmin Reward Editor");

        // Create day buttons (1-31)
        for (int day = 1; day <= 31; day++) {
            ItemStack item = new ItemStack(Material.BOOK);
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                meta.setDisplayName("§eEdit Day " + day);
                meta.setLore(buildRewardLore(day));
                item.setItemMeta(meta);
            }

            // Set item in inventory (day-1 because inventory starts at 0)
            inventory.setItem(day - 1, item);
        }

        // Add streak rewards edit button at slot 45
        ItemStack streakButton = new ItemStack(Material.BLAZE_POWDER);
        ItemMeta streakMeta = streakButton.getItemMeta();
        if (streakMeta != null) {
            streakMeta.setDisplayName("§6Edit Streak Rewards");
            streakButton.setItemMeta(streakMeta);
        }
        inventory.setItem(45, streakButton);

        // Open inventory for player
        player.openInventory(inventory);
        
        // Play open sound
        playSound(player, "ui.button.click");
    }

    private void playSound(Player player, String soundName) {
        if (plugin.getConfig().getBoolean("sounds.enabled", true)) {
            try {
                Sound sound = Sound.valueOf(plugin.getConfig().getString("sounds.ui.sound", "UI_BUTTON_CLICK"));
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            } catch (IllegalArgumentException ignored) {
                // Fallback to default sound
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            }
        }
    }

    private List<String> buildRewardLore(int day) {
        List<String> lore = new ArrayList<>();
        
        // Get all rewards for this day
        List<ItemStack> allRewards = new ArrayList<>();
        allRewards.addAll(rewardManager.getBaseRewardItems(day));
        allRewards.addAll(rewardManager.getSpecialRewards(day));
        
        if (allRewards.isEmpty()) {
            lore.add("§7No rewards set");
        } else {
            lore.add("§7Rewards:");
            
            for (ItemStack item : allRewards) {
                String itemName;
                
                // Get display name or material name
                if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                    itemName = item.getItemMeta().getDisplayName();
                } else {
                    itemName = item.getType().name();
                }
                
                // Convert color codes
                itemName = ChatColor.translateAlternateColorCodes('&', itemName);
                
                // Format: §f<item name> §7x<amount>
                lore.add("§f" + itemName + " §7x" + item.getAmount());
            }
        }
        
        return lore;
    }
}
