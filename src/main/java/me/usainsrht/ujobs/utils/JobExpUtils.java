package me.usainsrht.ujobs.utils;

import me.usainsrht.ujobs.models.Job;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class JobExpUtils {
    private final UJobsPlugin plugin;

    public JobExpUtils(UJobsPlugin plugin) {
        this.plugin = plugin;
    }

    public void addJobExp(Player player, String action, String value, int amount) {
        // Check all jobs that have this action/value combination
        for (Job job : plugin.getJobManager().getAllJobs()) {
            Job.ActionReward reward = job.getActionReward(action, value);
            if (reward != null) {
                processJobExp(player, job, reward, amount);
            }
        }
    }

    private void processJobExp(Player player, Job job, Job.ActionReward reward, int amount) {
        PlayerJobData playerData = plugin.getDataManager().getPlayerData(player.getUniqueId());
        PlayerJobData.JobStats stats = playerData.getJobStats(job.getId());

        // Calculate rewards
        long expGain = (long) (reward.getExp() * amount);
        double moneyGain = reward.getMoney() * amount;

        // Current stats
        int currentLevel = stats.getLevel();
        long currentExp = stats.getExp();
        long newExp = currentExp + expGain;

        // Calculate required exp for next level
        long requiredExp = job.calculateExpForLevel(currentLevel);

        // Check for level up
        boolean leveledUp = false;
        int newLevel = currentLevel;

        if (newExp >= requiredExp) {
            leveledUp = true;
            newLevel = currentLevel + 1;
            newExp = newExp - requiredExp; // Carry over excess exp

            stats.setLevel(newLevel);
            stats.setExp(newExp);

            // Handle level up
            handleLevelUp(player, job, newLevel);
        } else {
            stats.setExp(newExp);
        }

        // Add money
        if (moneyGain > 0) {
            plugin.getEconomy().depositPlayer(player, moneyGain);
            stats.setTotalMoney(stats.getTotalMoney() + moneyGain);
        }

        // Show boss bar
        showJobBossBar(player, job, newLevel, newExp, expGain, leveledUp);

        // Save data
        plugin.getDataManager().savePlayerData(playerData);
    }

    private void handleLevelUp(Player player, Job job, int newLevel) {
        // Play sound
        player.playSound(player.getLocation(), job.getLevelUpSound(), 1.0f, 1.0f);

        // Show level up boss bar animation
        showLevelUpAnimation(player, job, newLevel);

        // Check leaderboard changes
        plugin.getLeaderboardManager().checkLeaderboardChange(player, job.getId(), newLevel);
    }

    private void showJobBossBar(Player player, Job job, int level, long exp, long expGain, boolean leveledUp) {
        long requiredExp = job.calculateExpForLevel(level);
        double progress = Math.min(1.0, (double) exp / requiredExp);

        // Create boss bar title with placeholders
        String titleTemplate = job.getBossBarConfig().getTitleTemplate();
        Component title = plugin.getMiniMessageHelper().deserialize(titleTemplate,
                Placeholder.unparsed("level", String.valueOf(level)),
                Placeholder.component("job", job.getName()),
                Placeholder.unparsed("incomeexp", String.valueOf(expGain)),
                Placeholder.unparsed("exp", String.valueOf(exp)),
                Placeholder.unparsed("nextexp", String.valueOf(requiredExp))
        );

        BossBar bossBar = BossBar.bossBar(
                title,
                (float) progress,
                job.getBossBarConfig().getColor(),
                job.getBossBarConfig().getOverlay()
        );

        plugin.getBossBarManager().showBossBar(player, job.getId(), bossBar, 3);
    }

    private void showLevelUpAnimation(Player player, Job job, int newLevel) {
        String levelUpTemplate = job.getBossBarConfig().getLevelUpTemplate();

        new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 60) { // 3 seconds
                    cancel();
                    return;
                }

                // Create animated phase value
                double phase = Math.random();
                String animatedTemplate = levelUpTemplate.replace("{phase}", String.valueOf(phase % 1.0));

                Component title = plugin.getMiniMessageHelper().deserialize(animatedTemplate,
                        Placeholder.component("job", job.getName()),
                        Placeholder.unparsed("level", String.valueOf(newLevel))
                );

                BossBar levelUpBar = BossBar.bossBar(
                        title,
                        1.0f,
                        job.getBossBarConfig().getColor(),
                        job.getBossBarConfig().getOverlay()
                );

                plugin.getBossBarManager().showBossBar(player, job.getId() + "_levelup", levelUpBar, 1);

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public int calculatePlayerLevel(String jobId, long experience) {
        Job job = plugin.getJobManager().getJob(jobId);
        if (job == null) return 0;

        int level = 0;
        long totalExpNeeded = 0;

        while (totalExpNeeded <= experience) {
            long expForNextLevel = job.calculateExpForLevel(level);
            if (totalExpNeeded + expForNextLevel > experience) {
                break;
            }
            totalExpNeeded += expForNextLevel;
            level++;
        }

        return level;
    }

    public long getExpForCurrentLevel(String jobId, int level, long totalExp) {
        Job job = plugin.getJobManager().getJob(jobId);
        if (job == null) return 0;

        long expUsed = 0;
        for (int i = 0; i < level; i++) {
            expUsed += job.calculateExpForLevel(i);
        }

        return totalExp - expUsed;
    }
}