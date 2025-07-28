package me.usainsrht.ujobs.managers;

import lombok.Getter;
import me.usainsrht.ujobs.UJobsPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

@Getter
public class ConfigManager {

    private UJobsPlugin plugin;
    private YamlConfiguration jobsConfig;

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

        // Load jobs.yml
        File jobsFile = new File(plugin.getDataFolder(), "jobs.yml");
        if (!jobsFile.exists()) {
            plugin.saveResource("jobs.yml", false);
        }
        jobsConfig = YamlConfiguration.loadConfiguration(jobsFile);
    }

}
