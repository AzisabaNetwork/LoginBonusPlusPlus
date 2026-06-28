package me.pino.loginbonusplusplus.gui;

import me.pino.loginbonusplusplus.manager.RewardManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class AdminCalendarGUI implements Listener {

    private final JavaPlugin plugin;
    private final RewardManager rewardManager;
    private final ClaimableIconEditGUI claimableIconEditGUI;
    private StreakRewardEditGUI streakRewardEditGUI;

    public AdminCalendarGUI(JavaPlugin plugin, RewardManager rewardManager,
                            ClaimableIconEditGUI claimableIconEditGUI) {
        this.plugin = plugin;
        this.rewardManager = rewardManager;
        this.claimableIconEditGUI = claimableIconEditGUI;
    }

    // StreakRewardEditGUI は循環依存を避けるため setter で渡す
    public void setStreakRewardEditGUI(StreakRewardEditGUI streakRewardEditGUI) {
        this.streakRewardEditGUI = streakRewardEditGUI;
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, "§cAdmin Reward Editor");

        for (int day = 1; day <= 31; day++) {
            ItemStack item = new ItemStack(Material.BOOK);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§eEdit Day " + day);
                meta.setLore(buildRewardLore(day));
                item.setItemMeta(meta);
            }
            inventory.setItem(day - 1, item);
        }

        ItemStack streakButton = new ItemStack(Material.BLAZE_POWDER);
        ItemMeta streakMeta = streakButton.getItemMeta();
        if (streakMeta != null) {
            streakMeta.setDisplayName("§6Edit Streak Rewards");
            streakButton.setItemMeta(streakMeta);
        }
        inventory.setItem(45, streakButton);

        ItemStack claimIconBtn = new ItemStack(Material.NETHER_STAR);
        ItemMeta claimIconMeta = claimIconBtn.getItemMeta();
        if (claimIconMeta != null) {
            claimIconMeta.setDisplayName("§bEdit Claimable Icons");
            claimIconMeta.setLore(java.util.Arrays.asList(
                    "§7受取可能時・受取済みの",
                    "§7カスタムアイコンを設定"));
            claimIconBtn.setItemMeta(claimIconMeta);
        }
        inventory.setItem(46, claimIconBtn);

        player.openInventory(inventory);
        playSound(player);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!event.getView().getTitle().equals("§cAdmin Reward Editor")) return;

        if (event.getClickedInventory() == null ||
                !event.getClickedInventory().equals(event.getView().getTopInventory())) return;

        // slot 45はAdminCalendarClickListenerが処理するのでここでは触らない
        if (event.getSlot() == 46) {
            event.setCancelled(true);
            claimableIconEditGUI.open((Player) event.getWhoClicked());
        }
    }

    private void playSound(Player player) {
        if (plugin.getConfig().getBoolean("sounds.enabled", true)) {
            try {
                Sound sound = Sound.valueOf(
                        plugin.getConfig().getString("sounds.ui.sound", "UI_BUTTON_CLICK"));
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            } catch (IllegalArgumentException ignored) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            }
        }
    }

    private List<String> buildRewardLore(int day) {
        List<String> lore = new ArrayList<>();
        List<ItemStack> allRewards = new ArrayList<>();
        allRewards.addAll(rewardManager.getBaseRewardItems(day));
        allRewards.addAll(rewardManager.getSpecialRewards(day));

        if (allRewards.isEmpty()) {
            lore.add("§7No rewards set");
        } else {
            lore.add("§7Rewards:");
            for (ItemStack item : allRewards) {
                String itemName = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                        ? item.getItemMeta().getDisplayName()
                        : item.getType().name();
                itemName = ChatColor.translateAlternateColorCodes('&', itemName);
                lore.add("§f" + itemName + " §7x" + item.getAmount());
            }
        }
        return lore;
    }
}