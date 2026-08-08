package ua.striker.guildquest.database;

import ua.striker.guildquest.GuildQuest;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private final GuildQuest plugin;
    private Connection connection;

    public DatabaseManager(GuildQuest plugin) {
        this.plugin = plugin;
    }

    // Підключення до бази даних (створення файлу, якщо його немає)
    public void connect() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdir();
        }

        File databaseFile = new File(dataFolder, "database.db");
        String url = "jdbc:sqlite:" + databaseFile.getAbsolutePath();

        try {
            connection = DriverManager.getConnection(url);
            plugin.getLogger().info("Успішне підключення до SQLite!");
            setupTables();
        } catch (SQLException e) {
            plugin.getLogger().severe("Помилка підключення до бази даних: " + e.getMessage());
        }
    }

    // Закриття з'єднання
    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("З'єднання з SQLite закрито.");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Помилка при закритті бази даних: " + e.getMessage());
        }
    }

    // Створення таблиць при першому запуску
    private void setupTables() {
        String createPlayersTable = "CREATE TABLE IF NOT EXISTS players (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "name VARCHAR(16) NOT NULL," +
                "rank VARCHAR(20) DEFAULT 'COPPER'," +
                "points INTEGER DEFAULT 0," +
                "money DOUBLE DEFAULT 0.0," +
                "completed_quests INTEGER DEFAULT 0," +
                "rating DOUBLE DEFAULT 0.0" +
                ");";

        String createQuestsTable = "CREATE TABLE IF NOT EXISTS quests (" +
                "quest_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "client_uuid VARCHAR(36) NOT NULL," +
                "target_item VARCHAR(50) NOT NULL," +
                "amount INTEGER NOT NULL," +
                "reward DOUBLE NOT NULL," +
                "status VARCHAR(20) DEFAULT 'OPEN'" +
                ");";

        String createReviewsTable = "CREATE TABLE IF NOT EXISTS reviews (" +
                "review_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "worker_uuid VARCHAR(36) NOT NULL," +
                "score INTEGER NOT NULL," +
                "review_text TEXT" +
                ");";

        try (Statement statement = connection.createStatement()) {
            statement.execute(createPlayersTable);
            statement.execute(createQuestsTable);
            statement.execute(createReviewsTable);
        } catch (SQLException e) {
            plugin.getLogger().severe("Помилка при створенні таблиць: " + e.getMessage());
        }
    }

    public Connection getConnection() {
        return connection;
    }
}