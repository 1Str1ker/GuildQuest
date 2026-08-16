package ua.striker.guildquest.menus;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import ua.striker.guildquest.GuildQuest;

public class QuestConfirmMenu implements InventoryHolder {
    private final Inventory inventory;
    private final int questId;

    public QuestConfirmMenu(GuildQuest plugin, int questId) {
        this.questId = questId;
        
        // Завантажуємо дані з конфігу і замінюємо плейсхолдер %id%
        String title = plugin.getMenuConfigManager().getMenuTitle("quest-confirm").replace("%id%", String.valueOf(questId));
        int size = plugin.getMenuConfigManager().getMenuSize("quest-confirm");
        this.inventory = Bukkit.createInventory(this, size, title);
        
        // Динамічне завантаження кнопок
        inventory.setItem(plugin.getMenuConfigManager().getItemSlot("quest-confirm", "accept"), 
                plugin.getMenuConfigManager().getConfigItem("quest-confirm", "accept"));
                
        inventory.setItem(plugin.getMenuConfigManager().getItemSlot("quest-confirm", "cancel"), 
                plugin.getMenuConfigManager().getConfigItem("quest-confirm", "cancel"));
    }

    public int getQuestId() { return questId; }

    @NotNull
    @Override
    public Inventory getInventory() { return inventory; }
}