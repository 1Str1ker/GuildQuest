package ua.striker.guildquest.models;

import java.util.UUID;

public class Quest {
    private final int questId;
    private final UUID clientUuid;
    private UUID workerUuid; // НОВЕ ПОЛЕ: Виконавець квесту
    private final String targetItem;
    private final int amount;
    private final double reward;
    private QuestStatus status;

    public enum QuestStatus {
        OPEN,         // Доступний на дошці
        IN_PROGRESS,  // Хтось взяв на виконання
        COMPLETED,    // Ресурси здані, чекає поки замовник їх забере
        ARCHIVED      // Завершений і оцінений
    }

    public Quest(int questId, UUID clientUuid, UUID workerUuid, String targetItem, int amount, double reward, QuestStatus status) {
        this.questId = questId;
        this.clientUuid = clientUuid;
        this.workerUuid = workerUuid;
        this.targetItem = targetItem;
        this.amount = amount;
        this.reward = reward;
        this.status = status;
    }

    public int getQuestId() { return questId; }
    public UUID getClientUuid() { return clientUuid; }
    public UUID getWorkerUuid() { return workerUuid; }
    public String getTargetItem() { return targetItem; }
    public int getAmount() { return amount; }
    public double getReward() { return reward; }
    public QuestStatus getStatus() { return status; }

    public void setWorkerUuid(UUID workerUuid) { this.workerUuid = workerUuid; }
    public void setStatus(QuestStatus status) { this.status = status; }
}