package me.usainsrht.ujobs.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.usainsrht.ujobs.UJobsPlugin;
import me.usainsrht.ujobs.models.Job;
import me.usainsrht.ujobs.models.PlayerJobData;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.UUID;

public class JobPlaceholders extends PlaceholderExpansion {

    private final UJobsPlugin plugin; //

    public JobPlaceholders(UJobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public String getAuthor() {
        return String.join(", ", plugin.getPluginMeta().getAuthors());
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "ujobs";
    }

    @Override
    @NotNull
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    /*
    * ujobs_job_name_<job>                       - Job name                                             - Hunter
    * ujobs_job_displayname_<job>                - Job name with colors                                 - <red>Hunter
    * ujobs_job_legacydisplayname_<job>          - Job name with legacy colors                          - &cHunter
    *
    * ujobs_player_level_<job>                   - Player's level in the job                            - 5
    * ujobs_player_exp_<job>                     - Player's exp in the job                              - 1260.8
    * ujobs_player_position_<job>                - Player's position in the job leaderboard             - 12
    * ujobs_player_totalmoney_<job>              - Total amount of money player has gained in the job   - 14179.5
    *
    * ujobs_leaderboard_name_<job>_<position>    - Player name at position in job leaderboard           - UsainSrht
    * ujobs_leaderboard_level_<job>_<position>   - Player level at position in job leaderboard          - 31
    * */

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        String[] param = params.split("_");
        if (param.length < 3) return null;
        String jobId = param[2];
        Job job = plugin.getJobManager().getJobs().get(jobId);
        if (job == null) return null;
        if (param[0].equalsIgnoreCase("job")) {
            if (param[1].equalsIgnoreCase("name")) {
                return PlainTextComponentSerializer.plainText().serialize(job.getName());
            } else if (param[1].equalsIgnoreCase("displayname")) {
                return plugin.getMiniMessage().serialize(job.getName());
            } else if (param[1].equalsIgnoreCase("legacydisplayname")) {
                return LegacyComponentSerializer.legacySection().serialize(job.getName());
            }
        } else if (param[0].equalsIgnoreCase("player")) {
            if (player == null || !player.isOnline()) return null;
            PlayerJobData playerJobData = plugin.getStorage().getCached(player.getUniqueId());
            if (playerJobData == null) return null;
            PlayerJobData.JobStats jobStats = playerJobData.getJobStats(jobId);
            if (param[1].equalsIgnoreCase("level")) {
                return jobStats != null ? String.valueOf(jobStats.getLevel()) : "0";
            } else if (param[1].equalsIgnoreCase("exp")) {
                return jobStats != null ? new DecimalFormat().format(jobStats.getExp()) : "0";
            } else if (param[1].equalsIgnoreCase("position")) {
                int position = plugin.getLeaderboardManager().getPosition(player.getUniqueId(), job);
                return String.valueOf(position + 1);
            } else if (param[1].equalsIgnoreCase("totalmoney")) {
                return jobStats != null ? String.valueOf(jobStats.getTotalMoney()) : "0";
            }
        } else if (param[0].equalsIgnoreCase("leaderboard")) {
            if (param.length < 4) return null;
            int position = Integer.parseInt(param[3]);
            UUID target = plugin.getLeaderboardManager().getPlayerByPosition(position - 1, job);
            if (target == null) return "?";
            if (param[1].equalsIgnoreCase("name")) {
                OfflinePlayer lbPlayer = plugin.getServer().getOfflinePlayer(target);
                return lbPlayer.getName();
            } else if (param[1].equalsIgnoreCase("level")) {
                return String.valueOf(plugin.getLeaderboardManager().getLevel(target, job));
            }
        }

        return null;
    }

}
