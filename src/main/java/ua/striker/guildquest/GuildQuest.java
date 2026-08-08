package ua.striker.guildquest;

import org.bukkit.plugin.java.JavaPlugin;
import ua.striker.guildquest.commands.GuildQuestCommand;
import ua.striker.guildquest.database.DatabaseManager;
import ua.striker.guildquest.listeners.MenuListener;
import ua.striker.guildquest.listeners.PlayerConnectionListener;
import ua.striker.guildquest.managers.PlayerManager;
import ua.striker.guildquest.managers.QuestManager;

public final class GuildQuest extends JavaPlugin {

    private DatabaseManager databaseManager;
    private PlayerManager playerManager;
    private QuestManager questManager;

    @Override
    public void onEnable() {
        // Перевірка наявності PlaceholderAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            getLogger().info("PlaceholderAPI знайдено! Інтеграцію увімкнено.");
            // TODO: Тут пізніше ми зареєструємо розширення для PAPI
        } else {
            getLogger().warning("PlaceholderAPI не знайдено! Деякі функції можуть не працювати.");
        }

        // 1. Ініціалізуємо та підключаємо базу даних
        databaseManager = new DatabaseManager(this);
        databaseManager.connect();

        // 2. Ініціалізуємо менеджери
        playerManager = new PlayerManager(this);
        questManager = new QuestManager(this);

        // 3. Реєструємо слухачі подій (Listeners)
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(), this);

        // 4. Реєструємо команду /guildquest
        if (getCommand("guildquest") != null) {
            getCommand("guildquest").setExecutor(new GuildQuestCommand(this));
        }

        getLogger().info("====================================");
        getLogger().info(" ⚔️ GuildQuest успішно запущено!");
        getLogger().info(" Версія: 1.0");
        getLogger().info("====================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("GuildQuest вимкнено. Збереження даних...");
        
        // Зберігаємо дані всіх гравців, які залишилися онлайн під час вимкнення/рестарту
        if (playerManager != null) {
            getServer().getOnlinePlayers().forEach(player -> 
                playerManager.savePlayerAndRemove(player.getUniqueId())
            );
        }
        
        // Безпечно закриваємо з'єднання з БД
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
    }

    // Гетери для доступу до менеджерів з інших класів
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public QuestManager getQuestManager() {
        return questManager;
    }
}