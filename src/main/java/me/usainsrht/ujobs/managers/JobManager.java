package me.usainsrht.ujobs.managers;

import lombok.Getter;
import me.usainsrht.ujobs.UJobsPlugin;
import me.usainsrht.ujobs.models.Action;
import me.usainsrht.ujobs.models.BuiltInActions;
import me.usainsrht.ujobs.models.Job;
import me.usainsrht.ujobs.models.PlayerJobData;
import me.usainsrht.ujobs.utils.JobExpUtils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;

@Getter
public class JobManager {
    private final UJobsPlugin plugin;
    private final Map<String, Job> jobs;
    private final Map<Action, Set<Job>> actionJobMap;

    public JobManager(UJobsPlugin plugin) {
        this.plugin = plugin;
        this.jobs = new LinkedHashMap<>();
        this.actionJobMap = new HashMap<>();
    }

    public void loadJobs() {
        jobs.clear();
        actionJobMap.clear();

        ConfigurationSection jobsSection = plugin.getConfigManager().getJobsConfig()
                .getConfigurationSection("jobs");

        if (jobsSection == null) {
            plugin.getLogger().warning("No jobs section found in config!");
            return;
        }

        int jobCount = 0;
        for (String jobId : jobsSection.getKeys(false)) {
            try {
                Job job = loadJob(jobId, jobsSection.getConfigurationSection(jobId));
                if (job != null) {
                    jobs.put(jobId, job);
                    jobCount++;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load job: " + jobId + " - " + e.getMessage());
            }
        }

        plugin.getLogger().info("Loaded " + jobCount + " jobs successfully!");
    }

    private Job loadJob(String jobId, ConfigurationSection jobSection) {
        if (jobSection == null) return null;

        // Load basic job info
        String nameText = jobSection.getString("name", jobId);
        Component name = plugin.getMiniMessage().deserialize(nameText);

        String iconString = jobSection.getString("icon", "STONE");
        Material icon;
        try {
            icon = Material.valueOf(iconString.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid icon material for job " + jobId + ": " + iconString);
            icon = Material.STONE;
        }

        String levelEquation = jobSection.getString("level_equation", "<nextlevel>*<nextlevel>*5");

        String soundString = jobSection.getString("levelup_sound", "ENTITY_PLAYER_LEVELUP");
        Sound levelUpSound;
        try {
            levelUpSound = Sound.valueOf(soundString.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound for job " + jobId + ": " + soundString);
            levelUpSound = Sound.ENTITY_PLAYER_LEVELUP;
        }

        // Load boss bar config
        Job.BossBarConfig bossBarConfig = loadBossBarConfig(jobSection.getConfigurationSection("bossbar"));

        Job job = new Job(jobId, name, icon, levelEquation, levelUpSound, bossBarConfig);

        // Load actions
        ConfigurationSection actionsSection = jobSection.getConfigurationSection("actions");
        if (actionsSection != null) {
            loadJobActions(job, actionsSection);
        }

        return job;
    }

    private Job.BossBarConfig loadBossBarConfig(ConfigurationSection bossBarSection) {
        if (bossBarSection == null) {
            return new Job.BossBarConfig(
                    "<yellow><level> <gray>seviye <job> <gradient:#6D6666:#938B8B:#777676>+<incomeexp>xp <exp>/<nextexp>",
                    "<gradient:#5DFF00:#E8FF00:{phase}><job> yeteneğinde <bold><level></bold>. seviyeye yükseldin!",
                    BossBar.Color.BLUE,
                    BossBar.Overlay.NOTCHED_10
            );
        }

        String titleTemplate = bossBarSection.getString("title",
                "<yellow><level> <gray>seviye <job> <gradient:#6D6666:#938B8B:#777676>+<incomeexp>xp <exp>/<nextexp>");
        String levelUpTemplate = bossBarSection.getString("levelup",
                "<gradient:#5DFF00:#E8FF00:{phase}><job> yeteneğinde <bold><level></bold>. seviyeye yükseldin!");

        BossBar.Color color;
        try {
            color = BossBar.Color.valueOf(bossBarSection.getString("color", "BLUE").toUpperCase());
        } catch (IllegalArgumentException e) {
            color = BossBar.Color.BLUE;
        }

        BossBar.Overlay overlay;
        try {
            overlay = BossBar.Overlay.valueOf(bossBarSection.getString("overlay", "NOTCHED_10").toUpperCase());
        } catch (IllegalArgumentException e) {
            overlay = BossBar.Overlay.NOTCHED_10;
        }

        return new Job.BossBarConfig(titleTemplate, levelUpTemplate, color, overlay);
    }

    private void loadJobActions(Job job, ConfigurationSection actionsSection) {
        for (String actionName : actionsSection.getKeys(false)) {
            Action action = BuiltInActions.get(actionName);
            if (action == null) return;
            ConfigurationSection actionSection = actionsSection.getConfigurationSection(actionName);
            if (actionSection != null) {
                for (String value : actionSection.getKeys(false)) {
                    ConfigurationSection rewardSection = actionSection.getConfigurationSection(value);
                    if (rewardSection != null) {
                        double exp = rewardSection.getDouble("exp", 0.0);
                        double money = rewardSection.getDouble("money", 0.0);
                        job.addAction(action, value, new Job.ActionReward(exp, money));
                        actionJobMap.computeIfAbsent(action, k -> new HashSet<>()).add(job);
                    }
                }
            }
        }
    }

    public void processAction(Player player, Action action, String value, Job job, int amount) {
        PlayerJobData playerJobData = plugin.getStorage().getCached(player.getUniqueId());
        if (playerJobData == null) return; //job data has to be loaded at this point

        Job.ActionReward reward = job.getActionReward(action, value);
        if (reward == null) return;

        JobExpUtils.processJobExp(player, job, reward, amount);

    }
}