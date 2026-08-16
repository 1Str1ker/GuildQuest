package ua.striker.guildquest.models;

public enum GuildRank {
    COPPER("§cМідний", 0, 0),
    IRON("§fЗалізний", 100, 2),
    GOLD("§6Золотий", 500, 5),
    EMERALD("§aСмарагдовий", 1000, 10),
    DIAMOND("§bДіамантовий", 2500, 20),
    NETHERITE("§8Незеритовий", 5000, 30);

    private final String displayName;
    private final int requiredPoints;
    private final int requiredUniqueClients;

    GuildRank(String displayName, int requiredPoints, int requiredUniqueClients) {
        this.displayName = displayName;
        this.requiredPoints = requiredPoints;
        this.requiredUniqueClients = requiredUniqueClients;
    }

    public String getDisplayName() { return displayName; }
    public int getRequiredPoints() { return requiredPoints; }
    public int getRequiredUniqueClients() { return requiredUniqueClients; }

    // ОНОВЛЕНО: Тепер приймає boolean прапорець з конфігу
    public static GuildRank calculateRank(int points, int uniqueClients, boolean requireUnique) {
        GuildRank currentRank = COPPER;
        for (GuildRank rank : values()) {
            if (points >= rank.requiredPoints) {
                if (!requireUnique || uniqueClients >= rank.requiredUniqueClients) {
                    currentRank = rank;
                }
            }
        }
        return currentRank;
    }
}