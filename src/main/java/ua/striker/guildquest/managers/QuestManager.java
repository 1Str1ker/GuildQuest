package ua.striker.guildquest.managers;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ua.striker.guildquest.GuildQuest;
import ua.striker.guildquest.models.GuildPlayer;
import ua.striker.guildquest.models.Quest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class QuestManager {

    private final GuildQuest plugin;

    public QuestManager(GuildQuest plugin) {
        this.plugin = plugin;
    }

    public void createQuest(UUID clientUuid, String targetItem, int amount, double reward) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Connection connection = plugin.getDatabaseManager().getConnection();
            try {
                try (PreparedStatement statement = connection.prepareStatement(
                             "INSERT INTO quests (client_uuid, target_item, amount, reward, status) VALUES (?, ?, ?, ?, ?)")) {
                    statement.setString(1, clientUuid.toString());
                    statement.setString(2, targetItem);
                    statement.setInt(3, amount);
                    statement.setDouble(4, reward);
                    statement.setString(5, Quest.QuestStatus.OPEN.name());
                    statement.executeUpdate();
                }

                try (PreparedStatement updateStmt = connection.prepareStatement("UPDATE players SET created_quests = created_quests + 1 WHERE uuid = ?")) {
                    updateStmt.setString(1, clientUuid.toString());
                    updateStmt.executeUpdate();
                }

                Bukkit.getScheduler().runTask(plugin, () -> {
                    GuildPlayer gp = plugin.getPlayerManager().getGuildPlayer(clientUuid);
                    if (gp != null) {
                        gp.addCreatedQuest();
                    }
                });
            } catch (SQLException e) {
                plugin.getLogger().severe("Помилка при створенні квесту: " + e.getMessage());
            }
        });
    }

    public void acceptQuest(Player worker, int questId) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Connection connection = plugin.getDatabaseManager().getConnection();
            try (PreparedStatement statement = connection.prepareStatement(
                         "UPDATE quests SET status = ?, worker_uuid = ? WHERE quest_id = ? AND status = 'OPEN'")) {
                
                statement.setString(1, Quest.QuestStatus.IN_PROGRESS.name());
                statement.setString(2, worker.getUniqueId().toString());
                statement.setInt(3, questId);
                
                int rowsUpdated = statement.executeUpdate();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (rowsUpdated > 0) worker.sendMessage("§a[Гільдія] Ви успішно прийняли контракт #" + questId + "!");
                    else worker.sendMessage("§c[Гільдія] Цей контракт вже недоступний.");
                });
            } catch (SQLException e) {
                plugin.getLogger().severe("Помилка при прийнятті квесту: " + e.getMessage());
            }
        });
    }

    public List<Quest> getActiveQuestsFor(UUID workerUuid) {
        List<Quest> quests = new ArrayList<>();
        Connection connection = plugin.getDatabaseManager().getConnection();
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM quests WHERE status = 'IN_PROGRESS' AND worker_uuid = ?")) {
            
            statement.setString(1, workerUuid.toString());
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                int questId = resultSet.getInt("quest_id");
                UUID clientUuid = UUID.fromString(resultSet.getString("client_uuid"));
                String targetItem = resultSet.getString("target_item");
                int amount = resultSet.getInt("amount");
                double reward = resultSet.getDouble("reward");
                Quest.QuestStatus status = Quest.QuestStatus.valueOf(resultSet.getString("status"));
                quests.add(new Quest(questId, clientUuid, workerUuid, targetItem, amount, reward, status));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Помилка при завантаженні активних квестів: " + e.getMessage());
        }
        return quests;
    }

    public void submitQuestItems(Player worker, Quest quest) {
        Material requiredMaterial = Material.matchMaterial(quest.getTargetItem());
        if (requiredMaterial == null) return;
        if (!worker.getInventory().contains(requiredMaterial, quest.getAmount())) {
            worker.sendMessage(plugin.getMessageManager().getMessage("not-enough-items"));
            return;
        }

        worker.getInventory().removeItem(new ItemStack(requiredMaterial, quest.getAmount()));
        GuildQuest.getEconomy().depositPlayer(worker, quest.getReward());

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Connection connection = plugin.getDatabaseManager().getConnection();
            try {
                try (PreparedStatement stmt = connection.prepareStatement("UPDATE quests SET status = 'COMPLETED' WHERE quest_id = ?")) {
                    stmt.setInt(1, quest.getQuestId());
                    stmt.executeUpdate();
                }

                long currentTime = System.currentTimeMillis();
                long yesterday = currentTime - (24L * 60 * 60 * 1000);

                boolean isUniqueClient = true;
                try (PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(*) FROM quest_history WHERE worker_uuid = ? AND client_uuid = ?")) {
                    stmt.setString(1, worker.getUniqueId().toString());
                    stmt.setString(2, quest.getClientUuid().toString());
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next() && rs.getInt(1) > 0) isUniqueClient = false;
                }

                int questsLast24h = 0;
                try (PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(*) FROM quest_history WHERE worker_uuid = ? AND client_uuid = ? AND timestamp > ?")) {
                    stmt.setString(1, worker.getUniqueId().toString());
                    stmt.setString(2, quest.getClientUuid().toString());
                    stmt.setLong(3, yesterday);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) questsLast24h = rs.getInt(1);
                }

                try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO quest_history (worker_uuid, client_uuid, timestamp) VALUES (?, ?, ?)")) {
                    stmt.setString(1, worker.getUniqueId().toString());
                    stmt.setString(2, quest.getClientUuid().toString());
                    stmt.setLong(3, currentTime);
                    stmt.executeUpdate();
                }

                boolean finalIsUniqueClient = isUniqueClient;
                int finalQuestsLast24h = questsLast24h;

                Bukkit.getScheduler().runTask(plugin, () -> {
                    String compMsg = plugin.getMessageManager().getMessage("quest-completed").replace("%id%", String.valueOf(quest.getQuestId()));
                    worker.sendMessage(compMsg);
                    
                    String rewMsg = plugin.getMessageManager().getMessage("reward-received").replace("%reward%", String.valueOf(quest.getReward()));
                    worker.sendMessage(rewMsg);

                    GuildPlayer gp = plugin.getPlayerManager().getGuildPlayer(worker.getUniqueId());
                    if (gp != null) {
                        gp.addCompletedQuest();
                        if (finalIsUniqueClient) gp.setUniqueClients(gp.getUniqueClients() + 1);
                        if (finalQuestsLast24h < 3) {
                            int points = plugin.getConfigManager().getItemPoints(quest.getTargetItem()) * quest.getAmount();
                            if (points > 0) {
                                gp.addPoints(points);
                                String ptsMsg = plugin.getMessageManager().getMessage("points-received").replace("%points%", String.valueOf(points));
                                worker.sendMessage(ptsMsg);
                            }
                        }
                    }
                    Player client = Bukkit.getPlayer(quest.getClientUuid());
                    if (client != null && client.isOnline()) {
                        client.sendMessage("§a[Гільдія] Ваш контракт #" + quest.getQuestId() + " був виконаний!");
                        client.sendMessage("§e[Гільдія] Введіть §b/gq collect§e, щоб забрати ресурси.");
                    }
                });
            } catch (SQLException e) {
                plugin.getLogger().severe("Помилка: " + e.getMessage());
            }
        });
    }

    public void collectQuestItems(Player client) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Connection connection = plugin.getDatabaseManager().getConnection();
            try {
                List<Quest> readyToCollect = new ArrayList<>();
                try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM quests WHERE status = 'COMPLETED' AND client_uuid = ?")) {
                    statement.setString(1, client.getUniqueId().toString());
                    ResultSet resultSet = statement.executeQuery();
                    while (resultSet.next()) {
                        int questId = resultSet.getInt("quest_id");
                        String targetItem = resultSet.getString("target_item");
                        int amount = resultSet.getInt("amount");
                        String workerStr = resultSet.getString("worker_uuid");
                        UUID workerUuid = (workerStr != null) ? UUID.fromString(workerStr) : null;
                        
                        readyToCollect.add(new Quest(questId, client.getUniqueId(), workerUuid, targetItem, amount, 0, Quest.QuestStatus.COMPLETED));
                    }
                }

                if (readyToCollect.isEmpty()) {
                    Bukkit.getScheduler().runTask(plugin, () -> client.sendMessage("§c[Гільдія] Немає виконаних замовлень."));
                    return;
                }

                try (PreparedStatement updateStmt = connection.prepareStatement("UPDATE quests SET status = 'ARCHIVED' WHERE status = 'COMPLETED' AND client_uuid = ?")) {
                    updateStmt.setString(1, client.getUniqueId().toString());
                    updateStmt.executeUpdate();
                }

                Bukkit.getScheduler().runTask(plugin, () -> {
                    for (Quest q : readyToCollect) {
                        Material mat = Material.matchMaterial(q.getTargetItem());
                        if (mat != null) {
                            HashMap<Integer, ItemStack> leftOvers = client.getInventory().addItem(new ItemStack(mat, q.getAmount()));
                            for (ItemStack item : leftOvers.values()) client.getWorld().dropItemNaturally(client.getLocation(), item);
                            
                            client.sendMessage("§a[Гільдія] Ви отримали " + q.getAmount() + "x " + q.getTargetItem() + " за контракт #" + q.getQuestId());
                            
                            if (q.getWorkerUuid() != null) {
                                TextComponent message = new TextComponent("§e[Гільдія] Оцініть виконавця: ");
                                for (int i = 1; i <= 5; i++) {
                                    TextComponent star = new TextComponent("§b[" + i + "⭐] ");
                                    star.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/gq rate " + q.getQuestId() + " " + q.getWorkerUuid().toString() + " " + i));
                                    star.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§7Поставити " + i + " зірок")));
                                    message.addExtra(star);
                                }
                                client.spigot().sendMessage(message);
                            }
                        }
                    }
                });
            } catch (SQLException e) {
                plugin.getLogger().severe("Помилка: " + e.getMessage());
            }
        });
    }

    public void addReview(Player client, int questId, UUID workerUuid, int score) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Connection connection = plugin.getDatabaseManager().getConnection();
            try {
                try (PreparedStatement checkStmt = connection.prepareStatement("SELECT COUNT(*) FROM reviews WHERE quest_id = ?")) {
                    checkStmt.setInt(1, questId);
                    ResultSet rs = checkStmt.executeQuery();
                    if (rs.next() && rs.getInt(1) > 0) {
                        Bukkit.getScheduler().runTask(plugin, () -> client.sendMessage("§c[Гільдія] Ви вже залишили відгук для цього контракту!"));
                        return;
                    }
                }

                try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO reviews (quest_id, worker_uuid, score) VALUES (?, ?, ?)")) {
                    stmt.setInt(1, questId);
                    stmt.setString(2, workerUuid.toString());
                    stmt.setInt(3, score);
                    stmt.executeUpdate();
                }

                double newRating = 0.0;
                try (PreparedStatement stmt = connection.prepareStatement("SELECT AVG(score) FROM reviews WHERE worker_uuid = ?")) {
                    stmt.setString(1, workerUuid.toString());
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) newRating = rs.getDouble(1);
                }

                try (PreparedStatement stmt = connection.prepareStatement("UPDATE players SET rating = ? WHERE uuid = ?")) {
                    stmt.setDouble(1, newRating);
                    stmt.setString(2, workerUuid.toString());
                    stmt.executeUpdate();
                }

                double finalNewRating = newRating;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    GuildPlayer gp = plugin.getPlayerManager().getGuildPlayer(workerUuid);
                    if (gp != null) gp.setRating(finalNewRating);
                    client.sendMessage("§a[Гільдія] Дякуємо! Ви оцінили роботу на " + score + " зірок.");
                });

            } catch (SQLException e) {
                plugin.getLogger().severe("Помилка при збереженні відгуку: " + e.getMessage());
            }
        });
    }

    public void deleteQuestAdmin(Player admin, int questId) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Connection connection = plugin.getDatabaseManager().getConnection();
            try (PreparedStatement statement = connection.prepareStatement("SELECT client_uuid, reward, status FROM quests WHERE quest_id = ?")) {
                statement.setInt(1, questId);
                ResultSet rs = statement.executeQuery();
                if (rs.next()) {
                    String clientUuidStr = rs.getString("client_uuid");
                    double reward = rs.getDouble("reward");
                    String status = rs.getString("status");

                    if (status.equals("COMPLETED") || status.equals("ARCHIVED")) {
                        Bukkit.getScheduler().runTask(plugin, () -> admin.sendMessage("§c[Гільдія] Не можна видалити виконаний або заархівований квест."));
                        return;
                    }

                    try (PreparedStatement delStmt = connection.prepareStatement("DELETE FROM quests WHERE quest_id = ?")) {
                        delStmt.setInt(1, questId);
                        delStmt.executeUpdate();
                    }

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        org.bukkit.OfflinePlayer client = Bukkit.getOfflinePlayer(UUID.fromString(clientUuidStr));
                        GuildQuest.getEconomy().depositPlayer(client, reward);
                        admin.sendMessage("§a[Гільдія] Контракт #" + questId + " видалено. §e" + reward + " монет §aповернено замовнику.");
                        
                        if (client.isOnline()) {
                            ((Player) client).sendMessage("§c[Гільдія] Ваш контракт #" + questId + " був видалений адміністрацією. Нагороду повернено.");
                        }
                    });
                } else {
                    Bukkit.getScheduler().runTask(plugin, () -> admin.sendMessage("§c[Гільдія] Квест #" + questId + " не знайдено."));
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Помилка видалення квесту: " + e.getMessage());
            }
        });
    }

    public List<Quest> getOpenQuests() {
        List<Quest> quests = new ArrayList<>();
        Connection connection = plugin.getDatabaseManager().getConnection();
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM quests WHERE status = 'OPEN'")) {
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                quests.add(new Quest(resultSet.getInt("quest_id"), UUID.fromString(resultSet.getString("client_uuid")), 
                        resultSet.getString("worker_uuid") != null ? UUID.fromString(resultSet.getString("worker_uuid")) : null, 
                        resultSet.getString("target_item"), resultSet.getInt("amount"), resultSet.getDouble("reward"), 
                        Quest.QuestStatus.valueOf(resultSet.getString("status"))));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Помилка: " + e.getMessage());
        }
        return quests;
    }
}