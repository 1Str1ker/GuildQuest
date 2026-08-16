package ua.striker.guildquest.menus;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import ua.striker.guildquest.GuildQuest;
import ua.striker.guildquest.models.GuildPlayer;

import java.util.ArrayList;
import java.util.List;

public class TopClientsMenu implements InventoryHolder {

    private final Inventory inventory;
    private final GuildQuest plugin;

    public TopClientsMenu(GuildQuest plugin) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, 36, "§8💼 Топ Замовників");
        setupMenu();
    }

    private void setupMenu() {
        // Отримуємо топ 10 замовників
        List<GuildPlayer> topPlayers = plugin.getPlayerManager().getTopClients(10);
        
        int slot = 0;
        int position = 1;
        
        for (GuildPlayer gp : topPlayers) {
            if (slot >= 27) break;

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            
            if (meta != null) {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(gp.getUuid());
                meta.setOwningPlayer(offlinePlayer);
                meta.setDisplayName("§e#" + position + " §6" + gp.getName());
                
                List<String> lore = new ArrayList<>();
                lore.add("§7Створено завдань: §a" + gp.getCreatedQuests());
                lore.add("§7Ранг: §f" + gp.getRank().getDisplayName());
                
                meta.setLore(lore);
                head.setItemMeta(meta);
            }
            
            inventory.setItem(slot, head);
            slot++;
            position++;
        }

        // Кнопка: Перемкнути на Топ Шукачів (слот 30)
        ItemStack switchBtn = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta switchMeta = switchBtn.getItemMeta();
        if (switchMeta != null) {
            switchMeta.setDisplayName("§b⬅ Топ Шукачів Пригод");
            switchMeta.setLore(List.of("§7Натисніть, щоб переглянути", "§7найкращих виконавців."));
            switchBtn.setItemMeta(switchMeta);
        }
        inventory.setItem(30, switchBtn);

        // Кнопка: Назад до дошки (слот 31)
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