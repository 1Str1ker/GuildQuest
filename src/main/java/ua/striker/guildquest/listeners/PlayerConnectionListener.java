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
        // Завантажуємо гравця, коли він заходить
        plugin.getPlayerManager().loadPlayer(event.getPlayer().getUniqueId(), event.getPlayer().getName());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Зберігаємо дані та видаляємо з кешу, коли виходить
        plugin.getPlayerManager().savePlayerAndRemove(event.getPlayer().getUniqueId());
    }
}