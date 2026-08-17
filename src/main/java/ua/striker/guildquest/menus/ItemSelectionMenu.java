package ua.striker.guildquest.menus;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ua.striker.guildquest.GuildQuest;

import java.util.ArrayList;
import java.util.List;

public class ItemSelectionMenu implements InventoryHolder {

    private final Inventory inventory;
    private final double reward;
    private final int page;

    public ItemSelectionMenu(GuildQuest plugin, double reward, int page) {
        this.reward = reward;
        this.page = page;
        
        this.inventory = Bukkit.createInventory(this, 54, "§8Виберіть предмет (Стор. " + (page + 1) + ")");

        // Читаємо саме твій розділ з конфігу
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("points.items");
        if (section == null) return;
        
        List<String> materials = new ArrayList<>(section.getKeys(false));
        double defaultPoints = plugin.getConfig().getDouble("points.default-points", 1.0);
        
        int maxItemsPerPage = 45;
        int startIndex = page * maxItemsPerPage;
        int endIndex = Math.min(startIndex + maxItemsPerPage, materials.size());
        
        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            String matName = materials.get(i);
            Material mat = Material.matchMaterial(matName.toUpperCase());
            
            if (mat != null && mat != Material.AIR) {
                double pointsPerItem = section.getDouble(matName, defaultPoints);
                
                ItemStack item = new ItemStack(mat);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§a" + mat.toString().replace("_", " "));
                    List<String> lore = new ArrayList<>();
                    lore.add("§7Натисніть, щоб створити замовлення.");
                    lore.add("");
                    lore.add("§e💰 Нагорода: §f" + reward);
                    lore.add("§b🌟 Очок за 1 шт: §f" + pointsPerItem);
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
                inventory.setItem(slot, item);
                slot++;
            }
        }
        
        if (page > 0) {
            inventory.setItem(45, createNavButton(Material.ARROW, "§c⬅ Попередня сторінка"));
        }
        
        if (endIndex < materials.size()) {
            inventory.setItem(53, createNavButton(Material.ARROW, "§aНаступна сторінка ➡"));
        }
    }

    private ItemStack createNavButton(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
    
    public double getReward() {
        return reward;
    }
    
    public int getPage() {
        return page;
    }
}