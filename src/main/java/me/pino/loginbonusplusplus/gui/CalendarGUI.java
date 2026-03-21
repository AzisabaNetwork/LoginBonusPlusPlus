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
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class CalendarGUI implements Listener {

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
                        lore.add("§7Click to claim");
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
                    material = Material.YELLOW_STAINED_GLASS_PANE;
                    lore.add("§7Click to claim");
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

        // Add info display items (slots 45-48)
        addInfoDisplayItems(inventory, monthly, streak, player);

        // Open inventory for player
        player.openInventory(inventory);
    }

    private void addInfoDisplayItems(Inventory inventory, int monthly, int streak, Player player) {
        // Slot 45: Today's date
        ItemStack todayItem = new ItemStack(Material.CLOCK);
        ItemMeta todayMeta = todayItem.getItemMeta();
        if (todayMeta != null) {
            todayMeta.setDisplayName("§eToday");
            List<String> todayLore = new ArrayList<>();
            todayLore.add("§7Date: " + DateUtil.getCurrentMonth() + "/" + DateUtil.getCurrentDayOfMonth());
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

        // Slot 48: Streak reward claim button
        ItemStack streakClaimItem = createStreakClaimButton(streak, player);
        inventory.setItem(48, streakClaimItem);

        // Slot 49: Streak rewards list
        ItemStack streakList = createStreakRewardsList();
        inventory.setItem(49, streakList);
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

    private int[] getNextStreakReward(int currentStreak) {
        // Get all streak rewards from streak config
        List<Integer> streaks = new ArrayList<>();
        
        if (rewardManager.getStreakConfig().contains("streak")) {
            for (String key : rewardManager.getStreakConfig().getConfigurationSection("streak").getKeys(false)) {
                try {
                    int streak = Integer.parseInt(key);
                    if (streak > currentStreak) {
                        streaks.add(streak);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        
        if (streaks.isEmpty()) {
            return new int[]{0, 0}; // No more rewards
        }
        
        // Sort and find the next streak
        streaks.sort(Integer::compareTo);
        int nextStreak = streaks.get(0);
        int daysUntil = nextStreak - currentStreak;
        
        return new int[]{nextStreak, daysUntil};
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        String title = event.getView().getTitle();
        if (!title.equals("§6Login Bonus Calendar")) {
            return;
        }

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }

        int slot = event.getSlot();
        
        // Handle streak reward claim (slot 48)
        if (slot == 48) {
            event.setCancelled(true);
            claimStreakRewards(player);
        }
    }

    private void claimStreakRewards(Player player) {
        PlayerData data = playerDataManager.getPlayer(player.getUniqueId());
        int currentStreak = data.getStreak();
        
        // Get all available streak rewards
        List<Integer> availableStreaks = new ArrayList<>();
        if (rewardManager.getStreakConfig().contains("streak")) {
            for (String key : rewardManager.getStreakConfig().getConfigurationSection("streak").getKeys(false)) {
                try {
                    int streak = Integer.parseInt(key);
                    if (streak <= currentStreak) {
                        availableStreaks.add(streak);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        
        if (availableStreaks.isEmpty()) {
            player.sendMessage("§cNo streak rewards available to claim!");
            return;
        }
        
        // Check for unclaimed rewards using claimedDays (streak rewards use negative numbers)
        List<ItemStack> allRewards = new ArrayList<>();
        List<Integer> claimedStreaks = new ArrayList<>();
        
        for (int streak : availableStreaks) {
            // Use negative streak number to distinguish from day rewards in claimedDays
            if (!data.hasClaimed(-streak)) {
                List<ItemStack> streakRewards = rewardManager.getStreakRewards(streak);
                if (!streakRewards.isEmpty()) {
                    allRewards.addAll(streakRewards);
                    claimedStreaks.add(streak);
                }
            }
        }
        
        if (allRewards.isEmpty()) {
            player.sendMessage("§eAll available streak rewards have been claimed!");
            return;
        }
        
        // Give rewards
        for (ItemStack reward : allRewards) {
            player.getInventory().addItem(reward);
        }
        
        // Mark streaks as claimed using negative numbers
        for (int streak : claimedStreaks) {
            data.addClaimedDay(-streak);
        }
        
        playerDataManager.savePlayer(data);
        
        player.sendMessage("§aClaimed " + allRewards.size() + " streak rewards!");
        player.sendMessage("§eStreak rewards: " + claimedStreaks.stream().map(String::valueOf).reduce((a, b) -> a + ", " + b).orElse(""));
        
        // Refresh GUI
        open(player);
    }

    private ItemStack createStreakClaimButton(int streak, Player player) {
        // Get all available streak rewards
        List<Integer> availableStreaks = new ArrayList<>();
        List<ItemStack> allRewards = new ArrayList<>();
        
        if (rewardManager.getStreakConfig().contains("streak")) {
            for (String key : rewardManager.getStreakConfig().getConfigurationSection("streak").getKeys(false)) {
                try {
                    int streakNum = Integer.parseInt(key);
                    if (streakNum <= streak) {
                        availableStreaks.add(streakNum);
                        List<ItemStack> streakRewards = rewardManager.getStreakRewards(streakNum);
                        allRewards.addAll(streakRewards);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        
        // Check for unclaimed rewards
        List<Integer> unclaimedStreaks = new ArrayList<>();
        PlayerData data = playerDataManager.getPlayer(player.getUniqueId());
        
        for (int streakNum : availableStreaks) {
            if (!data.hasClaimed(-streakNum)) {
                unclaimedStreaks.add(streakNum);
            }
        }
        
        ItemStack item;
        ItemMeta meta;
        
        if (unclaimedStreaks.isEmpty()) {
            // No rewards to claim
            item = new ItemStack(Material.PAPER);
            meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§c§l次の連続ログイン報酬");
                List<String> lore = new ArrayList<>();
                
                // Show next streak reward info
                int[] nextStreakInfo = getNextStreakReward(streak);
                int nextStreak = nextStreakInfo[0];
                
                if (nextStreak > 0) {
                    int daysUntil = nextStreak - streak;
                    lore.add("§7次の報酬まで: §e" + daysUntil + "日");
                    
                    List<ItemStack> nextRewards = rewardManager.getStreakRewards(nextStreak);
                    if (!nextRewards.isEmpty()) {
                        lore.add("§7次の報酬:");
                        for (ItemStack reward : nextRewards) {
                            String name = reward.hasItemMeta() && reward.getItemMeta().hasDisplayName() 
                                ? reward.getItemMeta().getDisplayName() 
                                : reward.getType().name();
                            lore.add("§f- " + name + " §7x" + reward.getAmount());
                        }
                    }
                } else {
                    lore.add("§7報酬はありません");
                }
                
                meta.setLore(lore);
            }
        } else {
            // Has rewards to claim
            item = new ItemStack(Material.GOLD_BLOCK);
            meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§3§lログインストリーク報酬");
                List<String> lore = new ArrayList<>();
                lore.add("§a§l受け取れる報酬があります！");
                
                // Show next streak info
                int[] nextStreakInfo = getNextStreakReward(streak);
                int nextStreak = nextStreakInfo[0];
                
                if (nextStreak > 0 && !unclaimedStreaks.contains(nextStreak)) {
                    int daysUntil = nextStreak - streak;
                    lore.add("§7次の報酬まで: §e" + daysUntil + "日");
                }
                
                // Show all unclaimed rewards
                lore.add("§7受け取れる報酬:");
                for (int streakNum : unclaimedStreaks) {
                    List<ItemStack> streakRewards = rewardManager.getStreakRewards(streakNum);
                    for (ItemStack reward : streakRewards) {
                        String name = reward.hasItemMeta() && reward.getItemMeta().hasDisplayName() 
                            ? reward.getItemMeta().getDisplayName() 
                            : reward.getType().name();
                        lore.add("§f- " + name + " §7x" + reward.getAmount());
                    }
                }
                
                meta.setLore(lore);
                meta.addEnchant(org.bukkit.enchantments.Enchantment.DURABILITY, 1, true);
            }
        }
        
        if (meta != null) {
            item.setItemMeta(meta);
        }
        
        return item;
    }

    private ItemStack createStreakRewardsList() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§lストリーク報酬一覧");
            List<String> lore = new ArrayList<>();
            lore.add("§7全ストリーク報酬:");
            
            // Get all streak rewards from config
            if (rewardManager.getStreakConfig().contains("streak")) {
                List<Integer> streaks = new ArrayList<>();
                for (String key : rewardManager.getStreakConfig().getConfigurationSection("streak").getKeys(false)) {
                    try {
                        streaks.add(Integer.parseInt(key));
                    } catch (NumberFormatException ignored) {}
                }
                
                // Sort streaks
                streaks.sort(Integer::compareTo);
                
                for (int streak : streaks) {
                    List<ItemStack> rewards = rewardManager.getStreakRewards(streak);
                    if (!rewards.isEmpty()) {
                        lore.add("§e" + streak + "日:");
                        for (ItemStack reward : rewards) {
                            String name = reward.hasItemMeta() && reward.getItemMeta().hasDisplayName() 
                                ? reward.getItemMeta().getDisplayName() 
                                : reward.getType().name();
                            lore.add("  §f- " + name + " §7x" + reward.getAmount());
                        }
                    }
                }
            } else {
                lore.add("§c報酬が設定されていません");
            }
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
