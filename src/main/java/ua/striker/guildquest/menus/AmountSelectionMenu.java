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

public class AmountSelectionMenu implements InventoryHolder {
    private final Inventory inventory;
    private final double reward;
    private final Material targetItem;
    private final int amount;
    private final GuildQuest plugin;

    public AmountSelectionMenu(GuildQuest plugin, double reward, Material targetItem, int amount) {
        this.plugin = plugin;
        this.reward = reward;
        this.targetItem = targetItem;
        this.amount = Math.max(1, amount); // Кількість не може бути менше 1
        this.inventory = Bukkit.createInventory(this, 27, "§8Кількість: " + this.amount);
        setupMenu();
    }

    private void setupMenu() {
        // Центральний предмет
        ItemStack center = new ItemStack(targetItem, Math.min(64, amount));
        ItemMeta centerMeta = center.getItemMeta();
        if (centerMeta != null) {
            centerMeta.setDisplayName("§eОбрано: §f" + amount + "x " + targetItem.name());
            centerMeta.setLore(List.of("§7Нагорода: §a" + reward + " монет"));
            center.setItemMeta(centerMeta);
        }
        inventory.setItem(13, center);

        // Кнопки віднімання
        inventory.setItem(10, createBtn(Material.RED_STAINED_GLASS_PANE, "§c-64", "Відняти стак"));
        inventory.setItem(11, createBtn(Material.RED_STAINED_GLASS_PANE, "§c-10", "Відняти 10"));
        inventory.setItem(12, createBtn(Material.RED_STAINED_GLASS_PANE, "§c-1", "Відняти 1"));

        // Кнопки додавання
        inventory.setItem(14, createBtn(Material.GREEN_STAINED_GLASS_PANE, "§a+1", "Додати 1"));
        inventory.setItem(15, createBtn(Material.GREEN_STAINED_GLASS_PANE, "§a+10", "Додати 10"));
        inventory.setItem(16, createBtn(Material.GREEN_STAINED_GLASS_PANE, "§a+64", "Додати стак"));

        // Кнопка підтвердження
        double commPercent = plugin.getConfigManager().getCommissionPercent();
        double commAmount = reward * (commPercent / 100.0);
        double totalCost = reward + commAmount;
        
        ItemStack confirm = new ItemStack(Material.EMERALD);
        ItemMeta confirmMeta = confirm.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.setDisplayName("§a✔ Підтвердити створення");
            confirmMeta.setLore(List.of(
                "§7До сплати: §e" + String.format("%.2f", totalCost) + " монет",
                "§8(вкл. комісію " + commPercent + "%)"
            ));
            confirm.setItemMeta(confirmMeta);
        }
        inventory.setItem(22, confirm);
    }

    private ItemStack createBtn(Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(List.of("§7" + lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    public double getReward() { return reward; }
    public Material getTargetItem() { return targetItem; }
    public int getAmount() { return amount; }

    @NotNull
    @Override
    public Inventory getInventory() { return inventory; }
}