package ua.striker.guildquest.managers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class QuestManager {

    private final GuildQuest plugin;
    
    // Кеш для квестів: швидкий доступ без лагів + безпека для потоків
    private final Map<Integer, Quest> questCache = new ConcurrentHashMap<>();

    public QuestManager(GuildQuest plugin) {
        this.plugin = plugin;
        loadQuestsAsync();
    }

    // Завантажуємо всі квести з бази у фоновому режимі при старті
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
        // Записуємо в БД асинхронно, щоб отримати унікальний ID, і одразу додаємо в кеш
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
            
            // Асинхронне оновлення бази
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
            
            GuildQuest.getEconomy().depositPlayer(worker, quest.getReward());
            quest.setStatus("COMPLETED");
            plugin.getDatabaseManager().executeAsync("UPDATE quests SET status = ? WHERE id = ?", "COMPLETED", quest.getQuestId());
            
            worker.sendMessage("§a[Гільдія] Ви виконали замовлення та отримали " + quest.getReward() + " монет!");
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
                    creator.getInventory().addItem(new ItemStack(mat, q.getAmount()));
                }
                
                plugin.getDatabaseManager().executeAsync("DELETE FROM quests WHERE id = ?", q.getQuestId());
                toRemove.add(q.getQuestId());
                collected = true;
                
                creator.sendMessage("§a[Гільдія] Ви отримали ресурси з замовлення #" + q.getQuestId() + "!");
            }
        }

        // Очищаємо кеш після циклу
        for (int id : toRemove) questCache.remove(id);

        if (!collected) {
            creator.sendMessage("§c[Гільдія] У вас немає виконаних замовлень для збору.");
        }
    }

    public void deleteQuestAdmin(Player admin, int questId) {
        Quest quest = questCache.remove(questId); // Одразу видаляємо з кешу
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

    public void addReview(Player creator, int questId, UUID workerUuid, int score) {
        creator.sendMessage("§a[Гільдія] Дякуємо за вашу оцінку!");
    }
}