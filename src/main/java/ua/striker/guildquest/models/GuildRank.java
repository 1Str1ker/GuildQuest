package ua.striker.guildquest.models;

public enum GuildRank {
    COPPER("Мідний", 1),
    SILVER("Срібний", 2),
    GOLD("Золотий", 3),
    PLATINUM("Платиновий", 4),
    MITHRIL("Міфриловий", 5);

    private final String displayName;
    private final int maxConcurrentQuests; // Скільки квестів гравець може брати одночасно

    GuildRank(String displayName, int maxConcurrentQuests) {
        this.displayName = displayName;
        this.maxConcurrentQuests = maxConcurrentQuests;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMaxConcurrentQuests() {
        return maxConcurrentQuests;
    }
}