package ua.striker.guildquest.managers;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import ua.striker.guildquest.GuildQuest;
import ua.striker.guildquest.models.GuildPlayer;

import java.util.ArrayList;
import java.util.List;

public class HologramManager {

    private final GuildQuest plugin;
    private final String holoName = "GuildQuest_Top";
    
    public HologramManager(GuildQuest plugin) {
        this.plugin = plugin;
    }

    // Створення або оновлення голограми
    public void updateTopHologram(Location location) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<GuildPlayer> topPlayers = plugin.getPlayerManager().getTopAdventurers(10);
            
            List<String> lines = new ArrayList<>();
            lines.add("§8⚔ ======================= ⚔");
            lines.add("§6🏆 §lТоп Шукачів Пригод §6🏆");
            lines.add("");
            
            int position = 1;
            for (GuildPlayer gp : topPlayers) {
                lines.add("§e#" + position + " §f" + gp.getName() + " §7- " + gp.getRank().getDisplayName() + " §8(§d" + gp.getPoints() + " оч.§8)");
                position++;
            }
            if (topPlayers.isEmpty()) {
                lines.add("§7Поки що немає даних...");
            }
            lines.add("");
            lines.add("§8⚔ ======================= ⚔");

            Bukkit.getScheduler().runTask(plugin, () -> {
                Hologram hologram = DHAPI.getHologram(holoName);
                if (hologram == null) {
                    if (location != null) {
                        DHAPI.createHologram(holoName, location, lines);
                        plugin.getLogger().info("Голограму Топу успішно створено!");
                    }
                } else {
                    DHAPI.setHologramLines(hologram, lines);
                    if (location != null) {
                        DHAPI.moveHologram(hologram, location);
                    }
                }
            });
        });
    }
    
    public void deleteHologram() {
        Hologram hologram = DHAPI.getHologram(holoName);
        if (hologram != null) {
            hologram.delete();
        }
    }
}