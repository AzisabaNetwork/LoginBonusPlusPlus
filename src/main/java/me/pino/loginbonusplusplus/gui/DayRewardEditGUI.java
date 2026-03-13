package me.pino.loginbonusplusplus.gui;

import me.pino.loginbonusplusplus.manager.MessageManager;
import me.pino.loginbonusplusplus.manager.RewardManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class DayRewardEditGUI implements Listener {

    private final RewardManager rewardManager;
    private final AdminCalendarGUI adminCalendarGUI;
    private final MessageManager messageManager;
    private final JavaPlugin plugin;

    public DayRewardEditGUI(RewardManager rewardManager, AdminCalendarGUI adminCalendarGUI, MessageManager messageManager) {
        this.rewardManager = rewardManager;
        this.adminCalendarGUI = adminCalendarGUI;
        this.messageManager = messageManager;
        // Plugin reference will be set by AdminCalendarGUI
        this.plugin = null;
    }

    public DayRewardEditGUI(RewardManager rewardManager, AdminCalendarGUI adminCalendarGUI, MessageManager messageManager, JavaPlugin plugin) {
        this.rewardManager = rewardManager;
        this.adminCalendarGUI = adminCalendarGUI;
        this.messageManager = messageManager;
        this.plugin = plugin;
    }

    public void open(Player player, int day) {
        // Create 54-slot inventory with day-specific title
        Inventory inventory = Bukkit.createInventory(null, 54, "§cEdit Rewards - Day " + day);

        // Load existing rewards into inventory (slots 0-44 only)
        List<ItemStack> existingRewards = rewardManager.getBaseRewardItems(day);
        for (int i = 0; i < existingRewards.size() && i < 45; i++) {
            inventory.setItem(i, existingRewards.get(i));
        }

        // Add control buttons
        addControlButtons(inventory);

        player.openInventory(inventory);
        
        // Play open sound
        playSound(player, "ui.button.click");
    }

    private void addControlButtons(Inventory inventory) {
        // Slot 45: Save button
        ItemStack saveButton = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta saveMeta = saveButton.getItemMeta();
        if (saveMeta != null) {
            saveMeta.setDisplayName("§a保存して戻る");
            saveButton.setItemMeta(saveMeta);
        }
        inventory.setItem(45, saveButton);

        // Slot 46: Cancel button
        ItemStack cancelButton = new ItemStack(Material.RED_CONCRETE);
        ItemMeta cancelMeta = cancelButton.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.setDisplayName("§c保存せず戻る");
            cancelButton.setItemMeta(cancelMeta);
        }
        inventory.setItem(46, cancelButton);

        // Slots 47-53: Leave empty
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();

        // Check if this is our reward edit GUI
        if (!title.startsWith("§cEdit Rewards - Day ")) {
            return;
        }

        // 1) null check for clicked inventory
        if (event.getClickedInventory() == null) {
            return;
        }

        // 2) Allow clicks on bottom inventory (player inventory) for item movement
        if (!event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return; // Don't cancel - allow player inventory interaction
        }

        int slot = event.getSlot();
        Player player = (Player) event.getWhoClicked();

        // Handle control buttons
        if (slot == 45) {
            // Save button
            event.setCancelled(true);
            playSound(player, "success");
            saveAndReturn(player, title);
            return;
        } else if (slot == 46) {
            // Cancel button
            event.setCancelled(true);
            playSound(player, "cancel");
            adminCalendarGUI.open(player);
            return;
        } else if (slot >= 0 && slot <= 44) {
            // Editable slots - allow item movement (don't cancel)
            return;
        } else if (slot >= 47 && slot <= 53) {
            // Empty slots - cancel interaction
            event.setCancelled(true);
            return;
        }

        // For all other slots, cancel the event
        event.setCancelled(true);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();

        // Check if this is our reward edit GUI
        if (!title.startsWith("§cEdit Rewards - Day ")) {
            return;
        }

        // Cancel if any dragged slot is outside top inventory size
        int topInventorySize = event.getView().getTopInventory().getSize();
        for (Integer slot : event.getInventorySlots()) {
            if (slot >= topInventorySize) {
                event.setCancelled(true);
                return;
            }
        }
        // Allow dragging within top inventory
    }

    private void saveAndReturn(Player player, String title) {
        try {
            // Extract day number from title
            String dayString = title.substring("§cEdit Rewards - Day ".length());
            int day = Integer.parseInt(dayString.trim());

            // Get items from editable slots (0-44 only)
            List<ItemStack> items = new ArrayList<>();
            for (int i = 0; i <= 44; i++) {
                ItemStack item = player.getOpenInventory().getTopInventory().getItem(i);
                if (item != null) {
                    items.add(item);
                }
            }

            // Save items to rewards.yml
            rewardManager.saveDayRewards(day, items);
            player.sendMessage("§a報酬を保存しました！");


        } catch (NumberFormatException e) {
            player.sendMessage("§cエラー: 無効な日付番号です");

        } catch (Exception e) {
            player.sendMessage("§c報酬の保存中にエラーが発生しました");

            e.printStackTrace();
        }

        // Return to admin calendar
        adminCalendarGUI.open(player);
    }

    private void playSound(Player player, String soundType) {
        if (plugin == null) return;
        
        if (plugin.getConfig().getBoolean("sounds.enabled", true)) {
            try {
                String soundName;
                switch (soundType.toLowerCase()) {
                    case "success":
                        soundName = plugin.getConfig().getString("sounds.success.sound", "ENTITY_PLAYER_LEVELUP");
                        break;
                    case "cancel":
                        soundName = plugin.getConfig().getString("sounds.cancel.sound", "UI_BUTTON_CLICK");
                        break;
                    default:
                        soundName = plugin.getConfig().getString("sounds.ui.sound", "UI_BUTTON_CLICK");
                        break;
                }
                
                Sound sound = Sound.valueOf(soundName);
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            } catch (IllegalArgumentException ignored) {
                // Fallback to default sound
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            }
        }
    }
}
