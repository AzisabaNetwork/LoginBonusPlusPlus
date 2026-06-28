package me.pino.loginbonusplusplus.listener;

import me.pino.loginbonusplusplus.LoginBonusPlusPlus;
import me.pino.loginbonusplusplus.gui.CalendarGUI;
import me.pino.loginbonusplusplus.manager.MessageManager;
import me.pino.loginbonusplusplus.manager.PlayerDataManager;
import me.pino.loginbonusplusplus.manager.RewardManager;
import me.pino.loginbonusplusplus.model.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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
        if (!event.getView().getTitle().equals("§6Login Bonus Calendar")) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getClickedInventory() == null) return;

        event.setCancelled(true);

        if (!event.getClickedInventory().equals(event.getView().getTopInventory())) return;

        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        // slot 53: 補填券預け・引き出し（slot >= 31チェックより先に処理）
        if (slot == 53) {
            handleFreezeTicket(player, event.isRightClick());
            return;
        }

        // slot 0〜30: 日報酬
        if (slot < 0 || slot >= 31) return;

        int day = slot + 1;
        PlayerData data = playerDataManager.getPlayer(player.getUniqueId());
        int unlockDay = data.getMonthlyLoginCount();

        if (day > unlockDay || data.hasClaimed(day)) return;

        // ===== すべての報酬を合算 =====
        List<ItemStack> allRewards = new ArrayList<>();
        allRewards.addAll(rewardManager.getBaseRewardItems(day));
        allRewards.addAll(rewardManager.getSpecialRewards(day));

        // ===== 空きチェック =====
        HashMap<Integer, ItemStack> leftover =
                player.getInventory().addItem(allRewards.toArray(new ItemStack[0]));

        if (!leftover.isEmpty()) {
            // 入った分だけ正確にロールバック
            HashMap<Integer, ItemStack> added = new HashMap<>();
            for (int i = 0; i < allRewards.size(); i++) {
                if (!leftover.containsKey(i)) {
                    added.put(i, allRewards.get(i));
                }
            }
            for (ItemStack item : added.values()) {
                player.getInventory().removeItem(item);
            }
            player.sendMessage(messageManager.get("inventory-full"));
            return;
        }

        data.addClaimedDay(day);
        playerDataManager.savePlayer(data);
        player.sendMessage("§eClaimed!");
        player.closeInventory();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                CalendarGUI calendarGUI = ((LoginBonusPlusPlus) plugin).getCalendarGUI();
                calendarGUI.open(player);
            } catch (Exception e) {
                player.sendMessage("§cError reopening calendar. Please open it manually.");
            }
        }, 2L);

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
        if (!event.getView().getTitle().equals("§6Login Bonus Calendar")) return;
        event.setCancelled(true);
    }

    private void handleFreezeTicket(Player player, boolean rightClick) {
        PlayerData data = playerDataManager.getPlayer(player.getUniqueId());
        String name = plugin.getConfig().getString("freeze-item.name", "ログイン補填券");

        Material mat;
        try {
            mat = Material.valueOf(plugin.getConfig().getString("freeze-item.material", "PAPER"));
        } catch (IllegalArgumentException e) {
            mat = Material.PAPER;
        }

        if (rightClick) {
            // 引き出し
            if (data.getFreezeTickets() <= 0) {
                player.sendMessage("§c預けている補填券がありません。");
                return;
            }
            ItemStack ticket = new ItemStack(mat);
            ItemMeta meta = ticket.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(name);
                List<String> lore = new ArrayList<>();
                lore.add("§71日だけ途切れた場合に自動消費されます");
                lore.add("§c2日以上空いた場合は消費されません");
                lore.add("§a§l");
                lore.add("§a§lこれは実験的な要素です");
                meta.setLore(lore);
                ticket.setItemMeta(meta);
            }
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(ticket);
            if (!leftover.isEmpty()) {
                player.sendMessage("§cインベントリが満杯です！");
                return;
            }
            data.consumeFreezeTicket();
            player.sendMessage("§e補填券を1枚引き出しました。(残り: §b" + data.getFreezeTickets() + "§e枚)");

        } else {
            // 預け入れ
            if (data.getFreezeTickets() >= 64) {
                player.sendMessage("§c補填券は64枚までしか預けられません。");
                return;
            }
            ItemStack[] contents = player.getInventory().getContents();
            boolean found = false;
            for (int i = 0; i < contents.length; i++) {
                ItemStack item = contents[i];
                if (item == null || item.getType() == Material.AIR) continue;
                if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) continue;
                if (!item.getItemMeta().getDisplayName().equals(name)) continue;

                int canDeposit = Math.min(item.getAmount(), 64 - data.getFreezeTickets());
                data.addFreezeTicket(canDeposit);

                if (item.getAmount() > canDeposit) {
                    item.setAmount(item.getAmount() - canDeposit);
                } else {
                    player.getInventory().setItem(i, null);
                }
                player.sendMessage("§a補填券を§b" + canDeposit + "§a枚預けました。(合計: §b" + data.getFreezeTickets() + "§a枚)");
                found = true;
                break;
            }
            if (!found) {
                player.sendMessage("§cインベントリに補填券がありません。");
            }
        }

        playerDataManager.savePlayer(data);
        ((LoginBonusPlusPlus) plugin).getCalendarGUI().open(player);
    }
}