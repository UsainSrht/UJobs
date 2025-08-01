package me.usainsrht.ujobs.utils;

import me.usainsrht.ujobs.UJobsPlugin;
import me.usainsrht.ujobs.models.Job;
import me.usainsrht.ujobs.models.PlayerJobData;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class JobExpUtils {

    public static void processJobExp(Player player, Job job, Job.ActionReward reward, int amount) {
        UJobsPlugin plugin = UJobsPlugin.instance;

        PlayerJobData playerData = plugin.getStorage().getCached(player.getUniqueId());
        PlayerJobData.JobStats stats = playerData.getJobStats(job.getId());

        // Calculate rewards
        double expGain = reward.getExp() * amount;
        double moneyGain = reward.getMoney() * amount;

        // Current stats
        int currentLevel = stats.getLevel();
        double currentExp = stats.getExp();
        double newExp = currentExp + expGain;

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
        //plugin.getStorage().save(playerData.getUuid());
    }

    public static void handleLevelUp(Player player, Job job, int newLevel) {
        UJobsPlugin plugin = UJobsPlugin.instance;
        // Play sound
        player.playSound(player.getLocation(), job.getLevelUpSound(), 1.0f, 1.0f);

        // Show level up boss bar animation
        showLevelUpAnimation(player, job, newLevel);

        // Check leaderboard changes
        plugin.getLeaderboardManager().checkLeaderboardChange(player.getUniqueId(), job, newLevel);
    }

    public static void showJobBossBar(Player player, Job job, int level, double exp, double expGain, boolean leveledUp) {
        UJobsPlugin plugin = UJobsPlugin.instance;
        long requiredExp = job.calculateExpForLevel(level);
        double progress = Math.min(1.0, exp / requiredExp);

        // Create boss bar title with placeholders
        String titleTemplate = job.getBossBarConfig().getTitleTemplate();
        Component title = plugin.getMiniMessage().deserialize(titleTemplate,
                Formatter.number("level", level),
                Placeholder.component("job", job.getName()),
                Placeholder.unparsed("symbol_exp", plugin.getConfig().getString("symbols.exp", "xp")),
                Formatter.number("gained_exp", expGain),
                Formatter.number("exp", exp),
                Formatter.number("next_exp", requiredExp)
        );

        BossBar bossBar = BossBar.bossBar(
                title,
                (float) progress,
                job.getBossBarConfig().getColor(),
                job.getBossBarConfig().getOverlay()
        );

        plugin.getBossBarManager().showBossBar(player, job.getId(), bossBar, 3);
    }

    public static void showLevelUpAnimation(Player player, Job job, int newLevel) {
        UJobsPlugin plugin = UJobsPlugin.instance;
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

                Component title = plugin.getMiniMessage().deserialize(animatedTemplate,
                        Placeholder.component("job", job.getName()),
                        Formatter.number("level", newLevel)
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

    public int calculatePlayerLevel(String jobId, double experience) {
        Job job = UJobsPlugin.instance.getJobManager().getJobs().get(jobId);
        if (job == null) return 0;

        int level = 0;
        double totalExpNeeded = 0;

        while (totalExpNeeded <= experience) {
            double expForNextLevel = job.calculateExpForLevel(level);
            if (totalExpNeeded + expForNextLevel > experience) {
                break;
            }
            totalExpNeeded += expForNextLevel;
            level++;
        }

        return level;
    }

    public double getExpForCurrentLevel(String jobId, int level, double totalExp) {
        Job job = UJobsPlugin.instance.getJobManager().getJobs().get(jobId);
        if (job == null) return 0;

        double expUsed = 0;
        for (int i = 0; i < level; i++) {
            expUsed += job.calculateExpForLevel(i);
        }

        return totalExp - expUsed;
    }
}