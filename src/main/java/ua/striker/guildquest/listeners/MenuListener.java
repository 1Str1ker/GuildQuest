package ua.striker.guildquest.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import ua.striker.guildquest.menus.QuestBoardMenu;

public class MenuListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Перевіряємо, чи клік відбувся саме в нашому меню "Дошка оголошень"
        if (event.getInventory().getHolder() instanceof QuestBoardMenu) {
            
            // Обов'язково скасовуємо івент, щоб гравець не міг забрати предмет
            event.setCancelled(true);
            
            // Якщо гравець клікнув на порожнє місце, нічого не робимо
            if (event.getCurrentItem() == null) return;
            
            Player player = (Player) event.getWhoClicked();

            // Перевіряємо, на який слот клікнув гравець
            if (event.getSlot() == 49) {
                // Гравець клікнув на кнопку "Створити замовлення"
                player.closeInventory();
                player.sendMessage("§e[Гільдія] Функція створення квесту розробляється!");
            }
        }
    }
}