package me.pino.loginbonusplusplus.gui;

import me.pino.loginbonusplusplus.LoginBonusPlusPlus;
import me.pino.loginbonusplusplus.manager.RewardManager;
import me.pino.loginbonusplusplus.util.ItemStackSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StreakRewardEditGUI implements Listener {

    private final LoginBonusPlusPlus plugin;
    private final RewardManager rewardManager;
    private final Map<Player, Integer> editingStreak = new HashMap<>();
    private final Map<Player, List<ItemStack>> editingRewards = new HashMap<>();
    private final Map<Player, Boolean> waitingForChatInput = new HashMap<>();

    // Get all existing streak rewards from config
    private int[] getExistingStreaks() {
        List<Integer> streaks = new ArrayList<>();
        
        // Check streakrewards.yml for existing streak rewards
        if (rewardManager.getStreakConfig().contains("streak")) {
            for (String key : rewardManager.getStreakConfig().getConfigurationSection("streak").getKeys(false)) {
                try {
                    streaks.add(Integer.parseInt(key));
                } catch (NumberFormatException ignored) {}
            }
        }
        
        // Add common milestones if not exist
        int[] common = {3, 5, 7, 10, 14, 21, 30, 50, 100, 365};
        for (int streak : common) {
            if (!streaks.contains(streak)) {
                streaks.add(streak);
            }
        }
        
        // Sort and return as array
        streaks.sort(Integer::compareTo);
        return streaks.stream().mapToInt(i -> i).toArray();
    }

    public StreakRewardEditGUI(LoginBonusPlusPlus plugin, RewardManager rewardManager) {
        this.plugin = plugin;
        this.rewardManager = rewardManager;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§6Streak Rewards Editor");

        int[] existingStreaks = getExistingStreaks();

        // Add existing streak buttons
        for (int i = 0; i < existingStreaks.length && i < 45; i++) {
            int streak = existingStreaks[i];
            ItemStack item = createStreakButton(streak);
            inv.setItem(i, item);
        }

        // Add control buttons
        inv.setItem(45, createControlItem(Material.BOOK, "§eCurrent Rewards", "§7Click to view current streak rewards"));
        inv.setItem(46, createControlItem(Material.ANVIL, "§bAdd New Streak", "§7Add a new streak milestone"));
        inv.setItem(47, createControlItem(Material.REDSTONE, "§cClear All", "§7Clear all streak rewards"));
        inv.setItem(53, createControlItem(Material.BARRIER, "§cBack", "§7Return to admin menu"));

        player.openInventory(inv);
    }

    private ItemStack createStreakButton(int streak) {
        ItemStack item = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6" + streak + " Day Streak");
            
            List<String> lore = new ArrayList<>();
            lore.add("§7Click to edit rewards");
            lore.add("§7for " + streak + " day streak");
            
            // Show current rewards count
            List<ItemStack> currentRewards = rewardManager.getStreakRewards(streak);
            lore.add("§7Current rewards: §e" + currentRewards.size());
            
            if (!currentRewards.isEmpty()) {
                lore.add("§7Rewards:");
                for (ItemStack reward : currentRewards) {
                    String name = reward.hasItemMeta() && reward.getItemMeta().hasDisplayName() 
                        ? reward.getItemMeta().getDisplayName() 
                        : reward.getType().name();
                    lore.add("§f- " + name + " §7x" + reward.getAmount());
                }
            }
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createControlItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    public void openStreakEditor(Player player, int streak) {
        editingStreak.put(player, streak);
        editingRewards.put(player, new ArrayList<>(rewardManager.getStreakRewards(streak)));

        Inventory inv = Bukkit.createInventory(null, 54, "§cEdit Streak Rewards - " + streak + " Days");

        // Load current rewards
        List<ItemStack> rewards = editingRewards.get(player);
        for (int i = 0; i < rewards.size() && i < 45; i++) {
            inv.setItem(i, rewards.get(i));
        }

        // Add control buttons (matching DayRewardEditGUI design)
        addControlButtons(inv);

        player.openInventory(inv);
        
        // Play open sound
        playSound(player, "ui");
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        String title = event.getView().getTitle();
        if (!title.equals("§6Streak Rewards Editor") && !title.startsWith("§cEdit Streak Rewards - ")) {
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

        // Handle main menu clicks
        if (title.equals("§6Streak Rewards Editor")) {
            int[] existingStreaks = getExistingStreaks();
            
            if (slot < existingStreaks.length && slot < 45) {
                int streak = existingStreaks[slot];
                openStreakEditor(player, streak);
            } else if (slot == 45) {
                // Show current rewards
                showCurrentRewards(player);
            } else if (slot == 46) {
                // Add new streak
                promptForNewStreak(player);
            } else if (slot == 47) {
                // Clear all rewards
                clearAllStreakRewards(player);
            } else if (slot == 53) {
                // Back to admin menu
                plugin.getAdminCalendarGUI().open(player);
            }
            event.setCancelled(true);
            return;
        }

        // Handle streak editor clicks (matching DayRewardEditGUI logic)
        if (title.startsWith("§cEdit Streak Rewards - ")) {
            if (slot == 45) {
                // Save button
                event.setCancelled(true);
                playSound(player, "success");
                saveStreakRewards(player);
                return;
            } else if (slot == 46) {
                // Clear button
                event.setCancelled(true);
                playSound(player, "ui");
                editingRewards.get(player).clear();
                openStreakEditor(player, editingStreak.get(player));
                return;
            } else if (slot == 47) {
                // Cancel button
                event.setCancelled(true);
                playSound(player, "cancel");
                editingStreak.remove(player);
                editingRewards.remove(player);
                open(player);
                return;
            } else if (slot == 53) {
                // Back button
                event.setCancelled(true);
                playSound(player, "cancel");
                editingStreak.remove(player);
                editingRewards.remove(player);
                open(player);
                return;
            } else if (slot >= 0 && slot <= 44) {
                // Editable slots - allow item movement (don't cancel)
                return;
            } else if (slot >= 48 && slot <= 52) {
                // Empty slots - cancel interaction
                event.setCancelled(true);
                return;
            }

            // For all other slots, cancel the event
            event.setCancelled(true);
        }
    }

    private void saveStreakRewards(Player player) {
        int streak = editingStreak.get(player);

        // Get items from editable slots (0-44 only) - matching DayRewardEditGUI logic
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i <= 44; i++) {
            ItemStack item = player.getOpenInventory().getTopInventory().getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                items.add(item);
            }
        }

        // Save to streakrewards.yml
        rewardManager.saveStreakRewards(streak, items);

        player.sendMessage("§a" + streak + "日ストリークの報酬を保存しました！");
        playSound(player, "success");
        
        editingStreak.remove(player);
        editingRewards.remove(player);
        open(player);
    }

    private void showCurrentRewards(Player player) {
        player.sendMessage("§6=== Current Streak Rewards ===");
        int[] existingStreaks = getExistingStreaks();
        for (int streak : existingStreaks) {
            List<ItemStack> rewards = rewardManager.getStreakRewards(streak);
            if (!rewards.isEmpty()) {
                player.sendMessage("§e" + streak + " days: §f" + rewards.size() + " rewards");
                for (ItemStack reward : rewards) {
                    String name = reward.hasItemMeta() && reward.getItemMeta().hasDisplayName() 
                        ? reward.getItemMeta().getDisplayName() 
                        : reward.getType().name();
                    player.sendMessage("  §f- " + name + " §7x" + reward.getAmount());
                }
            }
        }
    }

    private void clearAllStreakRewards(Player player) {
        int[] existingStreaks = getExistingStreaks();
        for (int streak : existingStreaks) {
            rewardManager.saveStreakRewards(streak, new ArrayList<>());
        }
        player.sendMessage("§aCleared all streak rewards!");
        open(player);
    }

    private void promptForNewStreak(Player player) {
        player.sendMessage("§ePlease enter the streak number in chat (e.g., 15, 25, 40)");
        player.sendMessage("§7Type 'cancel' to cancel");
        
        // Mark player as waiting for chat input
        waitingForChatInput.put(player, true);
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

        // Slot 46: Clear button
        ItemStack clearButton = new ItemStack(Material.YELLOW_CONCRETE);
        ItemMeta clearMeta = clearButton.getItemMeta();
        if (clearMeta != null) {
            clearMeta.setDisplayName("§eクリア");
            clearButton.setItemMeta(clearMeta);
        }
        inventory.setItem(46, clearButton);

        // Slot 47: Cancel button
        ItemStack cancelButton = new ItemStack(Material.RED_CONCRETE);
        ItemMeta cancelMeta = cancelButton.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.setDisplayName("§c保存せず戻る");
            cancelButton.setItemMeta(cancelMeta);
        }
        inventory.setItem(47, cancelButton);

        // Slots 48-53: Leave empty
    }

    private void playSound(Player player, String soundType) {
        try {
            String soundName;
            switch (soundType.toLowerCase()) {
                case "success":
                    soundName = "ENTITY_PLAYER_LEVELUP";
                    break;
                case "cancel":
                    soundName = "UI_BUTTON_CLICK";
                    break;
                default:
                    soundName = "UI_BUTTON_CLICK";
                    break;
            }
            
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException ignored) {
            // Fallback to default sound
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        }
    }
    
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        
        // Check if player is waiting for input
        if (waitingForChatInput.containsKey(player) && waitingForChatInput.get(player)) {
            event.setCancelled(true);
            
            String message = event.getMessage().trim();
            
            // Handle cancel
            if (message.equalsIgnoreCase("cancel")) {
                waitingForChatInput.remove(player);
                player.sendMessage("§cCancelled streak creation.");
                return;
            }
            
            // Try to parse streak number
            try {
                int streakNumber = Integer.parseInt(message);
                
                if (streakNumber < 1) {
                    player.sendMessage("§cStreak number must be positive!");
                    return;
                }
                
                // Create new streak
                editingStreak.put(player, streakNumber);
                
                // Load existing rewards for this streak (if any)
                List<ItemStack> existingRewards = rewardManager.getStreakRewards(streakNumber);
                editingRewards.put(player, new ArrayList<>(existingRewards));
                waitingForChatInput.remove(player);
                
                player.sendMessage("§aCreated new streak milestone: " + streakNumber + " days");
                if (existingRewards.isEmpty()) {
                    player.sendMessage("§7Now add items to the inventory and click save.");
                } else {
                    player.sendMessage("§7Loaded " + existingRewards.size() + " existing rewards. You can modify them.");
                }
                
                // Open editing GUI for this streak
                openStreakEditor(player, streakNumber);
                
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid number! Please enter a valid streak number (e.g., 15, 25, 40)");
            }
        }
    }
}
