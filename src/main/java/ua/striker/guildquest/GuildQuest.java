package ua.striker.guildquest;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import ua.striker.guildquest.commands.GuildQuestCommand;
import ua.striker.guildquest.database.DatabaseManager;
import ua.striker.guildquest.hooks.GuildQuestExpansion;
import ua.striker.guildquest.hooks.GuildQuestTrait;
import ua.striker.guildquest.listeners.MenuListener;
import ua.striker.guildquest.listeners.PlayerConnectionListener;
import ua.striker.guildquest.managers.*;

public final class GuildQuest extends JavaPlugin {

    private ConfigManager configManager;
    private MessageManager messageManager;
    private MenuConfigManager menuConfigManager;
    private DatabaseManager databaseManager;
    private PlayerManager playerManager;
    private QuestManager questManager;
    private HologramManager hologramManager;
    
    private static Economy econ = null;

    @Override
    public void onEnable() {
        // 1. Ініціалізація базових конфігів
        configManager = new ConfigManager(this);
        messageManager = new MessageManager(this);
        menuConfigManager = new MenuConfigManager(this);

        // =========================================
        // ПЕРЕВІРКА КРИТИЧНИХ ЗАЛЕЖНОСТЕЙ
        // =========================================
        
        // Перевіряємо чи є сам плагін Vault
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().severe("=====================================================");
            getLogger().severe(" ❌ КРИТИЧНА ПОМИЛКА ЗАПУСКУ GuildQuest!");
            getLogger().severe(" Плагін Vault не знайдено на сервері.");
            getLogger().severe(" GuildQuest потребує Vault для роботи з грошима.");
            getLogger().severe(" Завантажте Vault: https://www.spigotmc.org/resources/34315/");
            getLogger().severe("=====================================================");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Перевіряємо чи є плагін на економіку (наприклад EssentialsX)
        if (!setupEconomy()) {
            getLogger().severe("=====================================================");
            getLogger().severe(" ❌ КРИТИЧНА ПОМИЛКА ЗАПУСКУ GuildQuest!");
            getLogger().severe(" Vault знайдено, але економіку не підключено.");
            getLogger().severe(" У вас не встановлено плагін на економіку (наприклад, EssentialsX).");
            getLogger().severe(" Встановіть його та перезапустіть сервер.");
            getLogger().severe("=====================================================");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 2. Ініціалізація баз даних та менеджерів
        databaseManager = new DatabaseManager(this);
        databaseManager.connect();

        playerManager = new PlayerManager(this);
        questManager = new QuestManager(this);
        hologramManager = new HologramManager(this);

        // 3. Реєстрація подій та команд
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);

        if (getCommand("guildquest") != null) {
            getCommand("guildquest").setExecutor(new GuildQuestCommand(this));
        }

        // =========================================
        // ПЕРЕВІРКА ДОДАТКОВИХ ЗАЛЕЖНОСТЕЙ
        // =========================================

        getLogger().info("--- Перевірка інтеграцій ---");

        // PlaceholderAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new GuildQuestExpansion(this).register();
            getLogger().info("✅ PlaceholderAPI: Знайдено (Плейсхолдери активовано)");
        } else {
            getLogger().warning("⚠️ PlaceholderAPI: Не знайдено (Плейсхолдери не працюватимуть)");
        }

        // Citizens
        if (getServer().getPluginManager().getPlugin("Citizens") != null) {
            net.citizensnpcs.api.CitizensAPI.getTraitFactory().registerTrait(net.citizensnpcs.api.trait.TraitInfo.create(GuildQuestTrait.class));
            getLogger().info("✅ Citizens: Знайдено (NPC-квестери активовано)");
        } else {
            getLogger().warning("⚠️ Citizens: Не знайдено (Прив'язка до NPC не працюватиме)");
        }

        // DecentHolograms
        if (getServer().getPluginManager().getPlugin("DecentHolograms") != null) {
            hologramManager.updateTopHologram(null);
            getLogger().info("✅ DecentHolograms: Знайдено (Голограми топу активовано)");
        } else {
            getLogger().warning("⚠️ DecentHolograms: Не знайдено (Голограми не працюватимуть)");
        }
        
        getLogger().info("----------------------------");
        getLogger().info(" ⚔️ GuildQuest v1.4 успішно запущено!");
        getLogger().info("====================================");
    }

    @Override
    public void onDisable() {
        if (playerManager != null) {
            getServer().getOnlinePlayers().forEach(player -> 
                playerManager.savePlayerAndRemove(player.getUniqueId())
            );
        }
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        econ = rsp.getProvider();
        return econ != null;
    }

    public static Economy getEconomy() { return econ; }
    
    public ConfigManager getConfigManager() { return configManager; }
    public MessageManager getMessageManager() { return messageManager; }
    public MenuConfigManager getMenuConfigManager() { return menuConfigManager; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public PlayerManager getPlayerManager() { return playerManager; }
    public QuestManager getQuestManager() { return questManager; }
    public HologramManager getHologramManager() { return hologramManager; }
}