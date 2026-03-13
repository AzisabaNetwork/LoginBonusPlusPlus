package me.pino.loginbonusplusplus.listener;

import me.pino.loginbonusplusplus.gui.AdminCalendarGUI;
import me.pino.loginbonusplusplus.gui.DayRewardEditGUI;
import me.pino.loginbonusplusplus.manager.MessageManager;
import me.pino.loginbonusplusplus.manager.RewardManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class AdminCalendarClickListener implements Listener {

    private final RewardManager rewardManager;
    private final DayRewardEditGUI dayRewardEditGUI;
    private final AdminCalendarGUI adminCalendarGUI;
    private final MessageManager messageManager;

    public AdminCalendarClickListener(RewardManager rewardManager, DayRewardEditGUI dayRewardEditGUI, AdminCalendarGUI adminCalendarGUI, MessageManager messageManager) {
        this.rewardManager = rewardManager;
        this.dayRewardEditGUI = dayRewardEditGUI;
        this.adminCalendarGUI = adminCalendarGUI;
        this.messageManager = messageManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        // Check if this is our admin calendar GUI
        if (!event.getView().getTitle().equals("§cAdmin Reward Editor")) {
            return;
        }

        // Add null check for clicked inventory
        if (event.getClickedInventory() == null) {
            //event.setCancelled(true);
            return;
        }

        // Cancel all clicks (both top and bottom inventory)
        event.setCancelled(true);

        // Check if clicked inventory is the top inventory (our GUI)
        if (!event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }

        int slot = event.getSlot();
        Player player = (Player) event.getWhoClicked();
        if (player == null) {
            return;
        }

        // Handle streak rewards edit button (slot 45)
        if (slot == 45) {
            // TODO: Open StreakRewardEditGUI when created
            player.sendMessage("§eStreak rewards editor coming soon!");

            return;
        }

        // Check if clicked slot is valid (0-30 for days 1-31)
        if (slot < 0 || slot >= 31) {
            return;
        }

        // Calculate day (slot + 1 because inventory starts at 0)
        int day = slot + 1;

        // Open day reward edit GUI
        dayRewardEditGUI.open(player, day);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        // Check if this is our admin calendar GUI
        if (!event.getView().getTitle().equals("§cAdmin Reward Editor")) {
            return;
        }

        // Prevent dragging in our admin calendar GUI
        event.setCancelled(true);
    }
}
