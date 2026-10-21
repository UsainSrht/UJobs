package me.usainsrht.ujobs.api;

import me.usainsrht.ujobs.UJobsPlugin;
import me.usainsrht.ujobs.managers.JobManager;
import me.usainsrht.ujobs.managers.LeaderboardManager;
import me.usainsrht.ujobs.models.Action;
import me.usainsrht.ujobs.models.Job;
import me.usainsrht.ujobs.models.PlayerJobData;
import me.usainsrht.ujobs.storage.Storage;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface UJobsAPI {

    static UJobsAPI get() {
        return UJobsPlugin.getInstance().getApi();
    }

    JobManager getJobManager();

    LeaderboardManager getLeaderboardManager();

    Storage getStorage();

    Collection<Job> getJobs();

    @Nullable
    Job getJob(String jobId);

    Optional<Job> findJob(String jobId);

    boolean hasJob(String jobId);

    @Nullable
    Job registerJob(Job job);

    boolean unregisterJob(String jobId);

    void modifyJob(String jobId, Consumer<Job> modifier);

    Action registerAction(Action action);

    @Nullable
    Action getAction(String name);

    Collection<Action> getRegisteredActions();

    void addActionToJob(String jobId, Action action, String value, Job.ActionReward reward);

    boolean removeActionFromJob(String jobId, Action action, String value);

    void processAction(Player player, Action action, String value, int amount);

    void processAction(Player player, Action action, String value, Job job, int amount);

    CompletableFuture<PlayerJobData> getPlayerData(UUID uuid);

    @Nullable
    PlayerJobData getCachedPlayerData(UUID uuid);

    void addExp(Player player, Job job, double exp);

    void addExp(Player player, String jobId, double exp);

    void setLevel(Player player, Job job, int level);

    void setLevel(Player player, String jobId, int level);
}
