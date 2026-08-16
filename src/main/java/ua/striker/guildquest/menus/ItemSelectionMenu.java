package ua.striker.guildquest.menus;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import ua.striker.guildquest.GuildQuest;

import java.util.List;
import java.util.Set;

public class ItemSelectionMenu implements InventoryHolder {
    private final Inventory inventory;
    private final double reward;

    public ItemSelectionMenu(GuildQuest plugin, double reward) {
        this.reward = reward;
        this.inventory = Bukkit.createInventory(this, 54, "§8Оберіть предмет");
        
        Set<String> items = plugin.getConfigManager().getConfiguredItems();
        int slot = 0;
        
        for (String itemName : items) {
            if (slot >= 53) break;
            Material mat = Material.matchMaterial(itemName);
            if (mat != null && mat != Material.AIR) {
                ItemStack item = new ItemStack(mat);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setLore(List.of("§7Натисніть, щоб обрати цей", "§7предмет для замовлення."));
                    item.setItemMeta(meta);
                }
                inventory.setItem(slot, item);
                slot++;
            }
        }
    }

    public double getReward() { return reward; }

    @NotNull
    @Override
    public Inventory getInventory() { return inventory; }
}