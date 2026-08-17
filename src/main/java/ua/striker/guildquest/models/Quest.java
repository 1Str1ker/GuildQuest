package ua.striker.guildquest.models;

import java.util.UUID;

public class Quest {
    private final int id;
    private final UUID creatorUuid;
    private final String targetItem;
    private final int amount;
    private final double reward;
    private String status;
    private UUID workerUuid;

    public Quest(int id, UUID creatorUuid, String targetItem, int amount, double reward, String status) {
        this.id = id;
        this.creatorUuid = creatorUuid;
        this.targetItem = targetItem;
        this.amount = amount;
        this.reward = reward;
        this.status = status;
    }

    public int getQuestId() { return id; }
    public UUID getCreatorUuid() { return creatorUuid; }
    public String getTargetItem() { return targetItem; }
    public int getAmount() { return amount; }
    public double getReward() { return reward; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public UUID getWorkerUuid() { return workerUuid; }
    public void setWorkerUuid(UUID workerUuid) { this.workerUuid = workerUuid; }
}