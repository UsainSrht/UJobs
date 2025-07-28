package me.usainsrht.ujobs.models;

import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerJobData {
    private final UUID playerId;
    private final String playerName;
    private final Map<String, JobStats> jobStats;

    public PlayerJobData(UUID playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
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

    public void save(ConfigurationSection section) {
        section.set("name", playerName);

        ConfigurationSection jobsSection = section.createSection("jobs");
        for (Map.Entry<String, JobStats> entry : jobStats.entrySet()) {
            ConfigurationSection jobSection = jobsSection.createSection(entry.getKey());
            JobStats stats = entry.getValue();
            jobSection.set("level", stats.getLevel());
            jobSection.set("exp", stats.getExp());
            jobSection.set("totalMoney", stats.getTotalMoney());
        }
    }

    public static PlayerJobData load(UUID playerId, ConfigurationSection section) {
        String playerName = section.getString("name", "Unknown");
        PlayerJobData data = new PlayerJobData(playerId, playerName);

        ConfigurationSection jobsSection = section.getConfigurationSection("jobs");
        if (jobsSection != null) {
            for (String jobId : jobsSection.getKeys(false)) {
                ConfigurationSection jobSection = jobsSection.getConfigurationSection(jobId);
                if (jobSection != null) {
                    JobStats stats = new JobStats(
                            jobSection.getInt("level", 0),
                            jobSection.getLong("exp", 0),
                            jobSection.getDouble("totalMoney", 0.0)
                    );
                    data.setJobStats(jobId, stats);
                }
            }
        }

        return data;
    }

    // Getters
    public UUID getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public Map<String, JobStats> getAllJobStats() { return new HashMap<>(jobStats); }

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

        // Getters and setters
        public int getLevel() { return level; }
        public void setLevel(int level) { this.level = level; }

        public long getExp() { return exp; }
        public void setExp(long exp) { this.exp = exp; }

        public double getTotalMoney() { return totalMoney; }
        public void setTotalMoney(double totalMoney) { this.totalMoney = totalMoney; }
    }
}