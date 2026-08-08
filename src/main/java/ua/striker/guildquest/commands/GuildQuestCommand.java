package ua.striker.guildquest.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ua.striker.guildquest.GuildQuest;
import ua.striker.guildquest.menus.QuestBoardMenu;

public class GuildQuestCommand implements CommandExecutor {

    private final GuildQuest plugin;

    public GuildQuestCommand(GuildQuest plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        // Перевіряємо, чи команду ввів гравець (а не консоль сервера)
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Цю команду може використовувати лише гравець!");
            return true;
        }

        // Якщо гравець ввів просто /gq або /guildquest
        if (args.length == 0) {
            // Передаємо плагін у конструктор меню
            QuestBoardMenu menu = new QuestBoardMenu(plugin);
            player.openInventory(menu.getInventory());
            return true;
        }

        // Заготовка для адмін-команд (наприклад /gq reload)
        if (args[0].equalsIgnoreCase("reload")) {
            if (player.hasPermission("guildquest.admin")) {
                player.sendMessage("§a[Гільдія] Конфігурацію плагіна перезавантажено!");
                // TODO: Логіка перезавантаження конфігів
            } else {
                player.sendMessage("§c[Гільдія] У вас немає прав для цієї команди.");
            }
            return true;
        }

        return true;
    }
}