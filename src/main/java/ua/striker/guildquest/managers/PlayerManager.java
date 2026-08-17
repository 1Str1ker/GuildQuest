package ua.striker.guildquest.managers;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import ua.striker.guildquest.GuildQuest;
import ua.striker.guildquest.models.GuildPlayer;
import ua.striker.guildquest.models.GuildRank;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PlayerManager {

    private final GuildQuest plugin;
    private final Map<UUID, GuildPlayer> playerCache = new ConcurrentHashMap<>();

    public PlayerManager(GuildQuest plugin) {
        this.plugin = plugin;
    }

    public void loadPlayer(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (PreparedStatement ps = plugin.getDatabaseManager().getConnection().prepareStatement("SELECT * FROM players WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                
                if (rs.next()) {
                    String name = rs.getString("name");
                    if (name == null) name = Bukkit.getOfflinePlayer(uuid).getName();
                    
                    GuildRank rank;
                    try {
                        rank = GuildRank.valueOf(rs.getString("rank"));
                    } catch (IllegalArgumentException e) {
                        rank = GuildRank.values()[0]; // Запобіжник, якщо ранг було змінено
                    }
                    
                    int points = rs.getInt("points");
                    double money = rs.getDouble("money");
                    int completedQuests = rs.getInt("completed_quests");
                    double rating = rs.getDouble("rating");
                    int uniqueClients = rs.getInt("unique_clients");
                    int createdQuests = rs.getInt("created_quests");

                    GuildPlayer gp = new GuildPlayer(uuid, name, rank, points, money, completedQuests, rating, uniqueClients, createdQuests);
                    playerCache.put(uuid, gp);
                } else {
                    String name = Bukkit.getOfflinePlayer(uuid).getName();
                    GuildRank startingRank = GuildRank.values()[0]; // Автоматично беремо найнижчий початковий ранг
                    
                    GuildPlayer gp = new GuildPlayer(uuid, name, startingRank, 0, 0.0, 0, 5.0, 0, 0);
                    playerCache.put(uuid, gp);
                    
                    plugin.getDatabaseManager().executeAsync(
                        "INSERT INTO players (uuid, name, points, rank, money, completed_quests, rating, unique_clients, created_quests) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", 
                        uuid.toString(), name, 0, startingRank.name(), 0.0, 0, 5.0, 0, 0
                    );
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Помилка завантаження гравця: " + e.getMessage());
            }
        });
    }

    public void savePlayerAndRemove(UUID uuid) {
        GuildPlayer gp = playerCache.remove(uuid);
        if (gp != null) {
            plugin.getDatabaseManager().executeAsync(
                "UPDATE players SET points = ?, rank = ?, money = ?, completed_quests = ?, rating = ?, unique_clients = ?, created_quests = ? WHERE uuid = ?",
                gp.getPoints(), gp.getRank().name(), gp.getMoney(), gp.getCompletedQuests(), gp.getRating(), gp.getUniqueClients(), gp.getCreatedQuests(), uuid.toString()
            );
        }
    }

    public GuildPlayer getGuildPlayer(UUID uuid) {
        return playerCache.get(uuid);
    }

    public void updateRankInDatabase(UUID uuid, GuildRank rank) {
        plugin.getDatabaseManager().executeAsync("UPDATE players SET rank = ? WHERE uuid = ?", rank.name(), uuid.toString());
    }

    public List<GuildPlayer> getTopAdventurers(int limit) {
        return playerCache.values().stream()
                .sorted((p1, p2) -> Integer.compare(p2.getPoints(), p1.getPoints()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<GuildPlayer> getTopClients(int limit) {
        return playerCache.values().stream()
                .sorted((p1, p2) -> Integer.compare(p2.getCreatedQuests(), p1.getCreatedQuests()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    // --- ВІДНОВЛЕНІ МЕТОДИ АДМІНІСТРАТОРА ---
    
    public void modifyPointsAdmin(Player admin, String targetName, int amount, boolean add) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target != null) {
            GuildPlayer gp = playerCache.get(target.getUniqueId());
            if (gp != null) {
                gp.addPoints(add ? amount : -amount);
                plugin.getDatabaseManager().executeAsync("UPDATE players SET points = ? WHERE uuid = ?", gp.getPoints(), gp.getUuid().toString());
                admin.sendMessage("§a[Гільдія] Очки гравця " + targetName + " оновлено!");
            }
        } else {
            admin.sendMessage("§c[Гільдія] Гравець не знайдений або офлайн!");
        }
    }

    public void setRankAdmin(Player admin, String targetName, String rankName) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target != null) {
            try {
                GuildRank rank = GuildRank.valueOf(rankName.toUpperCase());
                GuildPlayer gp = playerCache.get(target.getUniqueId());
                if (gp != null) {
                    gp.setRank(rank);
                    plugin.getDatabaseManager().executeAsync("UPDATE players SET rank = ? WHERE uuid = ?", rank.name(), gp.getUuid().toString());
                    admin.sendMessage("§a[Гільдія] Ранг гравця " + targetName + " змінено на " + rank.name() + "!");
                }
            } catch (IllegalArgumentException e) {
                admin.sendMessage("§c[Гільдія] Невідомий ранг! Перевірте правильність написання.");
            }
        } else {
            admin.sendMessage("§c[Гільдія] Гравець не знайдений або офлайн!");
        }
    }

    public void setRatingAdmin(Player admin, String targetName, double rating) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target != null) {
            GuildPlayer gp = playerCache.get(target.getUniqueId());
            if (gp != null) {
                gp.setRating(rating);
                plugin.getDatabaseManager().executeAsync("UPDATE players SET rating = ? WHERE uuid = ?", rating, gp.getUuid().toString());
                admin.sendMessage("§a[Гільдія] Рейтинг гравця " + targetName + " змінено на " + rating + "!");
            }
        } else {
            admin.sendMessage("§c[Гільдія] Гравець не знайдений або офлайн!");
        }
    }
}