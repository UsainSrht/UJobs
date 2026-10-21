package me.usainsrht.ujobs.utils;

import me.usainsrht.ujobs.UJobsPlugin;
import me.usainsrht.ujobs.api.events.PlayerJobExpGainEvent;
import me.usainsrht.ujobs.api.events.PlayerJobLevelUpEvent;
import me.usainsrht.ujobs.models.Action;
import me.usainsrht.ujobs.models.Job;
import me.usainsrht.ujobs.models.PlayerJobData;
import me.usainsrht.ujobs.yaml.YamlMessage;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JobExpUtils {

    public static void processJobExp(Player player, Job job, Job.ActionReward reward, int amount) {
        processJobExp(player, job, null, null, reward, amount);
    }

    public static void processJobExp(Player player, Job job, @Nullable Action action, @Nullable String actionValue, Job.ActionReward reward, int amount) {
        UJobsPlugin plugin = UJobsPlugin.instance;
        if (plugin == null || player == null || job == null || reward == null || amount <= 0) {
            return;
        }

        PlayerJobData playerData = plugin.getStorage().getCached(player.getUniqueId());
        if (playerData == null) {
            return;
        }
        PlayerJobData.JobStats stats = playerData.getJobStats(job.getId());

        // Fire PlayerJobExpGainEvent
        boolean isAsync = !Bukkit.isPrimaryThread();
        PlayerJobExpGainEvent expEvent = new PlayerJobExpGainEvent(
                player, job, action, actionValue, reward.getExp(), reward.getMoney(), amount, isAsync
        );
        Bukkit.getPluginManager().callEvent(expEvent);

        if (expEvent.isCancelled()) {
            return;
        }

        // Use modified values from event
        double effectiveExp = expEvent.getExp();
        double effectiveMoney = expEvent.getMoney();
        int effectiveAmount = expEvent.getAmount();

        if (effectiveAmount <= 0) {
            return;
        }

        double expGain = effectiveExp * effectiveAmount;
        double moneyGain = effectiveMoney * effectiveAmount;

        boolean leveledUp = false;
        int newLevel;
        double newExp;
        List<String> commandsToRun = null;
        YamlMessage messageToSend = null;

        synchronized (stats) {
            int currentLevel = stats.getLevel();
            double currentExp = stats.getExp();
            newExp = currentExp + expGain;

            long requiredExp = job.calculateExpForLevel(currentLevel);

            if (newExp >= requiredExp) {
                int targetLevel = currentLevel + 1;

                PlayerJobLevelUpEvent levelUpEvent = new PlayerJobLevelUpEvent(
                        player, job, currentLevel, targetLevel,
                        new ArrayList<>(job.getLevelUpCommands()), job.getLevelUpMessage(), isAsync
                );
                Bukkit.getPluginManager().callEvent(levelUpEvent);

                if (!levelUpEvent.isCancelled()) {
                    leveledUp = true;
                    newLevel = levelUpEvent.getToLevel();
                    newExp = newExp - requiredExp; // Carry over excess exp

                    stats.setLevel(newLevel);
                    stats.setExp(newExp);

                    commandsToRun = levelUpEvent.getCommands();
                    messageToSend = levelUpEvent.getMessage();
                } else {
                    newLevel = currentLevel;
                    stats.setExp(Math.min(newExp, requiredExp));
                }
            } else {
                newLevel = currentLevel;
                stats.setExp(newExp);
            }
        }

        if (leveledUp) {
            handleLevelUp(player, job, newLevel, commandsToRun, messageToSend);
        }

        // Add money
        if (moneyGain > 0 && plugin.getEconomy() != null) {
            final double finalMoneyGain = moneyGain;
            plugin.getMorePaperLib().scheduling().entitySpecificScheduler(player).run(
                    () -> plugin.getEconomy().depositPlayer(player, finalMoneyGain),
                    null
            );
            synchronized (stats) {
                stats.setTotalMoney(stats.getTotalMoney() + moneyGain);
            }
        }

        // Show boss bar
        if (plugin.getConfig().getBoolean("progress_bossbars", true)) {
            showJobBossBar(player, job, newLevel, newExp, expGain, leveledUp);
        }
    }

    public static void handleLevelUp(Player player, Job job, int newLevel) {
        handleLevelUp(player, job, newLevel, job.getLevelUpCommands(), job.getLevelUpMessage());
    }

    public static void handleLevelUp(Player player, Job job, int newLevel, @Nullable List<String> commands, @Nullable YamlMessage message) {
        UJobsPlugin plugin = UJobsPlugin.instance;
        if (plugin == null || player == null || job == null) return;

        if (message != null) {
            MessageUtil.send(player, message,
                    Placeholder.unparsed("player", player.getName()),
                    Formatter.number("level", newLevel));
        }

        if (commands != null) {
            for (String cmd : commands) {
                String parsedCmd = cmd.replace("<level>", String.valueOf(newLevel));
                List<String> all = new ArrayList<>();
                Matcher m = Pattern.compile("([0-9()+\\-*/.]+)").matcher(parsedCmd);
                while (m.find()) all.add(m.group(1));
                for (String math : all) {
                    // Skip plain numbers or negative values without arithmetic operators (+, *, /, (, ) or binary -)
                    // Using lastIndexOf('-') <= 0 allows negative values like -200 to be skipped while evaluating subtraction like 10-5 or -10-5
                    if (!math.contains("+") && !math.contains("*") && !math.contains("/") && !math.contains("(") && !math.contains(")") && math.lastIndexOf('-') <= 0) {
                        continue;
                    }
                    try {
                        double result = MathUtil.eval(math);
                        // Format whole numbers as integers to avoid passing doubles (e.g. 8.0, 100.0) to commands expecting ints
                        String formatted = (result == (long) result) ? String.valueOf((long) result) : String.valueOf(result);
                        parsedCmd = parsedCmd.replace(math, formatted);
                    } catch (Exception ignored) {
                        // Ignore invalid math expressions
                    }
                }
                parsedCmd = parsedCmd.replace("<player>", player.getName());
                String finalParsedCmd = parsedCmd;
                plugin.getMorePaperLib().scheduling().globalRegionalScheduler().run(() ->
                        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), finalParsedCmd));
            }
        }

        // Show level up boss bar animation
        showLevelUpAnimation(player, job, newLevel);

        // Check leaderboard changes asynchronously
        plugin.getLeaderboardManager().checkLeaderboardChange(player.getUniqueId(), job, newLevel);
    }

    public static void showJobBossBar(Player player, Job job, int level, double exp, double expGain, boolean leveledUp) {
        UJobsPlugin plugin = UJobsPlugin.instance;
        if (plugin == null || player == null || job == null) return;

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
        if (plugin == null || player == null || job == null) return;

        String levelUpTemplate = job.getBossBarConfig().getLevelUpTemplate();

        AtomicInteger ticks = new AtomicInteger(0);

        plugin.getMorePaperLib().scheduling().entitySpecificScheduler(player).runAtFixedRate(task -> {
            if (ticks.getAndIncrement() >= 60) { // 3 seconds
                task.cancel();
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

        }, () -> {}, 1L, 1L);
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
