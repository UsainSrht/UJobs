package me.usainsrht.ujobs;

import lombok.Getter;
import me.usainsrht.ujobs.managers.*;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.logging.Level;

@Getter
public final class UJobsPlugin extends JavaPlugin {

    public static UJobsPlugin instance;
    private MiniMessage miniMessage;
    private Economy economy;

    // Managers
    private ConfigManager configManager;
    private JobManager jobManager;
    private JobDataManager dataManager;
    private LeaderboardManager leaderboardManager;
    private JobGUIManager guiManager;
    private BossBarManager bossBarManager;
    private NPCManager npcManager;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize Adventure API
        this.miniMessage = MiniMessage.miniMessage();

        // Setup Vault Economy
        if (!setupEconomy()) {
            getLogger().severe("Disabled due to no Vault dependency found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize managers
        initializeManagers();

        // Register events and commands
        registerEventsAndCommands();

        // Start leaderboard calculation timer
        startLeaderboardTimer();

        getLogger().info("UJobs has been enabled successfully!");
    }

    @Override
    public void onDisable() {

        // Save all data
        if (dataManager != null) {
            dataManager.saveAllData();
        }

        // Cancel all boss bars
        if (bossBarManager != null) {
            bossBarManager.removeAllBossBars();
        }

        getLogger().info("UJobs has been disabled!");
    }

    private void initializeManagers() {
        try {
            this.configManager = new ConfigManager(this);
            this.jobManager = new JobManager(this);
            this.dataManager = new JobDataManager(this);
            this.leaderboardManager = new LeaderboardManager(this);
            this.bossBarManager = new BossBarManager(this);
            this.guiManager = new JobGUIManager(this);
            this.npcManager = new NPCManager(this);

            // Load configurations
            configManager.loadConfigs();
            jobManager.loadJobs();

            getLogger().info("All managers initialized successfully!");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize managers", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void registerEventsAndCommands() {
        // Register event listeners
        getServer().getPluginManager().registerEvents(new JobEventListener(this), this);


    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    private void startLeaderboardTimer() {
        new BukkitRunnable() {
            @Override
            public void run() {
                leaderboardManager.calculateLeaderboard();
            }
        }.runTaskTimerAsynchronously(this, 20L * 60 * 2, 20L * 60 * 60); // Every hour after 2 minute delay
    }


}
