package me.pino.loginbonusplusplus.listener;

import me.pino.loginbonusplusplus.manager.MessageManager;
import me.pino.loginbonusplusplus.manager.PlayerDataManager;
import me.pino.loginbonusplusplus.manager.RewardManager;
import me.pino.loginbonusplusplus.model.PlayerData;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CalendarClickListener implements Listener {

    private final JavaPlugin plugin;
    private final PlayerDataManager playerDataManager;
    private final RewardManager rewardManager;
    private final MessageManager messageManager;

    public CalendarClickListener(JavaPlugin plugin,
                                 PlayerDataManager playerDataManager,
                                 RewardManager rewardManager,
                                 MessageManager messageManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
        this.rewardManager = rewardManager;
        this.messageManager = messageManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {

        if (!event.getView().getTitle().equals("§6Login Bonus Calendar")) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player)) return;

        if (event.getClickedInventory() == null) return;

        event.setCancelled(true);

        if (!event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }

        //event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();

        int slot = event.getSlot();
        if (slot < 0 || slot >= 31) {
            return;
        }

        int day = slot + 1;
        PlayerData data = playerDataManager.getPlayer(player.getUniqueId());

        int unlockDay = data.getMonthlyLoginCount();

        if (day != unlockDay || data.hasClaimed(day)) {
            return;
        }

        // ===== すべての報酬を合算 =====
        List<ItemStack> allRewards = new ArrayList<>();

        allRewards.addAll(rewardManager.getBaseRewardItems(day));
        allRewards.addAll(rewardManager.getSpecialRewards(day));

        int streak = data.getStreak();
        List<ItemStack> streakItems = rewardManager.getStreakRewards(streak);
        allRewards.addAll(streakItems);

        // ===== 先に仮投入して空きチェック =====
        HashMap<Integer, ItemStack> leftover =
                player.getInventory().addItem(
                        allRewards.toArray(new ItemStack[0])
                );

        if (!leftover.isEmpty()) {
            // 入らなかった分を戻す（ロールバック）
            for (ItemStack item : allRewards) {
                player.getInventory().removeItem(item);
            }

            player.sendMessage(messageManager.get("inventory-full"));
            return;
        }

        // ===== 付与成功 =====
        if (!streakItems.isEmpty()) {
            player.sendMessage(messageManager.get("streak-bonus"));
        }

        data.addClaimedDay(day);
        playerDataManager.savePlayer(data);

        player.sendMessage(messageManager.get("claimed"));

        // ===== サウンド =====
        if (plugin.getConfig().getBoolean("sounds.claim.enabled", false)) {
            try {
                String soundName = plugin.getConfig().getString("sounds.claim.sound", "ENTITY_PLAYER_LEVELUP");
                Sound sound = Sound.valueOf(soundName);
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!event.getView().getTitle().equals("§6Login Bonus Calendar")) {
            return;
        }
        event.setCancelled(true);
    }
}