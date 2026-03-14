package me.pino.loginbonusplusplus.gui;

import me.pino.loginbonusplusplus.LoginBonusPlusPlus;
import me.pino.loginbonusplusplus.manager.RewardManager;
import me.pino.loginbonusplusplus.util.ItemStackSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
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

    // Get all existing streak rewards from config
    private int[] getExistingStreaks() {
        List<Integer> streaks = new ArrayList<>();
        
        // Check rewards.yml for existing streak rewards
        if (rewardManager.getConfig().contains("streak")) {
            for (String key : rewardManager.getConfig().getConfigurationSection("streak").getKeys(false)) {
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

        Inventory inv = Bukkit.createInventory(null, 54, "§6Edit " + streak + " Day Streak Rewards");

        // Load current rewards
        List<ItemStack> rewards = editingRewards.get(player);
        for (int i = 0; i < rewards.size() && i < 45; i++) {
            inv.setItem(i, rewards.get(i));
        }

        // Add control buttons
        inv.setItem(45, createControlItem(Material.GREEN_STAINED_GLASS_PANE, "§aSave", "§7Save and return"));
        inv.setItem(46, createControlItem(Material.YELLOW_STAINED_GLASS_PANE, "§eClear", "§7Clear all rewards"));
        inv.setItem(47, createControlItem(Material.RED_STAINED_GLASS_PANE, "§cCancel", "§7Return without saving"));
        inv.setItem(53, createControlItem(Material.BARRIER, "§cBack", "§7Return to streak list"));

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        String title = event.getView().getTitle();
        if (!title.equals("§6Streak Rewards Editor") && !title.startsWith("§6Edit ")) {
            return;
        }

        event.setCancelled(true);
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }

        int slot = event.getSlot();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

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
            return;
        }

        // Handle streak editor clicks
        if (title.startsWith("§6Edit ")) {
            if (slot == 45) {
                // Save
                saveStreakRewards(player);
            } else if (slot == 46) {
                // Clear current streak rewards
                editingRewards.get(player).clear();
                openStreakEditor(player, editingStreak.get(player));
            } else if (slot == 47) {
                // Cancel
                editingStreak.remove(player);
                editingRewards.remove(player);
                open(player);
            } else if (slot == 53) {
                // Back
                editingStreak.remove(player);
                editingRewards.remove(player);
                open(player);
            } else if (slot < 45) {
                // Handle item slots (allow item movement)
                handleItemEdit(player, event);
            }
        }
    }

    private void handleItemEdit(Player player, InventoryClickEvent event) {
        int slot = event.getSlot();
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();

        if (cursor != null && cursor.getType() != Material.AIR) {
            // Place item from cursor
            if (current == null || current.getType() == Material.AIR) {
                event.setCurrentItem(cursor.clone());
                event.getWhoClicked().setItemOnCursor(null);
                editingRewards.get(player).set(slot, cursor.clone());
            }
        } else if (current != null && current.getType() != Material.AIR) {
            // Pick up item
            event.getWhoClicked().setItemOnCursor(current.clone());
            event.setCurrentItem(null);
            editingRewards.get(player).set(slot, null);
        }
    }

    private void saveStreakRewards(Player player) {
        int streak = editingStreak.get(player);
        List<ItemStack> rewards = editingRewards.get(player);

        // Remove null items
        rewards.removeIf(item -> item == null || item.getType() == Material.AIR);

        // Save to rewards.yml
        rewardManager.saveStreakRewards(streak, rewards);

        player.sendMessage("§aSaved " + rewards.size() + " rewards for " + streak + " day streak!");
        
        editingStreak.remove(player);
        editingRewards.remove(player);
        open(player);
    }

    private void showCurrentRewards(Player player) {
        player.sendMessage("§6=== Current Streak Rewards ===");
        for (int streak : STREAK_MILESTONES) {
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
        
        // Store player in waiting list for chat input
        // This would require a chat listener implementation
        // For now, we'll use a simple approach with existing commands
        player.sendMessage("§cUse: /lb debug set-streak <number> to create a new streak milestone");
    }
}
