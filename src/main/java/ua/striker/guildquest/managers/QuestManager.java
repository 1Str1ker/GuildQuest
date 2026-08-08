package ua.striker.guildquest.managers;

import org.bukkit.Bukkit;
import ua.striker.guildquest.GuildQuest;
import ua.striker.guildquest.models.Quest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class QuestManager {

    private final GuildQuest plugin;

    public QuestManager(GuildQuest plugin) {
        this.plugin = plugin;
    }

    // Створення нового квесту та збереження в БД
    public void createQuest(UUID clientUuid, String targetItem, int amount, double reward) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = plugin.getDatabaseManager().getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO quests (client_uuid, target_item, amount, reward, status) VALUES (?, ?, ?, ?, ?)")) {
                
                statement.setString(1, clientUuid.toString());
                statement.setString(2, targetItem);
                statement.setInt(3, amount);
                statement.setDouble(4, reward);
                statement.setString(5, Quest.QuestStatus.OPEN.name());
                
                statement.executeUpdate();
                plugin.getLogger().info("Створено новий квест від гравця " + clientUuid);
            } catch (SQLException e) {
                plugin.getLogger().severe("Помилка при створенні квесту: " + e.getMessage());
            }
        });
    }

    // Отримання всіх відкритих квестів для дошки оголошень
    public List<Quest> getOpenQuests() {
        List<Quest> quests = new ArrayList<>();
        try (Connection connection = plugin.getDatabaseManager().getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM quests WHERE status = 'OPEN'")) {
            
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                int questId = resultSet.getInt("quest_id");
                UUID clientUuid = UUID.fromString(resultSet.getString("client_uuid"));
                String targetItem = resultSet.getString("target_item");
                int amount = resultSet.getInt("amount");
                double reward = resultSet.getDouble("reward");
                Quest.QuestStatus status = Quest.QuestStatus.valueOf(resultSet.getString("status"));

                quests.add(new Quest(questId, clientUuid, targetItem, amount, reward, status));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Помилка при завантаженні квестів: " + e.getMessage());
        }
        return quests;
    }
}