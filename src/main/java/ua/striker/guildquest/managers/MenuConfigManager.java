package ua.striker.guildquest.managers;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ua.striker.guildquest.GuildQuest;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MenuConfigManager {

    private final GuildQuest plugin;
    private FileConfiguration menusConfig;
    private File menusFile;

    public MenuConfigManager(GuildQuest plugin) {
        this.plugin = plugin;
        loadMenus();
    }

    public void loadMenus() {
        menusFile = new File(plugin.getDataFolder(), "menus.yml");
        if (!menusFile.exists()) {
            menusFile.getParentFile().mkdirs();
            plugin.saveResource("menus.yml", false);
        }
        menusConfig = YamlConfiguration.loadConfiguration(menusFile);
    }

    public String getMenuTitle(String menuName) {
        String title = menusConfig.getString(menuName + ".title", "&cMenu Not Found");
        return ChatColor.translateAlternateColorCodes('&', title);
    }

    public int getMenuSize(String menuName) {
        return menusConfig.getInt(menuName + ".size", 54);
    }

    public int getItemSlot(String menuName, String itemKey) {
        return menusConfig.getInt(menuName + ".items." + itemKey + ".slot", 0);
    }

    public ItemStack getConfigItem(String menuName, String itemKey) {
        String path = menuName + ".items." + itemKey;
        
        String matName = menusConfig.getString(path + ".material", "DIRT").toUpperCase();
        Material material = Material.matchMaterial(matName);
        if (material == null) material = Material.DIRT;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            String name = menusConfig.getString(path + ".name");
            if (name != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            }
            
            List<String> lore = menusConfig.getStringList(path + ".lore");
            if (!lore.isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                meta.setLore(coloredLore);
            }
            item.setItemMeta(meta);
        }
        
        return item;
    }
}