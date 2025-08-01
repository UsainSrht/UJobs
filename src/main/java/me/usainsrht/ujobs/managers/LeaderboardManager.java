package me.usainsrht.ujobs.managers;

import me.usainsrht.ujobs.UJobsPlugin;
import me.usainsrht.ujobs.models.Job;
import me.usainsrht.ujobs.models.PlayerLeaderboardData;
import me.usainsrht.ujobs.utils.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.*;

public class LeaderboardManager {

    UJobsPlugin plugin;
    Map<UUID, PlayerLeaderboardData> leaderboardPlayerCache;
    Map<Job, UUID[]> leaderboardJobCache;

    public LeaderboardManager(UJobsPlugin plugin) {
        this.plugin = plugin;
        this.leaderboardPlayerCache = new HashMap<>();
        this.leaderboardJobCache = new HashMap<>();

        for (Job job : plugin.getJobManager().getJobs().values()) {
            leaderboardJobCache.put(job, new UUID[100]);
        }
    }

    public void load(ConfigurationSection yml) {
        ConfigurationSection leaderboardSection = yml.getConfigurationSection("leaderboard");
        if (leaderboardSection == null) return;
        leaderboardSection.getKeys(false).forEach(jobId -> {
            Job job = plugin.getJobManager().getJobs().get(jobId);
            if (job == null) return;

            ConfigurationSection jobSection = yml.getConfigurationSection("leaderboard." + jobId);
            if (jobSection == null) return;

            jobSection.getKeys(false).forEach(uuidString -> {
                UUID uuid = UUID.fromString(uuidString);
                int position = jobSection.getInt(uuidString + ".position", -1);
                int level = jobSection.getInt(uuidString + ".level", -1);

                if (position < 0 || level < 0) return;

                PlayerLeaderboardData playerData = leaderboardPlayerCache.computeIfAbsent(uuid, PlayerLeaderboardData::new);
                playerData.getLeaderboardStats().put(job, new PlayerLeaderboardData.LeaderboardStats(position, level));

                leaderboardJobCache.get(job)[position] = uuid;
            });
        });
    }

    public void save() {
        YamlConfiguration leaderboardConfig = plugin.getConfigManager().getLeaderboardConfig();

        leaderboardConfig.set("leaderboard", null); // Clear existing leaderboard data

        leaderboardPlayerCache.forEach(((uuid, playerLeaderboardData) -> {
            playerLeaderboardData.getLeaderboardStats().forEach((job, stats) -> {
                String path = "leaderboard." + job.getId() + "." + uuid.toString();
                leaderboardConfig.set(path + ".position", stats.getPosition());
                leaderboardConfig.set(path + ".level", stats.getLevel());
            });
        }));

        plugin.getConfigManager().saveLeaderboard();
    }

    public int getPosition(UUID uuid, Job job) {
        PlayerLeaderboardData data = leaderboardPlayerCache.get(uuid);
        if (data == null) return -1;
        return data.getLeaderboardStats().get(job).getPosition();
    }

    public UUID getPlayerByPosition(int position, Job job) {
        UUID[] leaderboard = leaderboardJobCache.get(job);
        if (leaderboard == null || position < 0 || position >= leaderboard.length) return null;
        return leaderboard[position];
    }

    public void checkLeaderboardChange(UUID uuid, Job job, int level) {
        int position = getPosition(uuid, job);
        if (position == -1) return;

        int oneHigher = position - 1;
        UUID opponent = getPlayerByPosition(oneHigher, job);
        if (opponent == null) return;

        int opponentLevel = leaderboardPlayerCache.get(opponent).getLeaderboardStats().get(job).getLevel();
        if (level > opponentLevel) {

            leaderboardPlayerCache.get(uuid).getLeaderboardStats().get(job).setPosition(oneHigher);
            leaderboardPlayerCache.get(uuid).getLeaderboardStats().get(job).setLevel(level);

            leaderboardPlayerCache.get(opponent).getLeaderboardStats().get(job).setPosition(position);

            leaderboardJobCache.get(job)[oneHigher] = uuid;
            leaderboardJobCache.get(job)[position] = opponent;

            // Prepare placeholders and messages
            OfflinePlayer opponentPlayer = Bukkit.getOfflinePlayer(opponent);
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);

            Set<TagResolver> placeholderSet = new HashSet<>();
            placeholderSet.add(Placeholder.component("opponent_displayname", opponentPlayer.isOnline() ? opponentPlayer.getPlayer().displayName() : Component.text(opponentPlayer.getName())));
            placeholderSet.add(Placeholder.unparsed("opponent_name", opponentPlayer.getName()));
            placeholderSet.add(Placeholder.component("displayname", player.isOnline() ? player.getPlayer().displayName() : Component.text(player.getName())));
            placeholderSet.add(Placeholder.unparsed("name", player.getName()));
            placeholderSet.add(Formatter.number("level", level));
            placeholderSet.add(Formatter.number("position", oneHigher));
            placeholderSet.add(Placeholder.component("job", job.getName()));
            placeholderSet.add(Placeholder.styling("primary", job.getName().children().getFirst().color()));
            placeholderSet.add(Placeholder.styling("secondary", job.getName().children().getLast().color()));

            TagResolver[] placeholders = placeholderSet.toArray(new TagResolver[]{});

            if (player.isOnline()) MessageUtil.send(player.getPlayer(), plugin.getConfigManager().getMessage("messages.leaderboard.take_someones_position"), placeholders);
            if (opponentPlayer.isOnline()) MessageUtil.send(opponentPlayer.getPlayer(), plugin.getConfigManager().getMessage("leaderboard.your_position_taken"), placeholders);

            if (oneHigher == 1) {
                MessageUtil.send(plugin.getServer(), plugin.getConfigManager().getMessage("messages.leaderboard.take_lead"), placeholders);
            } else if (oneHigher == 10) {
                MessageUtil.send(plugin.getServer(), plugin.getConfigManager().getMessage("messages.leaderboard.get_in_top_10"), placeholders);
            }

            save();
        }
    }
}
