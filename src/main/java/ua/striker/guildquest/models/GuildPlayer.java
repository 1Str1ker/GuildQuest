package ua.striker.guildquest.models;

import java.util.UUID;

public class GuildPlayer {
    private final UUID uuid;
    private final String name;
    private GuildRank rank;
    private int points;
    private double money;
    private int completedQuests;
    private double rating;

    public GuildPlayer(UUID uuid, String name, GuildRank rank, int points, double money, int completedQuests, double rating) {
        this.uuid = uuid;
        this.name = name;
        this.rank = rank;
        this.points = points;
        this.money = money;
        this.completedQuests = completedQuests;
        this.rating = rating;
    }

    // Гетери (для отримання даних)
    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public GuildRank getRank() { return rank; }
    public int getPoints() { return points; }
    public double getMoney() { return money; }
    public int getCompletedQuests() { return completedQuests; }
    public double getRating() { return rating; }

    // Сетери (для зміни даних під час гри)
    public void setRank(GuildRank rank) { this.rank = rank; }
    public void addPoints(int amount) { this.points += amount; }
    public void addMoney(double amount) { this.money += amount; }
    public void addCompletedQuest() { this.completedQuests++; }
    public void setRating(double rating) { this.rating = rating; }
}