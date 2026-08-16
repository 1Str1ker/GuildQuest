package ua.striker.guildquest.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ua.striker.guildquest.GuildQuest;

public class PlayerConnectionListener implements Listener {

    private final GuildQuest plugin;

    public PlayerConnectionListener(GuildQuest plugin) {
        this.plugin = plugin;
    }

    // Використовуємо NORMAL, оскільки це стандартне фонове завантаження
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getPlayerManager().loadPlayer(event.getPlayer().getUniqueId());
    }

    // Встановлюємо HIGH, щоб збереження відбулося навіть якщо інший плагін "кікнув" гравця
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getPlayerManager().savePlayerAndRemove(event.getPlayer().getUniqueId());
    }
}