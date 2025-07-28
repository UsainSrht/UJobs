package me.usainsrht.ujobs.models;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
public class PlayerJobData {
    private final UUID uuid;
    private final Map<String, JobStats> jobStats;

    public PlayerJobData(UUID playerId) {
        this.uuid = playerId;
        this.jobStats = new HashMap<>();
    }

    public JobStats getJobStats(String jobId) {
        return jobStats.computeIfAbsent(jobId, k -> new JobStats());
    }

    public void setJobStats(String jobId, JobStats stats) {
        jobStats.put(jobId, stats);
    }

    public boolean hasJobStats(String jobId) {
        return jobStats.containsKey(jobId);
    }

    public void addExp(String jobId, long exp) {
        JobStats stats = getJobStats(jobId);
        stats.setExp(stats.getExp() + exp);
    }

    public void addMoney(String jobId, double money) {
        JobStats stats = getJobStats(jobId);
        stats.setTotalMoney(stats.getTotalMoney() + money);
    }

    public void levelUp(String jobId) {
        JobStats stats = getJobStats(jobId);
        stats.setLevel(stats.getLevel() + 1);
    }

    @Getter
    @Setter
    public static class JobStats {
        private int level;
        private long exp;
        private double totalMoney;

        public JobStats() {
            this(0, 0, 0.0);
        }

        public JobStats(int level, long exp, double totalMoney) {
            this.level = level;
            this.exp = exp;
            this.totalMoney = totalMoney;
        }
    }
}