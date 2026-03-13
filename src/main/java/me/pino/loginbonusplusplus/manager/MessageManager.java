package me.pino.loginbonusplusplus.manager;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class MessageManager {

    private final JavaPlugin plugin;
    private File messagesFile;
    private FileConfiguration messagesConfig;

    public MessageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    private void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");

        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public void reload() {
        loadMessages();
    }

    public String get(String path) {
        if (messagesConfig.contains(path)) {
            String message = messagesConfig.getString(path);
            if (message != null) {
                return ChatColor.translateAlternateColorCodes('&', message);
            }
        }
        return "Message not found: " + path;
    }
}