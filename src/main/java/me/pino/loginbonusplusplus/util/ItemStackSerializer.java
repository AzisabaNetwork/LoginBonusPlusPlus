package me.pino.loginbonusplusplus.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.logging.Level;

public class ItemStackSerializer {

    private static JavaPlugin plugin;

    public static void setPlugin(JavaPlugin plugin) {
        ItemStackSerializer.plugin = plugin;
    }

    public static String itemToBase64(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

            dataOutput.writeObject(item);
            dataOutput.close();

            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (IOException e) {
            if (plugin != null) {
                plugin.getLogger().log(Level.WARNING, "Failed to serialize ItemStack: " + item.getType(), e);
            }
            throw new RuntimeException("Failed to serialize ItemStack", e);
        }
    }

    public static ItemStack itemFromBase64(String base64) {
        if (base64 == null || base64.trim().isEmpty()) {
            return null;
        }

        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(base64));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);

            Object obj = dataInput.readObject();
            dataInput.close();

            // 安全性チェック
            if (!(obj instanceof ItemStack)) {
                if (plugin != null) {
                    plugin.getLogger().warning("Invalid object type in deserialization: " + obj.getClass().getName());
                }
                return null;
            }

            ItemStack item = (ItemStack) obj;

            // アイテムの妥当性チェック
            if (!isValidItem(item)) {
                if (plugin != null) {
                    plugin.getLogger().warning("Invalid item detected: " + item.getType() + " x" + item.getAmount());
                }
                return null;
            }

            return item;

        } catch (IOException | ClassNotFoundException e) {
            if (plugin != null) {
                plugin.getLogger().log(Level.WARNING, "Failed to deserialize ItemStack from base64: " + base64.substring(0, Math.min(50, base64.length())), e);
            }
            return null; // 安全のためnullを返す
        } catch (Exception e) {
            if (plugin != null) {
                plugin.getLogger().log(Level.SEVERE, "Unexpected error during deserialization", e);
            }
            return null;
        }
    }

    private static boolean isValidItem(ItemStack item) {
        if (item == null) return false;
        
        // マテリアルチェック
        if (item.getType() == Material.AIR) return false;
        
        // 個数チェック
        if (item.getAmount() < 1 || item.getAmount() > item.getMaxStackSize()) {
            return false;
        }
        
        // カスタムモデルデータチェック（もし存在する場合）
        if (item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) {
            int customModel = item.getItemMeta().getCustomModelData();
            if (customModel < 0) {
                return false;
            }
        }
        
        return true;
    }
}
