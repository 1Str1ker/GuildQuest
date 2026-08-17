package ua.striker.guildquest.managers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ua.striker.guildquest.GuildQuest;
import ua.striker.guildquest.models.Quest;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class QuestManager {

    private final GuildQuest plugin;
    private final Map<Integer, Quest> questCache = new ConcurrentHashMap<>();
    private final Set<Integer> ratedQuests = ConcurrentHashMap.newKeySet();

    public QuestManager(GuildQuest plugin) {
        this.plugin = plugin;
        loadQuestsAsync();
    }

    private void loadQuestsAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (PreparedStatement ps = plugin.getDatabaseManager().getConnection().prepareStatement("SELECT * FROM quests")) {
                ResultSet rs = ps.executeQuery();
                questCache.clear();
                
                while (rs.next()) {
                    int id = rs.getInt("id");
                    UUID creator = UUID.fromString(rs.getString("creator"));
                    String targetItem = rs.getString("target_item");
                    int amount = rs.getInt("amount");
                    double reward = rs.getDouble("reward");
                    String status = rs.getString("status");
                    String workerStr = rs.getString("worker");

                    Quest quest = new Quest(id, creator, targetItem, amount, reward, status);
                    if (workerStr != null && !workerStr.isEmpty() && !workerStr.equals("null")) {
                        quest.setWorkerUuid(UUID.fromString(workerStr));
                    }
                    questCache.put(id, quest);
                }
                plugin.getLogger().info("Успішно завантажено " + questCache.size() + " контрактів у кеш!");
            } catch (SQLException e) {
                plugin.getLogger().severe("Помилка завантаження квестів: " + e.getMessage());
            }
        });
    }

    public List<Quest> getOpenQuests() {
        List<Quest> open = new ArrayList<>();
        for (Quest q : questCache.values()) {
            if (q.getStatus().equals("OPEN")) open.add(q);
        }
        return open;
    }

    public List<Quest> getActiveQuestsFor(UUID workerUuid) {
        List<Quest> active = new ArrayList<>();
        for (Quest q : questCache.values()) {
            if (q.getStatus().equals("IN_PROGRESS") && workerUuid.equals(q.getWorkerUuid())) {
                active.add(q);
            }
        }
        return active;
    }

    public void createQuest(UUID creator, String targetItem, int amount, double reward) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String query = "INSERT INTO quests (creator, target_item, amount, reward, status) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = plugin.getDatabaseManager().getConnection().prepareStatement(query, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, creator.toString());
                ps.setString(2, targetItem);
                ps.setInt(3, amount);
                ps.setDouble(4, reward);
                ps.setString(5, "OPEN");
                ps.executeUpdate();
                
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    int generatedId = rs.getInt(1);
                    Quest newQuest = new Quest(generatedId, creator, targetItem, amount, reward, "OPEN");
                    questCache.put(generatedId, newQuest);
                    
                    // Оновлюємо статистику замовника
                    ua.striker.guildquest.models.GuildPlayer guildPlayer = plugin.getPlayerManager().getGuildPlayer(creator);
                    if (guildPlayer != null) {
                        guildPlayer.addCreatedQuest(); 
                        
                        plugin.getDatabaseManager().executeAsync(
                            "UPDATE players SET created_quests = created_quests + 1 WHERE uuid = ?", 
                            creator.toString()
                        );
                    }

                    if (plugin.getHologramManager() != null) {
                        plugin.getHologramManager().updateTopHologram(null);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Помилка при створенні квесту: " + e.getMessage());
            }
        });
    }

    public void acceptQuest(Player worker, int questId) {
        Quest quest = questCache.get(questId);
        if (quest != null && quest.getStatus().equals("OPEN")) {
            if (quest.getCreatorUuid().equals(worker.getUniqueId())) {
                worker.sendMessage("§c[Гільдія] Ви не можете прийняти власне замовлення!");
                return;
            }
            quest.setStatus("IN_PROGRESS");
            quest.setWorkerUuid(worker.getUniqueId());
            
            plugin.getDatabaseManager().executeAsync("UPDATE quests SET status = ?, worker = ? WHERE id = ?", 
                    "IN_PROGRESS", worker.getUniqueId().toString(), questId);
            
            worker.sendMessage("§a[Гільдія] Ви успішно прийняли замовлення #" + questId + "!");
        }
    }

    public void submitQuestItems(Player worker, Quest quest) {
        Material mat = Material.matchMaterial(quest.getTargetItem());
        if (mat == null) return;
        
        int count = 0;
        for (ItemStack item : worker.getInventory().getContents()) {
            if (item != null && item.getType() == mat) {
                count += item.getAmount();
            }
        }
        
        if (count >= quest.getAmount()) {
            // 1. Забираємо предмети
            int toRemove = quest.getAmount();
            for (ItemStack item : worker.getInventory().getContents()) {
                if (item != null && item.getType() == mat) {
                    if (item.getAmount() <= toRemove) {
                        toRemove -= item.getAmount();
                        item.setAmount(0);
                    } else {
                        item.setAmount(item.getAmount() - toRemove);
                        toRemove = 0;
                    }
                    if (toRemove <= 0) break;
                }
            }
            
            // 2. Видаємо гроші та оновлюємо статус
            GuildQuest.getEconomy().depositPlayer(worker, quest.getReward());
            quest.setStatus("COMPLETED");
            plugin.getDatabaseManager().executeAsync("UPDATE quests SET status = ? WHERE id = ?", "COMPLETED", quest.getQuestId());
            
            // 3. Нарахування очок на основі конфігу
            ua.striker.guildquest.models.GuildPlayer guildPlayer = plugin.getPlayerManager().getGuildPlayer(worker.getUniqueId());
            int pointsEarned = 0;
            if (guildPlayer != null) {
                double defaultPoints = plugin.getConfig().getDouble("points.default-points", 1.0);
                double pointsPerItem = plugin.getConfig().getDouble("points.items." + mat.name(), defaultPoints);
                
                pointsEarned = (int) Math.round(pointsPerItem * quest.getAmount());
                pointsEarned = Math.max(0, pointsEarned); // Мінімум 0 очок

                if (pointsEarned > 0) {
                    guildPlayer.addPoints(pointsEarned);
                }
                
                guildPlayer.addCompletedQuest();
                guildPlayer.setUniqueClients(guildPlayer.getUniqueClients() + 1);
            }
            
            // 4. Оновлюємо голограму
            if (plugin.getHologramManager() != null) {
                plugin.getHologramManager().updateTopHologram(null);
            }

            worker.closeInventory();
            
            // 5. Повідомлення про успішне виконання
            worker.sendMessage("§a========================================");
            worker.sendMessage("§a ✔ Ви успішно виконали замовлення #" + quest.getQuestId() + "!");
            worker.sendMessage("§e 💰 Нагорода: §f" + quest.getReward() + " монет");
            if (pointsEarned > 0) {
                worker.sendMessage("§b 🌟 Отримано очок гільдії: §f" + pointsEarned);
            } else {
                worker.sendMessage("§8 🌟 Очок за цей предмет не передбачено.");
            }
            worker.sendMessage("§a========================================");

            // 6. Клікабельне повідомлення з оцінкою
            net.md_5.bungee.api.chat.TextComponent voteMessage = new net.md_5.bungee.api.chat.TextComponent("§e[Гільдія] Оцініть замовника: ");
            for (int i = 1; i <= 5; i++) {
                net.md_5.bungee.api.chat.TextComponent star = new net.md_5.bungee.api.chat.TextComponent("§6[ " + i + "⭐ ] ");
                star.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                        net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, 
                        "/gq review " + quest.getQuestId() + " " + quest.getCreatorUuid().toString() + " " + i
                ));
                star.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                        net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, 
                        new net.md_5.bungee.api.chat.hover.content.Text("§aПоставити " + i + " зірок!")
                ));
                voteMessage.addExtra(star);
            }
            worker.spigot().sendMessage(voteMessage);
            
            // 7. Сповіщення замовника
            Player creator = Bukkit.getPlayer(quest.getCreatorUuid());
            
            if (creator != null && creator.isOnline()) {
                creator.sendMessage("§6========================================");
                creator.sendMessage("§e🔔 [Гільдія] Ваше замовлення #" + quest.getQuestId() + " виконано!");
                creator.sendMessage("§fГравець §a" + worker.getName() + " §fзібрав необхідні ресурси.");
                creator.sendMessage("§fПропишіть §e/gq collect§f, щоб забрати їх у свій інвентар!");
                creator.sendMessage("§6========================================");
                
                creator.playSound(creator.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            }
            
        } else {
            worker.sendMessage("§c[Гільдія] У вас недостатньо предметів (" + count + "/" + quest.getAmount() + ").");
        }
    }

    public void collectQuestItems(Player creator) {
        boolean collected = false;
        List<Integer> toRemove = new ArrayList<>();

        for (Quest q : questCache.values()) {
            if (q.getStatus().equals("COMPLETED") && q.getCreatorUuid().equals(creator.getUniqueId())) {
                Material mat = Material.matchMaterial(q.getTargetItem());
                if (mat != null) {
                    ItemStack rewardItem = new ItemStack(mat, q.getAmount());
                    Map<Integer, ItemStack> leftover = creator.getInventory().addItem(rewardItem);
                    
                    if (!leftover.isEmpty()) {
                        for (ItemStack drop : leftover.values()) {
                            creator.getWorld().dropItemNaturally(creator.getLocation(), drop);
                        }
                        creator.sendMessage("§e[Гільдія] Ваш інвентар був повний, залишки ресурсів впали біля вас!");
                    }
                }
                
                plugin.getDatabaseManager().executeAsync("DELETE FROM quests WHERE id = ?", q.getQuestId());
                toRemove.add(q.getQuestId());
                collected = true;
                
                creator.sendMessage("§a[Гільдія] Ви отримали ресурси з замовлення #" + q.getQuestId() + "!");
            }
        }

        for (int id : toRemove) questCache.remove(id);

        if (!collected) {
            creator.sendMessage("§c[Гільдія] У вас немає виконаних замовлень для збору.");
        }
    }

    public void deleteQuestAdmin(Player admin, int questId) {
        Quest quest = questCache.remove(questId);
        if (quest != null) {
            plugin.getDatabaseManager().executeAsync("DELETE FROM quests WHERE id = ?", questId);
            
            if (quest.getStatus().equals("OPEN")) {
                OfflinePlayer creator = Bukkit.getOfflinePlayer(quest.getCreatorUuid());
                GuildQuest.getEconomy().depositPlayer(creator, quest.getReward());
            }
            admin.sendMessage("§a[Гільдія] Замовлення #" + questId + " успішно видалено з бази.");
        } else {
            admin.sendMessage("§c[Гільдія] Квест з таким ID не знайдено.");
        }
    }

    public void addReview(Player reviewer, int questId, UUID targetUuid, int score) {
        Quest quest = questCache.get(questId);
        
        if (quest == null) {
            reviewer.sendMessage("§c[Гільдія] Цього замовлення більше не існує!");
            return;
        }

        if (quest.getWorkerUuid() == null || !quest.getWorkerUuid().equals(reviewer.getUniqueId())) {
            reviewer.sendMessage("§c[Гільдія] Ви не можете оцінити це замовлення, бо не ви його виконували!");
            return;
        }

        if (ratedQuests.contains(questId)) {
            reviewer.sendMessage("§c[Гільдія] Ви вже оцінили це замовлення!");
            return;
        }

        ua.striker.guildquest.models.GuildPlayer target = plugin.getPlayerManager().getGuildPlayer(targetUuid);
        
        if (target != null) {
            double currentRating = target.getRating();
            double newRating = (currentRating + score) / 2.0;
            newRating = Math.round(newRating * 10.0) / 10.0;
            
            target.setRating(newRating);
            plugin.getDatabaseManager().executeAsync("UPDATE players SET rating = ? WHERE uuid = ?", newRating, targetUuid.toString());
            
            ratedQuests.add(questId);
            
            reviewer.sendMessage("§a[Гільдія] Дякуємо! Ви оцінили замовника на " + score + " ⭐.");
            
            Player targetPlayer = Bukkit.getPlayer(targetUuid);
            if (targetPlayer != null && targetPlayer.isOnline()) {
                targetPlayer.sendMessage("§e🌟 [Гільдія] Виконавець оцінив вас! Ваш новий рейтинг: §6" + newRating);
            }
        }
    }
}