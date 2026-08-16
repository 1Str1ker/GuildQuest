package ua.striker.guildquest.managers;

import org.bukkit.configuration.file.FileConfiguration;
import ua.striker.guildquest.GuildQuest;

public class ConfigManager {

    private final GuildQuest plugin;
    private FileConfiguration config;

    public ConfigManager(GuildQuest plugin) {
        this.plugin = plugin;
        // Зберігає config.yml з ресурсів, якщо його ще немає в папці плагіна
        plugin.saveDefaultConfig(); 
        this.config = plugin.getConfig();
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public double getCommissionPercent() {
        return config.getDouble("creation-commission-percent", 5.0);
    }

    // Отримує кількість очок за 1 штуку конкретного предмета
    public int getItemPoints(String materialName) {
        String path = "points.items." + materialName.toUpperCase();
        if (config.contains(path)) {
            return config.getInt(path);
        }
        return config.getInt("points.default-points", 1);
    }
    // Перевіряє, чи увімкнена вимога до унікальних замовників
    public boolean isUniqueClientsRequired() {
        return config.getBoolean("rank-system.require-unique-clients", true);
    }
    // НОВИЙ МЕТОД: Отримує список усіх дозволених предметів з конфігу
    public java.util.Set<String> getConfiguredItems() {
        if (config.contains("points.items")) {
            return config.getConfigurationSection("points.items").getKeys(false);
        }
        return new java.util.HashSet<>();
    }
}