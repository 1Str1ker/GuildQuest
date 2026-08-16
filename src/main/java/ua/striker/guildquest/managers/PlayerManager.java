package ua.striker.guildquest.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ua.striker.guildquest.GuildQuest;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerManager {

    private final GuildQuest plugin;
    
    // Кеш для миттєвого доступу без створення лагів (TPS drops)
    private final Map<UUID, Integer> pointsCache = new HashMap<>();
    private final Map<UUID, String> rankCache = new HashMap<>();
    private final Map<UUID, Double> ratingCache = new HashMap<>();

    public PlayerManager(GuildQuest plugin) {
        this.plugin = plugin;
    }

    /**
     * Асинхронне завантаження гравця при вході на сервер.
     * Запускається в окремому потоці.
     */
    public void loadPlayer(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (PreparedStatement ps = plugin.getDatabaseManager().getConnection().prepareStatement("SELECT * FROM players WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                
                if (rs.next()) {
                    pointsCache.put(uuid, rs.getInt("points"));
                    rankCache.put(uuid, rs.getString("rank"));
                    ratingCache.put(uuid, rs.getDouble("rating"));
                } else {
                    // Якщо гравець новий, записуємо його в БД асинхронно і додаємо в кеш
                    pointsCache.put(uuid, 0);
                    rankCache.put(uuid, "BRONZE");
                    ratingCache.put(uuid, 5.0);
                    plugin.getDatabaseManager().executeAsync("INSERT INTO players (uuid, points, rank, rating) VALUES (?, ?, ?, ?)", 
                        uuid.toString(), 0, "BRONZE", 5.0);
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Помилка завантаження гравця: " + e.getMessage());
            }
        });
    }

    /**
     * Асинхронне збереження гравця при виході.
     */
    public void savePlayerAndRemove(UUID uuid) {
        if (!pointsCache.containsKey(uuid)) return;
        
        // Відправляємо фоновий запит на оновлення
        plugin.getDatabaseManager().executeAsync("UPDATE players SET points = ?, rank = ?, rating = ? WHERE uuid = ?",
                pointsCache.get(uuid), rankCache.get(uuid), ratingCache.get(uuid), uuid.toString());
                
        // Очищаємо оперативну пам'ять
        pointsCache.remove(uuid);
        rankCache.remove(uuid);
        ratingCache.remove(uuid);
    }

    public void modifyPointsAdmin(Player admin, String targetName, int amount, boolean add) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target != null) {
            UUID uuid = target.getUniqueId();
            int currentPoints = pointsCache.getOrDefault(uuid, 0);
            int newPoints = add ? currentPoints + amount : Math.max(0, currentPoints - amount);
            pointsCache.put(uuid, newPoints);
            
            // Зберігаємо асинхронно
            plugin.getDatabaseManager().executeAsync("UPDATE players SET points = ? WHERE uuid = ?", newPoints, uuid.toString());
            admin.sendMessage("§a[Гільдія] Очки гравця " + targetName + " оновлено!");
        } else {
            admin.sendMessage("§c[Гільдія] Гравець не знайдений або офлайн!");
        }
    }

    public void setRankAdmin(Player admin, String targetName, String rankName) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target != null) {
            UUID uuid = target.getUniqueId();
            rankCache.put(uuid, rankName);
            
            // Зберігаємо асинхронно
            plugin.getDatabaseManager().executeAsync("UPDATE players SET rank = ? WHERE uuid = ?", rankName, uuid.toString());
            admin.sendMessage("§a[Гільдія] Ранг гравця " + targetName + " змінено на " + rankName + "!");
        } else {
            admin.sendMessage("§c[Гільдія] Гравець не знайдений або офлайн!");
        }
    }

    public void setRatingAdmin(Player admin, String targetName, double rating) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target != null) {
            UUID uuid = target.getUniqueId();
            ratingCache.put(uuid, rating);
            
            // Зберігаємо асинхронно
            plugin.getDatabaseManager().executeAsync("UPDATE players SET rating = ? WHERE uuid = ?", rating, uuid.toString());
            admin.sendMessage("§a[Гільдія] Рейтинг гравця " + targetName + " змінено на " + rating + "!");
        } else {
            admin.sendMessage("§c[Гільдія] Гравець не знайдений або офлайн!");
        }
    }

    // Швидкі геттери, які миттєво беруть дані з оперативної пам'яті (кешу)
    public int getPoints(UUID uuid) { return pointsCache.getOrDefault(uuid, 0); }
    public String getRank(UUID uuid) { return rankCache.getOrDefault(uuid, "BRONZE"); }
    public double getRating(UUID uuid) { return ratingCache.getOrDefault(uuid, 5.0); }
}