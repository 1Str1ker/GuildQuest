package ua.striker.guildquest.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import ua.striker.guildquest.GuildQuest;
import ua.striker.guildquest.menus.*;
import ua.striker.guildquest.models.Quest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MenuListener implements Listener {

    private final Map<UUID, Long> refreshCooldowns = new HashMap<>();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        GuildQuest plugin = JavaPlugin.getPlugin(GuildQuest.class);
        Player player = (Player) event.getWhoClicked();
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        // 1. ЛОГІКА ДЛЯ ГОЛОВНОЇ ДОШКИ ОГОЛОШЕНЬ
        if (event.getInventory().getHolder() instanceof QuestBoardMenu) {
            event.setCancelled(true);

            // Читаємо динамічні слоти з конфігу
            int fameSlot = plugin.getMenuConfigManager().getItemSlot("quest-board", "hall-of-fame");
            int refreshSlot = plugin.getMenuConfigManager().getItemSlot("quest-board", "refresh");
            int createSlot = plugin.getMenuConfigManager().getItemSlot("quest-board", "create-quest");
            int activeSlot = plugin.getMenuConfigManager().getItemSlot("quest-board", "active-quests");

            if (event.getSlot() == fameSlot) {
                TopAdventurersMenu topMenu = new TopAdventurersMenu(plugin);
                player.openInventory(topMenu.getInventory());
                return;
            }

            if (event.getSlot() == refreshSlot) {
                long currentTime = System.currentTimeMillis();
                long lastRefresh = refreshCooldowns.getOrDefault(player.getUniqueId(), 0L);
                
                if (currentTime - lastRefresh < 3000) {
                    long timeLeft = (3000 - (currentTime - lastRefresh)) / 1000;
                    player.sendMessage("§c[Гільдія] Зачекайте ще " + timeLeft + " сек. перед оновленням!");
                    return;
                }
                
                refreshCooldowns.put(player.getUniqueId(), currentTime);
                QuestBoardMenu newMenu = new QuestBoardMenu(plugin);
                player.openInventory(newMenu.getInventory());
                return;
            }

            if (event.getSlot() == createSlot) {
                player.closeInventory();
                player.sendMessage("§e[Гільдія] Щоб створити замовлення, використовуйте команду:");
                player.sendMessage("§f/gq create <нагорода>");
                return;
            }

            if (event.getSlot() == activeSlot) {
                ActiveQuestsMenu activeMenu = new ActiveQuestsMenu(plugin, player);
                player.openInventory(activeMenu.getInventory());
                return;
            }

            if (clickedItem.getType() == Material.PAPER && clickedItem.hasItemMeta()) {
                String displayName = clickedItem.getItemMeta().getDisplayName();
                if (displayName.startsWith("§eКонтракт #")) {
                    try {
                        int questId = Integer.parseInt(displayName.replace("§eКонтракт #", ""));
                        QuestConfirmMenu confirmMenu = new QuestConfirmMenu(plugin, questId);
                        player.openInventory(confirmMenu.getInventory());
                    } catch (NumberFormatException e) {
                        player.sendMessage("§c[Гільдія] Сталася помилка при читанні номера контракту.");
                    }
                }
            }
        } 
        
        // 2. ЛОГІКА ДЛЯ МЕНЮ АКТИВНИХ КОНТРАКТІВ
        else if (event.getInventory().getHolder() instanceof ActiveQuestsMenu) {
            event.setCancelled(true);

            if (event.getSlot() == 31) {
                QuestBoardMenu boardMenu = new QuestBoardMenu(plugin);
                player.openInventory(boardMenu.getInventory());
                return;
            }

            if (clickedItem.getType() == Material.PAPER && clickedItem.hasItemMeta()) {
                String displayName = clickedItem.getItemMeta().getDisplayName();
                if (displayName.startsWith("§aВ процесі #")) {
                    try {
                        int questId = Integer.parseInt(displayName.replace("§aВ процесі #", ""));
                        
                        List<Quest> activeQuests = plugin.getQuestManager().getActiveQuestsFor(player.getUniqueId());
                        for (Quest q : activeQuests) {
                            if (q.getQuestId() == questId) {
                                player.closeInventory();
                                plugin.getQuestManager().submitQuestItems(player, q);
                                break;
                            }
                        }
                    } catch (NumberFormatException e) {
                        player.sendMessage("§c[Гільдія] Сталася помилка при читанні номера.");
                    }
                }
            }
        }

        // 3. ЛОГІКА ДЛЯ МЕНЮ ТОП ШУКАЧІВ ПРИГОД
        else if (event.getInventory().getHolder() instanceof TopAdventurersMenu) {
            event.setCancelled(true);
            
            if (event.getSlot() == 31) { 
                QuestBoardMenu boardMenu = new QuestBoardMenu(plugin);
                player.openInventory(boardMenu.getInventory());
            } else if (event.getSlot() == 32) { 
                TopClientsMenu clientsMenu = new TopClientsMenu(plugin);
                player.openInventory(clientsMenu.getInventory());
            }
        }

        // 4. ЛОГІКА ДЛЯ МЕНЮ ТОП ЗАМОВНИКІВ
        else if (event.getInventory().getHolder() instanceof TopClientsMenu) {
            event.setCancelled(true);
            
            if (event.getSlot() == 31) { 
                QuestBoardMenu boardMenu = new QuestBoardMenu(plugin);
                player.openInventory(boardMenu.getInventory());
            } else if (event.getSlot() == 30) { 
                TopAdventurersMenu adventurersMenu = new TopAdventurersMenu(plugin);
                player.openInventory(adventurersMenu.getInventory());
            }
        }
        
        // 5. ЛОГІКА ДЛЯ МЕНЮ ВИБОРУ ПРЕДМЕТА
        else if (event.getInventory().getHolder() instanceof ItemSelectionMenu menu) {
            event.setCancelled(true);
            if (clickedItem.getType() != Material.AIR) {
                AmountSelectionMenu amountMenu = new AmountSelectionMenu(plugin, menu.getReward(), clickedItem.getType(), 1);
                player.openInventory(amountMenu.getInventory());
            }
        }

        // 6. ЛОГІКА ДЛЯ МЕНЮ ВИБОРУ КІЛЬКОСТІ
        else if (event.getInventory().getHolder() instanceof AmountSelectionMenu menu) {
            event.setCancelled(true);
            int currentAmount = menu.getAmount();

            switch (event.getSlot()) {
                case 10 -> currentAmount -= 64;
                case 11 -> currentAmount -= 10;
                case 12 -> currentAmount -= 1;
                case 14 -> currentAmount += 1;
                case 15 -> currentAmount += 10;
                case 16 -> currentAmount += 64;
                case 22 -> { // Підтвердження
                    player.closeInventory();
                    
                    double commPercent = plugin.getConfigManager().getCommissionPercent();
                    double totalCost = menu.getReward() + (menu.getReward() * (commPercent / 100.0));

                    if (!GuildQuest.getEconomy().has(player, totalCost)) {
                        player.sendMessage("§c[Гільдія] Недостатньо коштів! Потрібно: " + String.format("%.2f", totalCost));
                        return;
                    }

                    GuildQuest.getEconomy().withdrawPlayer(player, totalCost);
                    plugin.getQuestManager().createQuest(player.getUniqueId(), menu.getTargetItem().name(), menu.getAmount(), menu.getReward());
                    player.sendMessage(plugin.getMessageManager().getMessage("quest-created"));
                    return;
                }
            }

            if (event.getSlot() >= 10 && event.getSlot() <= 16) {
                AmountSelectionMenu newMenu = new AmountSelectionMenu(plugin, menu.getReward(), menu.getTargetItem(), currentAmount);
                player.openInventory(newMenu.getInventory());
            }
        }

        // 7. ЛОГІКА ДЛЯ МЕНЮ ПІДТВЕРДЖЕННЯ ПРИЙНЯТТЯ КВЕСТУ
        else if (event.getInventory().getHolder() instanceof QuestConfirmMenu menu) {
            event.setCancelled(true);
            
            // Читаємо слоти з конфігу
            int acceptSlot = plugin.getMenuConfigManager().getItemSlot("quest-confirm", "accept");
            int cancelSlot = plugin.getMenuConfigManager().getItemSlot("quest-confirm", "cancel");

            if (event.getSlot() == acceptSlot) { 
                player.closeInventory();
                plugin.getQuestManager().acceptQuest(player, menu.getQuestId());
            } else if (event.getSlot() == cancelSlot) { 
                QuestBoardMenu boardMenu = new QuestBoardMenu(plugin);
                player.openInventory(boardMenu.getInventory());
            }
        }
    }
}