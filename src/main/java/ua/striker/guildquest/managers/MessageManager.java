package ua.striker.guildquest.managers;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ua.striker.guildquest.GuildQuest;

import java.io.File;

public class MessageManager {

    private final GuildQuest plugin;
    private FileConfiguration messagesConfig;
    private File messagesFile;

    public MessageManager(GuildQuest plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    public void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            messagesFile.getParentFile().mkdirs();
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    // Отримання повідомлення з додаванням префікса та кольорами
    public String getMessage(String path) {
        String prefix = messagesConfig.getString("prefix", "&8[&6Гільдія&8] &r");
        String message = messagesConfig.getString(path, "&cПовідомлення не знайдено: " + path);
        return ChatColor.translateAlternateColorCodes('&', prefix + message);
    }

    // Отримання чистого повідомлення (без префікса, наприклад для GUI)
    public String getRawMessage(String path) {
        String message = messagesConfig.getString(path, "&cПовідомлення не знайдено: " + path);
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}