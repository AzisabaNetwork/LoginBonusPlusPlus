package me.pino.loginbonusplusplus.gui;

import org.bukkit.Bukkit;
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

    public AdminCalendarGUI(JavaPlugin plugin) {
        this.plugin = plugin;
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
}
