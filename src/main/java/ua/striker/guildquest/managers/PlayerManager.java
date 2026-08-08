package ua.striker.guildquest.managers;

import org.bukkit.Bukkit;
import ua.striker.guildquest.GuildQuest;
import ua.striker.guildquest.models.GuildPlayer;
import ua.striker.guildquest.models.GuildRank;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerManager {

    private final GuildQuest plugin;
    // Кеш гравців, які зараз онлайн
    private final Map<UUID, GuildPlayer> onlinePlayers = new HashMap<>();

    public PlayerManager(GuildQuest plugin) {
        this.plugin = plugin;
    }

    // Отримати гравця з кешу
    public GuildPlayer getGuildPlayer(UUID uuid) {
        return onlinePlayers.get(uuid);
    }

    // Асинхронне завантаження даних гравця при вході
    public void loadPlayer(UUID uuid, String name) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = plugin.getDatabaseManager().getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT * FROM players WHERE uuid = ?")) {
                
                statement.setString(1, uuid.toString());
                ResultSet resultSet = statement.executeQuery();

                GuildPlayer guildPlayer;
                if (resultSet.next()) {
                    // Гравець вже є в базі, завантажуємо його дані
                    GuildRank rank = GuildRank.valueOf(resultSet.getString("rank"));
                    int points = resultSet.getInt("points");
                    double money = resultSet.getDouble("money");
                    int completedQuests = resultSet.getInt("completed_quests");
                    double rating = resultSet.getDouble("rating");

                    guildPlayer = new GuildPlayer(uuid, name, rank, points, money, completedQuests, rating);
                } else {
                    // Новий гравець, створюємо стандартний профіль
                    guildPlayer = new GuildPlayer(uuid, name, GuildRank.COPPER, 0, 0.0, 0, 0.0);
                    createNewPlayerInDB(guildPlayer);
                }

                // Додаємо в кеш у головному потоці
                Bukkit.getScheduler().runTask(plugin, () -> onlinePlayers.put(uuid, guildPlayer));

            } catch (SQLException e) {
                plugin.getLogger().severe("Помилка при завантаженні гравця " + name + ": " + e.getMessage());
            }
        });
    }

    // Створення нового запису в базі
    private void createNewPlayerInDB(GuildPlayer player) {
        try (Connection connection = plugin.getDatabaseManager().getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO players (uuid, name) VALUES (?, ?)")) {
            statement.setString(1, player.getUuid().toString());
            statement.setString(2, player.getName());
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Асинхронне збереження даних при виході
    public void savePlayerAndRemove(UUID uuid) {
        GuildPlayer player = onlinePlayers.remove(uuid); // Видаляємо з кешу
        if (player == null) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = plugin.getDatabaseManager().getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "UPDATE players SET rank = ?, points = ?, money = ?, completed_quests = ?, rating = ? WHERE uuid = ?")) {
                
                statement.setString(1, player.getRank().name());
                statement.setInt(2, player.getPoints());
                statement.setDouble(3, player.getMoney());
                statement.setInt(4, player.getCompletedQuests());
                statement.setDouble(5, player.getRating());
                statement.setString(6, uuid.toString());
                
                statement.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Помилка при збереженні гравця " + player.getName() + ": " + e.getMessage());
            }
        });
    }
}