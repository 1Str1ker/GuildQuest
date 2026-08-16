package ua.striker.guildquest.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ua.striker.guildquest.GuildQuest;
import ua.striker.guildquest.models.GuildPlayer;
import ua.striker.guildquest.models.GuildRank;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerManager {

    private final GuildQuest plugin;
    private final Map<UUID, GuildPlayer> playerCache = new HashMap<>();

    public PlayerManager(GuildQuest plugin) {
        this.plugin = plugin;
    }

    public void loadPlayer(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Connection connection = plugin.getDatabaseManager().getConnection();
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM players WHERE uuid = ?")) {
                statement.setString(1, player.getUniqueId().toString());
                ResultSet resultSet = statement.executeQuery();

                GuildPlayer guildPlayer;
                if (resultSet.next()) {
                    GuildRank rank = GuildRank.valueOf(resultSet.getString("rank"));
                    int points = resultSet.getInt("points");
                    double money = resultSet.getDouble("money");
                    int completedQuests = resultSet.getInt("completed_quests");
                    double rating = resultSet.getDouble("rating");
                    int uniqueClients = resultSet.getInt("unique_clients");
                    int createdQuests = resultSet.getInt("created_quests");

                    guildPlayer = new GuildPlayer(player.getUniqueId(), player.getName(), rank, points, money, completedQuests, rating, uniqueClients, createdQuests);
                } else {
                    guildPlayer = new GuildPlayer(player.getUniqueId(), player.getName(), GuildRank.COPPER, 0, 0.0, 0, 0.0, 0, 0);
                    createPlayer(guildPlayer);
                }

                playerCache.put(player.getUniqueId(), guildPlayer);
            } catch (SQLException e) {
                plugin.getLogger().severe("Помилка завантаження гравця: " + e.getMessage());
            }
        });
    }

    private void createPlayer(GuildPlayer gp) {
        Connection connection = plugin.getDatabaseManager().getConnection();
        try (PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO players (uuid, name, rank, points, money, completed_quests, rating, unique_clients, created_quests) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, gp.getUuid().toString());
            statement.setString(2, gp.getName());
            statement.setString(3, gp.getRank().name());
            statement.setInt(4, gp.getPoints());
            statement.setDouble(5, gp.getMoney());
            statement.setInt(6, gp.getCompletedQuests());
            statement.setDouble(7, gp.getRating());
            statement.setInt(8, gp.getUniqueClients());
            statement.setInt(9, gp.getCreatedQuests());
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Помилка створення гравця: " + e.getMessage());
        }
    }

    public void savePlayerAndRemove(UUID uuid) {
        GuildPlayer gp = playerCache.remove(uuid);
        if (gp == null) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Connection connection = plugin.getDatabaseManager().getConnection();
            try (PreparedStatement statement = connection.prepareStatement(
                         "UPDATE players SET name = ?, rank = ?, points = ?, money = ?, completed_quests = ?, rating = ?, unique_clients = ?, created_quests = ? WHERE uuid = ?")) {
                statement.setString(1, gp.getName());
                statement.setString(2, gp.getRank().name());
                statement.setInt(3, gp.getPoints());
                statement.setDouble(4, gp.getMoney());
                statement.setInt(5, gp.getCompletedQuests());
                statement.setDouble(6, gp.getRating());
                statement.setInt(7, gp.getUniqueClients());
                statement.setInt(8, gp.getCreatedQuests());
                statement.setString(9, gp.getUuid().toString());
                statement.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Помилка збереження гравця: " + e.getMessage());
            }
        });
    }

    public GuildPlayer getGuildPlayer(UUID uuid) {
        return playerCache.get(uuid);
    }

    public List<GuildPlayer> getTopAdventurers(int limit) {
        List<GuildPlayer> top = new ArrayList<>();
        Connection connection = plugin.getDatabaseManager().getConnection();
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM players ORDER BY points DESC, completed_quests DESC LIMIT ?")) {
            statement.setInt(1, limit);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                top.add(new GuildPlayer(UUID.fromString(rs.getString("uuid")), rs.getString("name"), 
                        GuildRank.valueOf(rs.getString("rank")), rs.getInt("points"), rs.getDouble("money"), 
                        rs.getInt("completed_quests"), rs.getDouble("rating"), rs.getInt("unique_clients"), rs.getInt("created_quests")));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Помилка отримання топу Шукачів: " + e.getMessage());
        }
        return top;
    }

    public List<GuildPlayer> getTopClients(int limit) {
        List<GuildPlayer> top = new ArrayList<>();
        Connection connection = plugin.getDatabaseManager().getConnection();
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM players ORDER BY created_quests DESC LIMIT ?")) {
            statement.setInt(1, limit);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                top.add(new GuildPlayer(UUID.fromString(rs.getString("uuid")), rs.getString("name"), 
                        GuildRank.valueOf(rs.getString("rank")), rs.getInt("points"), rs.getDouble("money"), 
                        rs.getInt("completed_quests"), rs.getDouble("rating"), rs.getInt("unique_clients"), rs.getInt("created_quests")));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Помилка отримання топу Замовників: " + e.getMessage());
        }
        return top;
    }

    public void modifyPointsAdmin(Player admin, String targetName, int amount, boolean add) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Connection connection = plugin.getDatabaseManager().getConnection();
            try (PreparedStatement statement = connection.prepareStatement("SELECT uuid, points FROM players WHERE name = ? COLLATE NOCASE")) {
                statement.setString(1, targetName);
                ResultSet rs = statement.executeQuery();
                if (rs.next()) {
                    String uuidStr = rs.getString("uuid");
                    int currentPoints = rs.getInt("points");
                    
                    int newPoints = add ? currentPoints + amount : Math.max(0, currentPoints - amount);

                    try (PreparedStatement updateStmt = connection.prepareStatement("UPDATE players SET points = ? WHERE uuid = ?")) {
                        updateStmt.setInt(1, newPoints);
                        updateStmt.setString(2, uuidStr);
                        updateStmt.executeUpdate();
                    }

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        UUID targetUuid = UUID.fromString(uuidStr);
                        GuildPlayer gp = getGuildPlayer(targetUuid);
                        if (gp != null) {
                            if (add) gp.addPoints(amount);
                            else gp.addPoints(-amount);
                        }
                        admin.sendMessage("§a[Гільдія] Очки гравця " + targetName + " оновлено! Тепер у нього: " + newPoints);
                    });
                } else {
                    Bukkit.getScheduler().runTask(plugin, () -> admin.sendMessage("§c[Гільдія] Гравця " + targetName + " не знайдено в базі."));
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Помилка при зміні очок: " + e.getMessage());
            }
        });
    }

    public void setRankAdmin(Player admin, String targetName, String rankName) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Connection connection = plugin.getDatabaseManager().getConnection();
            try {
                GuildRank newRank = GuildRank.valueOf(rankName.toUpperCase());
                
                try (PreparedStatement statement = connection.prepareStatement("SELECT uuid FROM players WHERE name = ? COLLATE NOCASE")) {
                    statement.setString(1, targetName);
                    ResultSet rs = statement.executeQuery();
                    if (rs.next()) {
                        String uuidStr = rs.getString("uuid");
                        
                        try (PreparedStatement updateStmt = connection.prepareStatement("UPDATE players SET rank = ? WHERE uuid = ?")) {
                            updateStmt.setString(1, newRank.name());
                            updateStmt.setString(2, uuidStr);
                            updateStmt.executeUpdate();
                        }
                        
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            GuildPlayer gp = getGuildPlayer(UUID.fromString(uuidStr));
                            if (gp != null) gp.setRank(newRank);
                            admin.sendMessage("§a[Гільдія] Ранг гравця " + targetName + " успішно змінено на " + newRank.getDisplayName() + "!");
                        });
                    } else {
                        Bukkit.getScheduler().runTask(plugin, () -> admin.sendMessage("§c[Гільдія] Гравця " + targetName + " не знайдено."));
                    }
                }
            } catch (IllegalArgumentException e) {
                Bukkit.getScheduler().runTask(plugin, () -> admin.sendMessage("§c[Гільдія] Ранг " + rankName + " не існує. Використовуйте: COPPER, IRON, GOLD, EMERALD, DIAMOND, NETHERITE"));
            } catch (SQLException e) {
                plugin.getLogger().severe("Помилка зміни рангу: " + e.getMessage());
            }
        });
    }

    public void setRatingAdmin(Player admin, String targetName, double newRating) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Connection connection = plugin.getDatabaseManager().getConnection();
            try (PreparedStatement statement = connection.prepareStatement("SELECT uuid FROM players WHERE name = ? COLLATE NOCASE")) {
                statement.setString(1, targetName);
                ResultSet rs = statement.executeQuery();
                if (rs.next()) {
                    String uuidStr = rs.getString("uuid");
                    
                    try (PreparedStatement delStmt = connection.prepareStatement("DELETE FROM reviews WHERE worker_uuid = ?")) {
                        delStmt.setString(1, uuidStr);
                        delStmt.executeUpdate();
                    }
                    try (PreparedStatement insertStmt = connection.prepareStatement("INSERT INTO reviews (quest_id, worker_uuid, score) VALUES (0, ?, ?)")) {
                        insertStmt.setString(1, uuidStr);
                        insertStmt.setInt(2, (int) Math.round(newRating));
                        insertStmt.executeUpdate();
                    }
                    try (PreparedStatement updateStmt = connection.prepareStatement("UPDATE players SET rating = ? WHERE uuid = ?")) {
                        updateStmt.setDouble(1, newRating);
                        updateStmt.setString(2, uuidStr);
                        updateStmt.executeUpdate();
                    }
                    
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        GuildPlayer gp = getGuildPlayer(UUID.fromString(uuidStr));
                        if (gp != null) gp.setRating(newRating);
                        admin.sendMessage("§a[Гільдія] Рейтинг гравця " + targetName + " примусово змінено на " + newRating + " ⭐!");
                    });
                } else {
                    Bukkit.getScheduler().runTask(plugin, () -> admin.sendMessage("§c[Гільдія] Гравця " + targetName + " не знайдено."));
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Помилка зміни рейтингу: " + e.getMessage());
            }
        });
    }
    // НОВИЙ МЕТОД: Миттєве збереження рангу в базу (викликається при підвищенні)
    public void updateRankInDatabase(UUID uuid, GuildRank newRank) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Connection connection = plugin.getDatabaseManager().getConnection();
            try (PreparedStatement statement = connection.prepareStatement("UPDATE players SET rank = ? WHERE uuid = ?")) {
                statement.setString(1, newRank.name());
                statement.setString(2, uuid.toString());
                statement.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Помилка при миттєвому збереженні рангу: " + e.getMessage());
            }
        });
    }
}