package me.usainsrht.ujobs.managers;

import me.usainsrht.ujobs.UJobsPlugin;
import me.usainsrht.ujobs.models.PlayerJobData;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class JobDataManager {
    private final UJobsPlugin plugin;
    private final File dataFolder;
    private final Map<UUID, PlayerJobData> playerDataCache;
    private final Map<UUID, Long> lastSaveTime;

    public JobDataManager(UJobsPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        this.playerDataCache = new ConcurrentHashMap<>();
        this.lastSaveTime = new ConcurrentHashMap<>();

        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    public PlayerJobData getPlayerData(UUID playerId) {
        return playerDataCache.computeIfAbsent(playerId, this::loadPlayerData);
    }

    public PlayerJobData getPlayerData(OfflinePlayer player) {
        return getPlayerData(player.getUniqueId());
    }

    private PlayerJobData loadPlayerData(UUID playerId) {
        File playerFile = new File(dataFolder, playerId.toString() + ".yml");

        if (!playerFile.exists()) {
            // Create new player data
            OfflinePlayer player = plugin.getServer().getOfflinePlayer(playerId);
            return new PlayerJobData(playerId, player.getName() != null ? player.getName() : "Unknown");
        }

        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
            return PlayerJobData.load(playerId, config);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load player data for " + playerId + ": " + e.getMessage());
            OfflinePlayer player = plugin.getServer().getOfflinePlayer(playerId);
            return new PlayerJobData(playerId, player.getName() != null ? player.getName() : "Unknown");
        }
    }

    public void savePlayerData(PlayerJobData playerData) {
        savePlayerData(playerData, false);
    }

    public void savePlayerData(PlayerJobData playerData, boolean force) {
        UUID playerId = playerData.getPlayerId();
        long currentTime = System.currentTimeMillis();

        // Rate limiting: only save once per 5 seconds unless forced
        if (!force) {
            Long lastSave = lastSaveTime.get(playerId);
            if (lastSave != null && (currentTime - lastSave) < 5000) {
                return;
            }
        }

        File playerFile = new File(dataFolder, playerId.toString() + ".yml");

        try {
            FileConfiguration config = new YamlConfiguration();
            playerData.save(config);
            config.save(playerFile);
            lastSaveTime.put(playerId, currentTime);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save player data for " + playerId + ": " + e.getMessage());
        }
    }

    public void saveAllData() {
        plugin.getLogger().info("Saving all player data...");

        for (PlayerJobData playerData : playerDataCache.values()) {
            savePlayerData(playerData, true);
        }

        plugin.getLogger().info("Saved data for " + playerDataCache.size() + " players.");
    }

    public void unloadPlayerData(UUID playerId) {
        PlayerJobData playerData = playerDataCache.remove(playerId);
        if (playerData != null) {
            savePlayerData(playerData, true);
        }
        lastSaveTime.remove(playerId);
    }

    public boolean isPlayerDataLoaded(UUID playerId) {
        return playerDataCache.containsKey(playerId);
    }

    public Map<String, Integer> getJobLevels(UUID playerId) {
        PlayerJobData playerData = getPlayerData(playerId);
        Map<String, Integer> levels = new HashMap<>();

        for (Map.Entry<String, PlayerJobData.JobStats> entry : playerData.getAllJobStats().entrySet()) {
            levels.put(entry.getKey(), entry.getValue().getLevel());
        }

        return levels;
    }

    public int getJobLevel(UUID playerId, String jobId) {
        PlayerJobData playerData = getPlayerData(playerId);
        return playerData.getJobStats(jobId).getLevel();
    }

    public long getJobExp(UUID playerId, String jobId) {
        PlayerJobData playerData = getPlayerData(playerId);
        return playerData.getJobStats(jobId).getExp();
    }

    public double getJobTotalMoney(UUID playerId, String jobId) {
        PlayerJobData playerData = getPlayerData(playerId);
        return playerData.getJobStats(jobId).getTotalMoney();
    }

    public void setJobLevel(UUID playerId, String jobId, int level) {
        PlayerJobData playerData = getPlayerData(playerId);
        playerData.getJobStats(jobId).setLevel(level);
        savePlayerData(playerData);
    }

    public void setJobExp(UUID playerId, String jobId, long exp) {
        PlayerJobData playerData = getPlayerData(playerId);
        playerData.getJobStats(jobId).setExp(exp);
        savePlayerData(playerData);
    }

    public void addJobExp(UUID playerId, String jobId, long exp) {
        PlayerJobData playerData = getPlayerData(playerId);
        playerData.addExp(jobId, exp);
        savePlayerData(playerData);
    }

    public void addJobMoney(UUID playerId, String jobId, double money) {
        PlayerJobData playerData = getPlayerData(playerId);
        playerData.addMoney(jobId, money);
        savePlayerData(playerData);
    }

    // Bulk operations for leaderboard calculations
    public Map<UUID, PlayerJobData> getAllPlayerData() {
        // Load all player data files
        Map<UUID, PlayerJobData> allData = new HashMap<>(playerDataCache);

        if (dataFolder.exists()) {
            File[] playerFiles = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
            if (playerFiles != null) {
                for (File playerFile : playerFiles) {
                    try {
                        String fileName = playerFile.getName();
                        UUID playerId = UUID.fromString(fileName.substring(0, fileName.length() - 4));

                        if (!allData.containsKey(playerId)) {
                            allData.put(playerId, loadPlayerData(playerId));
                        }
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid player file name: " + playerFile.getName());
                    }
                }
            }
        }

        return allData;
    }

    public void clearCache() {
        saveAllData();
        playerDataCache.clear();
        lastSaveTime.clear();
    }
}