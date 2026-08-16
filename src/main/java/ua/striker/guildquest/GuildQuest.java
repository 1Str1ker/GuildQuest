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
    private MenuConfigManager menuConfigManager; // НОВЕ: Менеджер меню
    private DatabaseManager databaseManager;
    private PlayerManager playerManager;
    private QuestManager questManager;
    private HologramManager hologramManager;
    
    private static Economy econ = null;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        messageManager = new MessageManager(this);
        menuConfigManager = new MenuConfigManager(this); // НОВЕ: Ініціалізація

        if (!setupEconomy()) {
            // Замінено застарілий метод, щоб прибрати Warning у редакторі
            getLogger().severe(String.format("[%s] - Вимкнено через відсутність Vault!", getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        databaseManager = new DatabaseManager(this);
        databaseManager.connect();

        playerManager = new PlayerManager(this);
        questManager = new QuestManager(this);
        hologramManager = new HologramManager(this);

        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(), this);

        if (getCommand("guildquest") != null) {
            getCommand("guildquest").setExecutor(new GuildQuestCommand(this));
        }

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new GuildQuestExpansion(this).register();
        }

        // Інтеграція з Citizens
        if (getServer().getPluginManager().getPlugin("Citizens") != null) {
            net.citizensnpcs.api.CitizensAPI.getTraitFactory().registerTrait(net.citizensnpcs.api.trait.TraitInfo.create(GuildQuestTrait.class));
            getLogger().info("Citizens знайдено! Трейт 'guildquest' зареєстровано.");
        }

        // Інтеграція з DecentHolograms
        if (getServer().getPluginManager().getPlugin("DecentHolograms") != null) {
            getLogger().info("DecentHolograms знайдено! Голограми активовано.");
            hologramManager.updateTopHologram(null);
        }

        getLogger().info("====================================");
        getLogger().info(" ⚔️ GuildQuest успішно запущено!");
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
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        econ = rsp.getProvider();
        return econ != null;
    }

    public static Economy getEconomy() { return econ; }
    
    public ConfigManager getConfigManager() { return configManager; }
    public MessageManager getMessageManager() { return messageManager; }
    public MenuConfigManager getMenuConfigManager() { return menuConfigManager; } // НОВЕ: Геттер
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public PlayerManager getPlayerManager() { return playerManager; }
    public QuestManager getQuestManager() { return questManager; }
    public HologramManager getHologramManager() { return hologramManager; }
}