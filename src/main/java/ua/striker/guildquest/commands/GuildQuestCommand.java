package ua.striker.guildquest.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ua.striker.guildquest.GuildQuest;
import ua.striker.guildquest.managers.MessageManager;
import ua.striker.guildquest.menus.ItemSelectionMenu;
import ua.striker.guildquest.menus.QuestBoardMenu;

import java.util.UUID;

public class GuildQuestCommand implements CommandExecutor {

    private final GuildQuest plugin;

    public GuildQuestCommand(GuildQuest plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        MessageManager msgManager = plugin.getMessageManager();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(msgManager.getMessage("player-only"));
            return true;
        }

        if (args.length == 0) {
            QuestBoardMenu menu = new QuestBoardMenu(plugin);
            player.openInventory(menu.getInventory());
            return true;
        }

        if (args[0].equalsIgnoreCase("help")) {
            player.sendMessage("§8⚔ ================= §6Гільдія Пригод §8================= ⚔");
            player.sendMessage("§e/gq §7- Відкрити Дошку оголошень");
            player.sendMessage("§e/gq create <нагорода> §7- Відкрити меню створення замовлення");
            player.sendMessage("§e/gq collect §7- Забрати ресурси з виконаних завдань");
            
            if (player.hasPermission("guildquest.admin")) {
                player.sendMessage("§c/gq admin help §7- Панель адміністратора");
                player.sendMessage("§c/gq reload §7- Перезавантажити конфігурацію");
            }
            player.sendMessage("§8===================================================");
            return true;
        }
        
        if (args[0].equalsIgnoreCase("admin")) {
            if (!player.hasPermission("guildquest.admin")) {
                player.sendMessage(msgManager.getMessage("no-permission"));
                return true;
            }
            
            if (args.length < 2 || args[1].equalsIgnoreCase("help")) {
                player.sendMessage("§8⚔ ================= §cАдмін Панель §8================= ⚔");
                player.sendMessage("§e/gq admin quest delete <id> §7- Видалити квест та повернути гроші");
                player.sendMessage("§e/gq admin points add <гравець> <кількість> §7- Видати очки");
                player.sendMessage("§e/gq admin points remove <гравець> <кількість> §7- Забрати очки");
                player.sendMessage("§e/gq admin rank set <гравець> <ранг> §7- Змінити ранг (напр. GOLD)");
                player.sendMessage("§e/gq admin rating set <гравець> <оцінка> §7- Примусово встановити рейтинг");
                player.sendMessage("§e/gq admin hologram set §7- Встановити голограму Топу на вашому місці");
                player.sendMessage("§e/gq admin hologram delete §7- Видалити голограму");
                player.sendMessage("§8===================================================");
                return true;
            }

            if (args[1].equalsIgnoreCase("hologram") && args.length == 3) {
                if (args[2].equalsIgnoreCase("set")) {
                    if (plugin.getHologramManager() != null) {
                        plugin.getHologramManager().updateTopHologram(player.getLocation());
                        player.sendMessage("§a[Гільдія] Голограму успішно встановлено!");
                    } else {
                        player.sendMessage("§c[Гільдія] Плагін DecentHolograms не знайдено на сервері.");
                    }
                } else if (args[2].equalsIgnoreCase("delete")) {
                    if (plugin.getHologramManager() != null) {
                        plugin.getHologramManager().deleteHologram();
                        player.sendMessage("§a[Гільдія] Голограму видалено.");
                    }
                }
                return true;
            }

            if (args[1].equalsIgnoreCase("quest") && args.length == 4 && args[2].equalsIgnoreCase("delete")) {
                try {
                    int questId = Integer.parseInt(args[3]);
                    plugin.getQuestManager().deleteQuestAdmin(player, questId);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cID квесту має бути числом!");
                }
                return true;
            }

            if (args[1].equalsIgnoreCase("points") && args.length == 5) {
                String action = args[2];
                String targetName = args[3];
                try {
                    int amount = Integer.parseInt(args[4]);
                    if (action.equalsIgnoreCase("add")) {
                        plugin.getPlayerManager().modifyPointsAdmin(player, targetName, amount, true);
                    } else if (action.equalsIgnoreCase("remove")) {
                        plugin.getPlayerManager().modifyPointsAdmin(player, targetName, amount, false);
                    } else {
                        player.sendMessage("§cВикористовуйте add або remove.");
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage("§cКількість має бути числом!");
                }
                return true;
            }

            if (args[1].equalsIgnoreCase("rank") && args.length == 5 && args[2].equalsIgnoreCase("set")) {
                String targetName = args[3];
                String rankName = args[4];
                plugin.getPlayerManager().setRankAdmin(player, targetName, rankName);
                return true;
            }

            if (args[1].equalsIgnoreCase("rating") && args.length == 5 && args[2].equalsIgnoreCase("set")) {
                String targetName = args[3];
                try {
                    double newRating = Double.parseDouble(args[4]);
                    if (newRating < 0.0 || newRating > 5.0) {
                        player.sendMessage("§cРейтинг має бути від 0.0 до 5.0!");
                        return true;
                    }
                    plugin.getPlayerManager().setRatingAdmin(player, targetName, newRating);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cРейтинг має бути числом (наприклад: 4.5)!");
                }
                return true;
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("create")) {
            if (args.length < 2) {
                player.sendMessage("§cВикористання: /gq create <нагорода>");
                return true;
            }
            
            try {
                double reward = Double.parseDouble(args[1]);
                if (reward <= 0) {
                    player.sendMessage(msgManager.getMessage("amount-invalid"));
                    return true;
                }
                
                if (!GuildQuest.getEconomy().has(player, reward)) {
                    String msg = msgManager.getMessage("not-enough-money").replace("%amount%", String.format("%.2f", reward));
                    player.sendMessage(msg);
                    return true;
                }

                ItemSelectionMenu menu = new ItemSelectionMenu(plugin, reward);
                player.openInventory(menu.getInventory());
                
            } catch (NumberFormatException e) {
                player.sendMessage(msgManager.getMessage("amount-invalid"));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("collect")) {
            plugin.getQuestManager().collectQuestItems(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("rate")) {
            if (args.length < 4) return true;
            try {
                int questId = Integer.parseInt(args[1]);
                UUID workerUuid = UUID.fromString(args[2]);
                int score = Integer.parseInt(args[3]);
                if (score >= 1 && score <= 5) {
                    plugin.getQuestManager().addReview(player, questId, workerUuid, score);
                }
            } catch (Exception ignored) {}
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (player.hasPermission("guildquest.admin")) {
                plugin.getConfigManager().reloadConfig();
                plugin.getMessageManager().loadMessages();
                plugin.getMenuConfigManager().loadMenus(); // ОНОВЛЕНО: Тепер меню також перезавантажуються!
                player.sendMessage(msgManager.getMessage("reload-success"));
            } else {
                player.sendMessage(msgManager.getMessage("no-permission"));
            }
            return true;
        }

        player.sendMessage(msgManager.getMessage("unknown-command"));
        return true;
    }
}