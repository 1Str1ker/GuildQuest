package ua.striker.guildquest.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ua.striker.guildquest.GuildQuest;

public class PlayerConnectionListener implements Listener {

    private final GuildQuest plugin;

    public PlayerConnectionListener(GuildQuest plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Завантажуємо дані гравця при вході
        plugin.getPlayerManager().loadPlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Зберігаємо дані гравця та видаляємо з пам'яті при виході
        plugin.getPlayerManager().savePlayerAndRemove(event.getPlayer().getUniqueId());
    }
}