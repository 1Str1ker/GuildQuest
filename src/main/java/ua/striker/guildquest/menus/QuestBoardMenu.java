package ua.striker.guildquest.menus;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import ua.striker.guildquest.GuildQuest;
import ua.striker.guildquest.models.Quest;

import java.util.ArrayList;
import java.util.List;

public class QuestBoardMenu implements InventoryHolder {

    private final Inventory inventory;
    private final GuildQuest plugin;

    public QuestBoardMenu(GuildQuest plugin) {
        this.plugin = plugin;
        String title = plugin.getMenuConfigManager().getMenuTitle("quest-board");
        int size = plugin.getMenuConfigManager().getMenuSize("quest-board");
        this.inventory = Bukkit.createInventory(this, size, title);
        setupMenu();
    }

    private void setupMenu() {
        List<Quest> openQuests = plugin.getQuestManager().getOpenQuests();

        int slot = 0;
        // Залишаємо нижній ряд для кнопок, тому віднімаємо 9 від загального розміру
        int maxQuestSlots = inventory.getSize() - 9; 

        for (Quest quest : openQuests) {
            if (slot >= maxQuestSlots) break;

            ItemStack questIcon = new ItemStack(Material.PAPER);
            ItemMeta meta = questIcon.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§eКонтракт #" + quest.getQuestId());
                List<String> lore = new ArrayList<>();
                lore.add("§7Ціль: §f" + quest.getAmount() + "x " + quest.getTargetItem());
                lore.add("§7Нагорода: §a" + quest.getReward() + " монет");
                lore.add("");
                lore.add("§eКлікніть, щоб прийняти!");
                meta.setLore(lore);
                questIcon.setItemMeta(meta);
            }
            inventory.setItem(slot, questIcon);
            slot++;
        }

        // Динамічне завантаження кнопок із menus.yml
        inventory.setItem(plugin.getMenuConfigManager().getItemSlot("quest-board", "hall-of-fame"), 
                plugin.getMenuConfigManager().getConfigItem("quest-board", "hall-of-fame"));
                
        inventory.setItem(plugin.getMenuConfigManager().getItemSlot("quest-board", "refresh"), 
                plugin.getMenuConfigManager().getConfigItem("quest-board", "refresh"));
                
        inventory.setItem(plugin.getMenuConfigManager().getItemSlot("quest-board", "create-quest"), 
                plugin.getMenuConfigManager().getConfigItem("quest-board", "create-quest"));
                
        inventory.setItem(plugin.getMenuConfigManager().getItemSlot("quest-board", "active-quests"), 
                plugin.getMenuConfigManager().getConfigItem("quest-board", "active-quests"));
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}