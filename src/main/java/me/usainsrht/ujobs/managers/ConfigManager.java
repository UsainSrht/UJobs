package me.usainsrht.ujobs.managers;

import lombok.Getter;
import me.usainsrht.ujobs.UJobsPlugin;
import me.usainsrht.ujobs.yaml.YamlMessage;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Getter
public class ConfigManager {

    private UJobsPlugin plugin;
    private YamlConfiguration jobsConfig;
    private YamlConfiguration leaderboardConfig;
    private Map<String, YamlMessage> messages;
    public static final YamlMessage EMPTY_YAML_MESSAGE = new YamlMessage(null);

    public ConfigManager(UJobsPlugin plugin) {
        this.plugin = plugin;

        loadConfigs();
    }

    public void reload() {
        plugin.reloadConfig();
        //reload jobs yml too

        loadConfigs();
    }

    public void loadConfigs() {
        // Load config.yml
        plugin.saveDefaultConfig();

        loadMessages();

        // Load jobs.yml
        File jobsFile = new File(plugin.getDataFolder(), "jobs.yml");
        if (!jobsFile.exists()) {
            plugin.saveResource("jobs.yml", false);
        }
        jobsConfig = YamlConfiguration.loadConfiguration(jobsFile);

        // Load leaderboard.yml
        File leaderBoardFile = new File(plugin.getDataFolder(), "leaderboard.yml");
        if (!leaderBoardFile.exists()) {
            try {
                leaderBoardFile.createNewFile();
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to create leaderboard.yml: " + e.getMessage());
            }
        }
        leaderboardConfig = YamlConfiguration.loadConfiguration(leaderBoardFile);
    }

    public void loadMessages() {
        this.messages = new HashMap<>();
        ConfigurationSection messagesSection = plugin.getConfig().getConfigurationSection("messages");
        if (messagesSection == null) return;
        for (String key : messagesSection.getKeys(false)) {
            YamlMessage yamlMessage = new YamlMessage(messagesSection.get(key));
            messages.put(key, yamlMessage);
        }
    }

    public YamlMessage getMessage(String key) {
        return messages.getOrDefault(key, EMPTY_YAML_MESSAGE);
    }

    public void saveLeaderboard() {
        try {
            leaderboardConfig.save(new File(plugin.getDataFolder(), "leaderboard.yml"));
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save leaderboard.yml: " + e.getMessage());
        }
    }

}
