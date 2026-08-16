package ua.striker.guildquest.menus;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import ua.striker.guildquest.GuildQuest;
import ua.striker.guildquest.models.Quest;

import java.util.ArrayList;
import java.util.List;

public class ActiveQuestsMenu implements InventoryHolder {

    private final Inventory inventory;
    private final GuildQuest plugin;
    private final Player player;

    public ActiveQuestsMenu(GuildQuest plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        // Створюємо інвентар на 36 слотів (4 рядки)
        this.inventory = Bukkit.createInventory(this, 36, "§8⚔ Мої активні контракти");
        setupMenu();
    }

    private void setupMenu() {
        // Отримуємо лише ті квести, які цей гравець взяв на виконання
        List<Quest> activeQuests = plugin.getQuestManager().getActiveQuestsFor(player.getUniqueId());

        int slot = 0;
        for (Quest quest : activeQuests) {
            if (slot >= 27) break; // Залишаємо нижній рядок для кнопок

            ItemStack questIcon = new ItemStack(Material.PAPER);
            ItemMeta meta = questIcon.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§aВ процесі #" + quest.getQuestId());
                
                List<String> lore = new ArrayList<>();
                lore.add("§7Потрібно здати: §f" + quest.getAmount() + "x " + quest.getTargetItem());
                lore.add("§7Ваша нагорода: §a" + quest.getReward() + " монет");
                lore.add("");
                lore.add("§eКлікніть, щоб здати ресурси!");
                
                meta.setLore(lore);
                questIcon.setItemMeta(meta);
            }
            inventory.setItem(slot, questIcon);
            slot++;
        }

        // Кнопка повернення до головного меню
        ItemStack backBtn = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backBtn.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§cНазад до дошки");
            backBtn.setItemMeta(backMeta);
        }
        inventory.setItem(31, backBtn);
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}