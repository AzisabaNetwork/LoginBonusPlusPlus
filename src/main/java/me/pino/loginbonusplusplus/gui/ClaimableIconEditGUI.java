package me.pino.loginbonusplusplus.gui;

import me.pino.loginbonusplusplus.LoginBonusPlusPlus;
import me.pino.loginbonusplusplus.manager.RewardManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class ClaimableIconEditGUI implements Listener {

    private final LoginBonusPlusPlus plugin;
    private final RewardManager rewardManager;

    public ClaimableIconEditGUI(LoginBonusPlusPlus plugin, RewardManager rewardManager) {
        this.plugin = plugin;
        this.rewardManager = rewardManager;
    }

    // 日選択画面（54スロット、1〜31日）
    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§bClaimable Icon Editor");
        for (int day = 1; day <= 31; day++) {
            ItemStack current = rewardManager.getClaimableIcon(day);
            ItemStack btn = current != null ? current.clone() : new ItemStack(Material.PAPER);
            ItemMeta meta = btn.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§eDay " + day);
                meta.setLore(Arrays.asList(
                        current != null ? "§a設定済" : "§7未設定",
                        "§7クリックで編集"
                ));
                btn.setItemMeta(meta);
            }
            inv.setItem(day - 1, btn);
        }
        inv.setItem(53, make(Material.BARRIER, "§c戻る", "§7Admin メニューへ"));
        player.openInventory(inv);
    }

    // アイコン編集画面（9スロット）
    public void openEditor(Player player, int day) {
        Inventory inv = Bukkit.createInventory(null, 9,
                "§bClaimable Icon - Day " + day);

        // slot 0: アイコン置き場（現在の設定があれば表示）
        ItemStack current = rewardManager.getClaimableIcon(day);
        if (current != null) inv.setItem(0, current.clone());

        // slot 2: 説明
        inv.setItem(2, make(Material.OAK_SIGN, "§f使い方",
                "§7slot 0 にアイコンを置いて",
                "§7「保存」を押してください",
                "§7カスタムモデルデータ対応"));

        // slot 4: 保存
        inv.setItem(4, make(Material.LIME_CONCRETE, "§a保存して戻る"));
        // slot 5: 削除
        inv.setItem(5, make(Material.RED_CONCRETE,  "§cアイコン削除"));
        // slot 8: 戻る
        inv.setItem(8, make(Material.BARRIER,       "§c保存せず戻る"));

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        // 日選択画面
        if (title.equals("§bClaimable Icon Editor")) {
            event.setCancelled(true);
            int slot = event.getSlot();
            if (slot == 53) {
                plugin.getAdminCalendarGUI().open(player);
                return;
            }
            if (slot >= 0 && slot < 31) {
                openEditor(player, slot + 1);
            }
            return;
        }

        // 編集画面
        if (title.startsWith("§bClaimable Icon - Day ")) {
            int day = Integer.parseInt(title.replace("§bClaimable Icon - Day ", ""));
            int slot = event.getSlot();

            // 下段（プレイヤーインベントリ）は自由に操作させる
            if (event.getClickedInventory() == null ||
                    !event.getClickedInventory().equals(event.getView().getTopInventory())) {
                return;
            }

            // 上段 slot 0 のみアイテム自由配置
            if (slot == 0) return;

            event.setCancelled(true);
            switch (slot) {
                case 4: // 保存
                    ItemStack placed = event.getView().getTopInventory().getItem(0);
                    if (placed == null || placed.getType() == Material.AIR) {
                        player.sendMessage("§cslot 0 にアイコンを置いてください。");
                        return;
                    }
                    rewardManager.saveClaimableIcon(day, placed);
                    player.sendMessage("§aDay " + day + " のアイコンを保存しました！");
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                    open(player);
                    break;
                case 5: // 削除
                    rewardManager.clearClaimableIcon(day);
                    player.sendMessage("§eDay " + day + " のアイコンを削除しました。");
                    open(player);
                    break;
                case 8: // 戻る
                    open(player);
                    break;
            }
        }
    }

    private ItemStack make(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }
}