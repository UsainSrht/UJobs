package me.usainsrht.ujobs.api;

import me.usainsrht.ujobs.UJobsPlugin;
import me.usainsrht.ujobs.managers.JobManager;
import me.usainsrht.ujobs.managers.LeaderboardManager;
import me.usainsrht.ujobs.models.Action;
import me.usainsrht.ujobs.models.ActionRegistry;
import me.usainsrht.ujobs.models.Job;
import me.usainsrht.ujobs.models.PlayerJobData;
import me.usainsrht.ujobs.storage.Storage;
import me.usainsrht.ujobs.utils.JobExpUtils;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class UJobsAPIImpl implements UJobsAPI {

    private final UJobsPlugin plugin;

    public UJobsAPIImpl(UJobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public JobManager getJobManager() {
        return plugin.getJobManager();
    }

    @Override
    public LeaderboardManager getLeaderboardManager() {
        return plugin.getLeaderboardManager();
    }

    @Override
    public Storage getStorage() {
        return plugin.getStorage();
    }

    @Override
    public Collection<Job> getJobs() {
        return plugin.getJobManager().getJobs().values();
    }

    @Override
    public @Nullable Job getJob(String jobId) {
        return plugin.getJobManager().getJob(jobId);
    }

    @Override
    public Optional<Job> findJob(String jobId) {
        return Optional.ofNullable(getJob(jobId));
    }

    @Override
    public boolean hasJob(String jobId) {
        return plugin.getJobManager().hasJob(jobId);
    }

    @Override
    public @Nullable Job registerJob(Job job) {
        return plugin.getJobManager().registerJob(job);
    }

    @Override
    public boolean unregisterJob(String jobId) {
        return plugin.getJobManager().unregisterJob(jobId);
    }

    @Override
    public void modifyJob(String jobId, Consumer<Job> modifier) {
        plugin.getJobManager().modifyJob(jobId, modifier);
    }

    @Override
    public Action registerAction(Action action) {
        return ActionRegistry.register(action);
    }

    @Override
    public @Nullable Action getAction(String name) {
        return ActionRegistry.get(name);
    }

    @Override
    public Collection<Action> getRegisteredActions() {
        return ActionRegistry.getAll();
    }

    @Override
    public void addActionToJob(String jobId, Action action, String value, Job.ActionReward reward) {
        plugin.getJobManager().addActionToJob(jobId, action, value, reward);
    }

    @Override
    public boolean removeActionFromJob(String jobId, Action action, String value) {
        return plugin.getJobManager().removeActionFromJob(jobId, action, value);
    }

    @Override
    public void processAction(Player player, Action action, String value, int amount) {
        plugin.getJobManager().processAction(player, action, value, amount);
    }

    @Override
    public void processAction(Player player, Action action, String value, Job job, int amount) {
        plugin.getJobManager().processAction(player, action, value, job, amount);
    }

    @Override
    public CompletableFuture<PlayerJobData> getPlayerData(UUID uuid) {
        return plugin.getStorage().load(uuid);
    }

    @Override
    public @Nullable PlayerJobData getCachedPlayerData(UUID uuid) {
        return plugin.getStorage().getCached(uuid);
    }

    @Override
    public void addExp(Player player, Job job, double exp) {
        if (player == null || job == null || exp <= 0) return;
        Job.ActionReward reward = new Job.ActionReward(exp, 0);
        JobExpUtils.processJobExp(player, job, reward, 1);
    }

    @Override
    public void addExp(Player player, String jobId, double exp) {
        Job job = getJob(jobId);
        if (job != null) {
            addExp(player, job, exp);
        }
    }

    @Override
    public void setLevel(Player player, Job job, int level) {
        if (player == null || job == null) return;
        plugin.getMorePaperLib().scheduling().entitySpecificScheduler(player).run(() -> {
            PlayerJobData playerJobData = plugin.getStorage().getCached(player.getUniqueId());
            if (playerJobData == null) return;
            PlayerJobData.JobStats jobStats = playerJobData.getJobStats(job.getId());
            synchronized (jobStats) {
                jobStats.setLevel(level);
                jobStats.setExp(0);
            }
            plugin.getLeaderboardManager().checkLeaderboardChange(player.getUniqueId(), job, level);
        }, null);
    }

    @Override
    public void setLevel(Player player, String jobId, int level) {
        Job job = getJob(jobId);
        if (job != null) {
            setLevel(player, job, level);
        }
    }
}
