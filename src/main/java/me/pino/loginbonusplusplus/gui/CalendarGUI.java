package me.pino.loginbonusplusplus.gui;

import me.pino.loginbonusplusplus.manager.PlayerDataManager;
import me.pino.loginbonusplusplus.manager.RewardManager;
import me.pino.loginbonusplusplus.model.PlayerData;
import me.pino.loginbonusplusplus.util.DateUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class CalendarGUI {

    private final JavaPlugin plugin;
    private final PlayerDataManager playerDataManager;
    private final RewardManager rewardManager;

    public CalendarGUI(JavaPlugin plugin,
                       PlayerDataManager playerDataManager,
                       RewardManager rewardManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
        this.rewardManager = rewardManager;
    }

    public void refreshCalendar(Player player) {
        // Close current inventory and reopen calendar
        player.closeInventory();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            open(player);
        }, 1L);
    }

    public void updateCalendar(Player player) {
        // GUIを即時更新（インベントリを再構築）
        if (player.getOpenInventory() != null && 
            player.getOpenInventory().getTitle().equals("§6Login Bonus Calendar")) {
            
            // インベントリを更新して表示をリフレッシュ
            player.updateInventory();
            
            // 少し遅延してから完全に再構築
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                refreshCalendar(player);
            }, 2L);
        }
    }

    public void open(Player player) {
        // Create 54-slot inventory
        Inventory inventory = Bukkit.createInventory(null, 54, "§6Login Bonus Calendar");

        // Get current month length
        int monthLength = DateUtil.getCurrentMonthLength();

        // Get player data and unlock day
        PlayerData data = playerDataManager.getPlayer(player.getUniqueId());
        int unlockDay = data.getMonthlyLoginCount();
        int monthly = data.getMonthlyLoginCount();
        int streak = data.getStreak();

        // Loop through days 1 to end of month
        for (int day = 1; day <= monthLength; day++) {
            // Try to get custom icon from rewards.yml
            ItemStack dayIcon = rewardManager.getDayIcon(day);
            ItemStack item;

            if (dayIcon != null) {
                // Use custom icon
                item = dayIcon.clone();
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    // Add reward lore first
                    List<String> lore = buildRewardLore(day);
                    
                    // Add claim status
                    if (data.hasClaimed(day)) {
                        lore.add("§aClaimed");
                    } else if (day == unlockDay) {
                        lore.add("§7Click to claim");
                    } else if (day < unlockDay) {
                        lore.add("§cMissed");
                    } else {
                        lore.add("§8Locked");
                    }

                    // Apply enchantment glow for today's claimable reward
                    if (canClaimToday(day, unlockDay, data)) {
                        meta.addEnchant(Enchantment.DURABILITY, 1, true);
                        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    }

                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
            } else {
                // Use traditional colored glass
                Material material;
                
                // Start with reward lore
                List<String> lore = buildRewardLore(day);

                // Determine material based on claim status
                int today = DateUtil.getCurrentDayOfMonth();
                
                if (data.hasClaimed(day)) {
                    material = Material.GREEN_STAINED_GLASS_PANE;
                    lore.add("§aClaimed");
                } else if (day == unlockDay && day == today) {
                    material = Material.YELLOW_STAINED_GLASS_PANE;
                    lore.add("§7Click to claim");
                } else if (day <= unlockDay && day < today) {
                    material = Material.RED_STAINED_GLASS_PANE;
                    lore.add("§cMissed");
                } else if (day <= unlockDay) {
                    material = Material.YELLOW_STAINED_GLASS_PANE;
                    lore.add("§7Click to claim");
                } else {
                    material = Material.GRAY_STAINED_GLASS_PANE;
                    lore.add("§8Locked");
                }

                // Create item with display name and lore
                item = new ItemStack(material);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§eDay " + day);
                    meta.setLore(lore);
                    
                    // Apply enchantment glow for today's claimable reward
                    if (canClaimToday(day, unlockDay, data)) {
                        meta.addEnchant(Enchantment.DURABILITY, 1, true);
                        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    }
                    
                    item.setItemMeta(meta);
                }
            }

            // Set item in inventory (day-1 because inventory starts at 0)
            inventory.setItem(day - 1, item);
        }

        // Add info display items (slots 45-49)
        addInfoDisplayItems(inventory, monthly, streak);

        // Open inventory for player
        player.openInventory(inventory);
    }

    private void addInfoDisplayItems(Inventory inventory, int monthly, int streak) {
        // Slot 45: Today's date
        ItemStack todayItem = new ItemStack(Material.CLOCK);
        ItemMeta todayMeta = todayItem.getItemMeta();
        if (todayMeta != null) {
            todayMeta.setDisplayName("§eToday");
            List<String> todayLore = new ArrayList<>();
            todayLore.add("§7Day: " + DateUtil.getCurrentDayOfMonth());
            todayMeta.setLore(todayLore);
            todayItem.setItemMeta(todayMeta);
        }
        inventory.setItem(45, todayItem);

        // Slot 46: Monthly login count
        ItemStack monthlyItem = new ItemStack(Material.BOOK);
        ItemMeta monthlyMeta = monthlyItem.getItemMeta();
        if (monthlyMeta != null) {
            monthlyMeta.setDisplayName("§eMonthly Logins");
            List<String> monthlyLore = new ArrayList<>();
            monthlyLore.add("§7Count: " + monthly);
            monthlyMeta.setLore(monthlyLore);
            monthlyItem.setItemMeta(monthlyMeta);
        }
        inventory.setItem(46, monthlyItem);

        // Slot 47: Streak (material based on streak value)
        Material streakMaterial = getStreakMaterial(streak);
        ItemStack streakItem = new ItemStack(streakMaterial);
        ItemMeta streakMeta = streakItem.getItemMeta();
        if (streakMeta != null) {
            streakMeta.setDisplayName("§eLogin Streak");
            List<String> streakLore = new ArrayList<>();
            streakLore.add("§7Current: " + streak + " days");
            streakMeta.setLore(streakLore);
            streakItem.setItemMeta(streakMeta);
        }
        inventory.setItem(47, streakItem);

        // Slot 48: Next streak reward
        ItemStack nextRewardItem = getNextStreakRewardDisplay(streak);
        inventory.setItem(48, nextRewardItem);

        // Slot 49: Days until next bonus
        ItemStack daysUntilItem = new ItemStack(Material.PAPER);
        ItemMeta daysUntilMeta = daysUntilItem.getItemMeta();
        if (daysUntilMeta != null) {
            daysUntilMeta.setDisplayName("§eNext Bonus");
            List<String> daysUntilLore = new ArrayList<>();
            int daysUntil = getDaysUntilNextStreak(streak);
            if (daysUntil > 0) {
                daysUntilLore.add("§7In: " + daysUntil + " days");
            } else {
                daysUntilLore.add("§7Available now!");
            }
            daysUntilMeta.setLore(daysUntilLore);
            daysUntilItem.setItemMeta(daysUntilMeta);
        }
        inventory.setItem(49, daysUntilItem);
    }

    private Material getStreakMaterial(int streak) {
        if (streak >= 30) {
            return Material.NETHER_STAR;
        } else if (streak >= 14) {
            return Material.DIAMOND;
        } else if (streak >= 7) {
            return Material.GOLD_INGOT;
        } else if (streak >= 3) {
            return Material.IRON_INGOT;
        } else {
            return Material.COAL;
        }
    }

    private ItemStack getNextStreakRewardDisplay(int currentStreak) {
        // Find the next streak milestone
        int[] milestones = {3, 7, 14, 30};
        int nextMilestone = -1;

        for (int milestone : milestones) {
            if (currentStreak < milestone) {
                nextMilestone = milestone;
                break;
            }
        }

        ItemStack item;
        if (nextMilestone == -1) {
            // No more milestones
            item = new ItemStack(Material.BARRIER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§eNext Streak Reward");
                List<String> lore = new ArrayList<>();
                lore.add("§7All milestones reached!");
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
        } else {
            // Show next milestone reward
            Material material = getStreakMaterial(nextMilestone);
            item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§eNext Streak Reward");
                List<String> lore = new ArrayList<>();
                lore.add("§7At: " + nextMilestone + " days");

                // Get actual reward items for this milestone
                List<ItemStack> rewardItems = rewardManager.getStreakRewards(nextMilestone);
                if (!rewardItems.isEmpty()) {
                    lore.add("§7Rewards:");
                    for (ItemStack rewardItem : rewardItems) {
                        if (rewardItem != null && rewardItem.getItemMeta() != null) {
                            String name = rewardItem.getItemMeta().hasDisplayName() ?
                                    rewardItem.getItemMeta().getDisplayName() :
                                    rewardItem.getType().name();
                            lore.add("§7- " + name + " x" + rewardItem.getAmount());
                        }
                    }
                } else {
                    lore.add("§7No rewards set");
                }
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
        }

        return item;
    }

    private int getDaysUntilNextStreak(int currentStreak) {
        int[] milestones = {3, 7, 14, 30};

        for (int milestone : milestones) {
            if (currentStreak < milestone) {
                return milestone - currentStreak;
            }
        }

        return 0; // All milestones reached
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

    private boolean canClaimToday(int day, int unlockDay, PlayerData data) {
        int today = DateUtil.getCurrentDayOfMonth();
        
        // 条件：
        // 1. 今日の日付である
        // 2. アンロック条件を満たしている
        // 3. まだ受取していない
        return day <= unlockDay && day == today && !data.hasClaimed(day);
    }
}
