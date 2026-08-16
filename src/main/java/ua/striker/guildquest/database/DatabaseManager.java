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

    public void connect() {
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            File dbFile = new File(plugin.getDataFolder(), "database.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            plugin.getLogger().info("Успішно підключено до SQLite бази даних!");
            setupTables();
        } catch (SQLException e) {
            plugin.getLogger().severe("Помилка підключення до бази даних: " + e.getMessage());
        }
    }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("З'єднання з базою даних закрито.");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Помилка закриття бази даних: " + e.getMessage());
        }
    }

    public Connection getConnection() {
        return connection;
    }

    private void setupTables() {
        String createPlayersTable = "CREATE TABLE IF NOT EXISTS players (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "name VARCHAR(16) NOT NULL," +
                "rank VARCHAR(20) DEFAULT 'COPPER'," +
                "points INTEGER DEFAULT 0," +
                "money DOUBLE DEFAULT 0.0," +
                "completed_quests INTEGER DEFAULT 0," +
                "rating DOUBLE DEFAULT 0.0," +
                "unique_clients INTEGER DEFAULT 0," +
                "created_quests INTEGER DEFAULT 0" +
                ");";

        String createQuestsTable = "CREATE TABLE IF NOT EXISTS quests (" +
                "quest_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "client_uuid VARCHAR(36) NOT NULL," +
                "worker_uuid VARCHAR(36)," + 
                "target_item VARCHAR(50) NOT NULL," +
                "amount INTEGER NOT NULL," +
                "reward DOUBLE NOT NULL," +
                "status VARCHAR(20) DEFAULT 'OPEN'" +
                ");";

        String createReviewsTable = "CREATE TABLE IF NOT EXISTS reviews (" +
                "review_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "quest_id INTEGER DEFAULT 0," +
                "worker_uuid VARCHAR(36) NOT NULL," +
                "score INTEGER NOT NULL," +
                "review_text TEXT" +
                ");";

        String createHistoryTable = "CREATE TABLE IF NOT EXISTS quest_history (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "worker_uuid VARCHAR(36) NOT NULL," +
                "client_uuid VARCHAR(36) NOT NULL," +
                "timestamp LONG NOT NULL" +
                ");";

        try (Statement statement = connection.createStatement()) {
            statement.execute(createPlayersTable);
            statement.execute(createQuestsTable);
            statement.execute(createReviewsTable);
            statement.execute(createHistoryTable);

            // Безпечне додавання нових колонок у разі оновлення існуючої бази
            try {
                statement.execute("ALTER TABLE players ADD COLUMN created_quests INTEGER DEFAULT 0;");
            } catch (SQLException ignored) {}
            
            try {
                statement.execute("ALTER TABLE reviews ADD COLUMN quest_id INTEGER DEFAULT 0;");
            } catch (SQLException ignored) {}
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Помилка при створенні таблиць: " + e.getMessage());
        }
    }
}