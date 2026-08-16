package ua.striker.guildquest.hooks;

import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitName;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.java.JavaPlugin;
import ua.striker.guildquest.GuildQuest;
import ua.striker.guildquest.menus.QuestBoardMenu;

// Назва трейту, яку адмін буде писати в грі: /trait guildquest
@TraitName("guildquest")
public class GuildQuestTrait extends Trait {

    private GuildQuest plugin;

    public GuildQuestTrait() {
        super("guildquest");
        this.plugin = JavaPlugin.getPlugin(GuildQuest.class);
    }

    @EventHandler
    public void click(NPCRightClickEvent event) {
        // Якщо клікнули саме на нашого NPC
        if (event.getNPC() == this.getNPC()) {
            Player player = event.getClicker();
            
            // Відкриваємо головну дошку
            QuestBoardMenu menu = new QuestBoardMenu(plugin);
            player.openInventory(menu.getInventory());
        }
    }
}