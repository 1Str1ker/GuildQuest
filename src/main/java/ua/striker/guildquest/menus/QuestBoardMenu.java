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
        // Створюємо інвентар на 54 слоти (6 рядків) із заголовком
        this.inventory = Bukkit.createInventory(this, 54, "§8⚔ Дошка оголошень");
        setupMenu();
    }

    private void setupMenu() {
        // Отримуємо список відкритих квестів із бази даних
        List<Quest> openQuests = plugin.getQuestManager().getOpenQuests();

        int slot = 0;
        for (Quest quest : openQuests) {
            if (slot >= 45) break; // Залишаємо нижній рядок для системних кнопок

            // Створюємо іконку для кожного квесту (наприклад, папір або цільовий предмет)
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

        // Додаємо кнопку "Створити завдання" в слот 49 (низу по центру)
        ItemStack createQuestBtn = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta createMeta = createQuestBtn.getItemMeta();
        if (createMeta != null) {
            createMeta.setDisplayName("§a+ Створити замовлення");
            createMeta.setLore(List.of(
                    "§7Натисніть, щоб додати",
                    "§7своє завдання на дошку."
            ));
            createQuestBtn.setItemMeta(createMeta);
        }
        
        inventory.setItem(49, createQuestBtn);
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}