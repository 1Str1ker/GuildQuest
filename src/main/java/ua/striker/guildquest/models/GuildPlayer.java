package ua.striker.guildquest.models;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ua.striker.guildquest.GuildQuest;

import java.util.UUID;

public class GuildPlayer {
    private final UUID uuid;
    private final String name;
    private GuildRank rank;
    private int points;
    private double money;
    private int completedQuests;
    private double rating;
    private int uniqueClients;
    private int createdQuests;

    public GuildPlayer(UUID uuid, String name, GuildRank rank, int points, double money, int completedQuests, double rating, int uniqueClients, int createdQuests) {
        this.uuid = uuid;
        this.name = name;
        this.rank = rank;
        this.points = points;
        this.money = money;
        this.completedQuests = completedQuests;
        this.rating = rating;
        this.uniqueClients = uniqueClients;
        this.createdQuests = createdQuests;
    }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public GuildRank getRank() { return rank; }
    public int getPoints() { return points; }
    public double getMoney() { return money; }
    public int getCompletedQuests() { return completedQuests; }
    public double getRating() { return rating; }
    public int getUniqueClients() { return uniqueClients; }
    public int getCreatedQuests() { return createdQuests; }

    public void addPoints(int amount) { 
        this.points += amount; 
        checkRankUpdate();
    }
    
    public void addMoney(double amount) { this.money += amount; }
    public void addCompletedQuest() { this.completedQuests++; }
    public void addCreatedQuest() { this.createdQuests++; }
    public void setRating(double rating) { this.rating = rating; }
    public void setRank(GuildRank rank) { this.rank = rank; }
    
    public void setUniqueClients(int amount) {
        this.uniqueClients = amount;
        checkRankUpdate();
    }

    private void checkRankUpdate() {
        GuildQuest plugin = JavaPlugin.getPlugin(GuildQuest.class);
        boolean requireUnique = plugin.getConfigManager().isUniqueClientsRequired();
        
        GuildRank newRank = GuildRank.calculateRank(this.points, this.uniqueClients, requireUnique);
        
        if (this.rank != newRank) {
            boolean isUpgrade = newRank.getRequiredPoints() > this.rank.getRequiredPoints();
            this.rank = newRank;

            // Ефекти та сповіщення для онлайн-гравця
            Player player = Bukkit.getPlayer(this.uuid);
            if (player != null && player.isOnline()) {
                if (isUpgrade) {
                    player.sendTitle("§6🏆 Ранг Підвищено!", "§fВаш новий ранг: " + newRank.getDisplayName(), 10, 70, 20);
                    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                    player.sendMessage("§a[Гільдія] Вітаємо! Ваш ранг гільдії підвищено до " + newRank.getDisplayName() + "§a!");
                } else {
                    player.sendMessage("§c[Гільдія] Ваш ранг гільдії було змінено на " + newRank.getDisplayName() + "§c.");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
            }

            // Миттєве збереження нового рангу в базу даних, щоб оновилися топи
            plugin.getPlayerManager().updateRankInDatabase(this.uuid, this.rank);
        }
    }
}