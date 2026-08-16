package ua.striker.guildquest.database;

import org.bukkit.Bukkit;
import ua.striker.guildquest.GuildQuest;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private final GuildQuest plugin;
    private Connection connection;

    public DatabaseManager(GuildQuest plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            File databaseFile = new File(dataFolder, "database.db");
            
            // Підключення до SQLite
            connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
            createTables();
            plugin.getLogger().info("Базу даних успішно підключено!");
        } catch (SQLException e) {
            plugin.getLogger().severe("Помилка підключення до БД: " + e.getMessage());
        }
    }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("З'єднання з базою даних безпечно закрито.");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Помилка при закритті БД: " + e.getMessage());
        }
    }

    public Connection getConnection() {
        return connection;
    }

    private void createTables() {
        // Створення таблиць виконується синхронно лише під час увімкнення плагіна
        String quests = "CREATE TABLE IF NOT EXISTS quests (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "creator VARCHAR(36) NOT NULL, " +
                "target_item VARCHAR(50) NOT NULL, " +
                "amount INTEGER NOT NULL, " +
                "reward DOUBLE NOT NULL, " +
                "status VARCHAR(20) NOT NULL, " +
                "worker VARCHAR(36));";

        String players = "CREATE TABLE IF NOT EXISTS players (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "points INTEGER DEFAULT 0, " +
                "rank VARCHAR(20) DEFAULT 'BRONZE', " +
                "rating DOUBLE DEFAULT 5.0);";

        try (Statement statement = connection.createStatement()) {
            statement.execute(quests);
            statement.execute(players);
        } catch (SQLException e) {
            plugin.getLogger().severe("Помилка створення таблиць: " + e.getMessage());
        }
    }

    /**
     * НОВИЙ МЕТОД: Асинхронне виконання запитів (INSERT, UPDATE, DELETE).
     * Використовує фоновий потік сервера, щоб не створювати лагів для гравців.
     */
    public void executeAsync(String query, Object... params) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (PreparedStatement ps = connection.prepareStatement(query)) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Помилка асинхронного запиту: " + query);
                e.printStackTrace();
            }
        });
    }
}