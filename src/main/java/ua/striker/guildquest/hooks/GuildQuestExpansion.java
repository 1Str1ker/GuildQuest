package ua.striker.guildquest.hooks;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import ua.striker.guildquest.GuildQuest;
import ua.striker.guildquest.models.GuildPlayer;

public class GuildQuestExpansion extends PlaceholderExpansion {

    private final GuildQuest plugin;

    public GuildQuestExpansion(GuildQuest plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "guildquest"; // Це означає, що всі плейсхолдери починатимуться з %guildquest_...%
    }

    @Override
    public @NotNull String getAuthor() {
        return "1StR1ker";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true; // Щоб розширення не вимикалося при /papi reload
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null || !player.isOnline()) {
            return "";
        }

        // Отримуємо дані гравця з нашого кешу
        GuildPlayer guildPlayer = plugin.getPlayerManager().getGuildPlayer(player.getUniqueId());
        
        if (guildPlayer == null) {
            return "Завантаження...";
        }

        // Обробляємо різні запити
        return switch (params.toLowerCase()) {
            case "points" -> String.valueOf(guildPlayer.getPoints());
            case "rank_name" -> guildPlayer.getRank().name();
            case "rank_display" -> guildPlayer.getRank().getDisplayName();
            case "completed_quests" -> String.valueOf(guildPlayer.getCompletedQuests());
            case "rating" -> String.format("%.1f", guildPlayer.getRating());
            case "unique_clients" -> String.valueOf(guildPlayer.getUniqueClients());
            default -> null; // Якщо невідомий плейсхолдер
        };
    }
}