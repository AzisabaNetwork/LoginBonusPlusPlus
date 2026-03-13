package me.pino.loginbonusplusplus.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class AdminCalendarGUI {

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
    }
}
