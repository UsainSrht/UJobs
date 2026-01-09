package me.usainsrht.ujobs.managers;

import lombok.Getter;
import me.usainsrht.ujobs.UJobsPlugin;
import me.usainsrht.ujobs.models.Job;
import me.usainsrht.ujobs.models.PlayerJobData;
import me.usainsrht.ujobs.models.PlayerLeaderboardData;
import me.usainsrht.ujobs.storage.PDCStorage;
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

@Getter
public class LeaderboardManager {

    UJobsPlugin plugin;
    Map<UUID, PlayerLeaderboardData> leaderboardPlayerCache;
    Map<Job, UUID[]> leaderboardJobCache;

    public LeaderboardManager(UJobsPlugin plugin) {
        this.plugin = plugin;
        this.leaderboardPlayerCache = new HashMap<>();
        this.leaderboardJobCache = new HashMap<>();
    }

    public void load(ConfigurationSection yml) {
        for (Job job : plugin.getJobManager().getJobs().values()) {
            leaderboardJobCache.put(job, new UUID[100]);
        }

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

        // Recompute ordered arrays from YAML-loaded stats to ensure positions reflect levels
        int calculateTop = plugin.getConfig().getInt("leaderboard.calculate_top", 100);
        for (Job job : plugin.getJobManager().getJobs().values()) {
            UUID[] ordered = new UUID[Math.max(calculateTop, 100)];
            // collect players who have stats for this job
            List<Map.Entry<UUID, PlayerLeaderboardData.LeaderboardStats>> entries = new ArrayList<>();
            for (Map.Entry<UUID, PlayerLeaderboardData> e : leaderboardPlayerCache.entrySet()) {
                PlayerLeaderboardData.LeaderboardStats stats = e.getValue().getLeaderboardStats().get(job);
                if (stats != null) entries.add(new AbstractMap.SimpleEntry<>(e.getKey(), stats));
            }
            // sort by level desc, then by stored position asc for stability
            entries.sort((a, b) -> {
                int levelCmp = Integer.compare(b.getValue().getLevel(), a.getValue().getLevel());
                if (levelCmp != 0) return levelCmp;
                return Integer.compare(a.getValue().getPosition(), b.getValue().getPosition());
            });

            int idx = 0;
            for (Map.Entry<UUID, PlayerLeaderboardData.LeaderboardStats> e : entries) {
                if (idx >= ordered.length) break;
                ordered[idx] = e.getKey();
                // update cached position to match sorted index
                leaderboardPlayerCache.computeIfAbsent(e.getKey(), PlayerLeaderboardData::new)
                        .getLeaderboardStats().put(job, new PlayerLeaderboardData.LeaderboardStats(idx, e.getValue().getLevel()));
                idx++;
            }

            leaderboardJobCache.put(job, ordered);
        }

        // Rebuild leaderboards from storage only if storage cache is already populated.
        // PDCStorage cache is populated asynchronously as players join, so avoid overwriting
        // YAML-loaded leaderboards with an empty in-memory cache during startup.
        if (plugin.getStorage() instanceof PDCStorage pdcStorage) {
            if (!pdcStorage.getCache().isEmpty()) {
                for (Job job : plugin.getJobManager().getJobs().values()) {
                    UUID[] rebuilt = createLeaderboard(job);
                    if (rebuilt != null) leaderboardJobCache.put(job, rebuilt);
                }
            }
        }
    }

    public void save() {
        YamlConfiguration leaderboardConfig = plugin.getConfigManager().getLeaderboardConfig();

        leaderboardConfig.set("leaderboard", null); // Clear existing leaderboard data

        for (Job job : plugin.getJobManager().getJobs().values()) {
            if (!leaderboardJobCache.containsKey(job) || leaderboardJobCache.get(job) == null || leaderboardJobCache.get(job)[0] == null) {
                leaderboardJobCache.put(job, createLeaderboard(job));
            }
        }

        leaderboardPlayerCache.forEach(((uuid, playerLeaderboardData) -> {
            playerLeaderboardData.getLeaderboardStats().forEach((job, stats) -> {
                String path = "leaderboard." + job.getId() + "." + uuid.toString();
                leaderboardConfig.set(path + ".position", stats.getPosition());
                leaderboardConfig.set(path + ".level", stats.getLevel());
            });
        }));

        plugin.getConfigManager().saveLeaderboard();
    }

    public UUID[] createLeaderboard(Job job) {
        if (plugin.getStorage() instanceof PDCStorage pdcStorage) {
            UUID[] leaderboard = new UUID[plugin.getConfig().getInt("leaderboard.calculate_top", 100)];
            leaderboardJobCache.put(job, leaderboard);

            for (PlayerJobData playerJobData : pdcStorage.getCache().values()) {
                int level = playerJobData.getJobStats(job.getId()).getLevel();
                if (level < 0) continue;
                // Find the correct index to insert based on level (descending)
                int insertIndex = 0;
                while (insertIndex < leaderboard.length && leaderboard[insertIndex] != null) {
                    UUID existingUuid = leaderboard[insertIndex];
                    int existingLevel = -1;
                    if (existingUuid != null) {
                        PlayerJobData existingPlayerJobData = pdcStorage.getCache().get(existingUuid);
                        if (existingPlayerJobData == null) break;
                        existingLevel = existingPlayerJobData.getJobStats(job.getId()).getLevel();
                    }
                    if (level > existingLevel) break;
                    insertIndex++;
                }
                if (insertIndex < leaderboard.length) {
                    // Shift lower entries down
                    System.arraycopy(leaderboard, insertIndex, leaderboard, insertIndex + 1, leaderboard.length - insertIndex - 1);
                    leaderboard[insertIndex] = playerJobData.getUuid();
                    leaderboardPlayerCache.computeIfAbsent(playerJobData.getUuid(), PlayerLeaderboardData::new)
                            .getLeaderboardStats().put(job, new PlayerLeaderboardData.LeaderboardStats(insertIndex, level));
                }
            }

            // Ensure leaderboard cache positions/levels are consistent for all entries
            for (int i = 0; i < leaderboard.length; i++) {
                UUID entry = leaderboard[i];
                if (entry == null) continue;
                PlayerJobData pjd = pdcStorage.getCache().get(entry);
                int lvl = -1;
                if (pjd != null) lvl = pjd.getJobStats(job.getId()).getLevel();
                // create or update the cached stats for this job
                leaderboardPlayerCache.computeIfAbsent(entry, PlayerLeaderboardData::new)
                        .getLeaderboardStats().put(job, new PlayerLeaderboardData.LeaderboardStats(i, lvl));
            }

            return leaderboard;
        }
        return null;
    }

    public int getPosition(UUID uuid, Job job) {
        PlayerLeaderboardData data = leaderboardPlayerCache.get(uuid);
        if (data == null) return -1;
        PlayerLeaderboardData.LeaderboardStats stats = data.getLeaderboardStats().get(job);
        if (stats == null) return -1;
        return stats.getPosition();
    }

    public int getLevel(UUID uuid, Job job) {
        PlayerLeaderboardData data = leaderboardPlayerCache.get(uuid);
        if (data == null) return -1;
        PlayerLeaderboardData.LeaderboardStats stats = data.getLeaderboardStats().get(job);
        if (stats == null) return -1;
        return stats.getLevel();
    }

    public PlayerLeaderboardData.LeaderboardStats getStats(UUID uuid, Job job) {
        PlayerLeaderboardData data = leaderboardPlayerCache.get(uuid);
        if (data == null) return null;
        return data.getLeaderboardStats().get(job);
    }

    public UUID getPlayerByPosition(int position, Job job) {
        if (position < 0) return null;
        UUID[] leaderboard = leaderboardJobCache.get(job);
        if (leaderboard == null || position >= leaderboard.length) return null;
        return leaderboard[position];
    }

    public void checkLeaderboardChange(UUID uuid, Job job, int level) {
        int position = getPosition(uuid, job);
        int oneHigher;
        UUID opponent;
        int calculateTop = plugin.getConfig().getInt("leaderboard.calculate_top", 100);
        if (position == -1) {
            // if a player is not in the leaderboard, get the last player in the leaderboard as an opponent
            oneHigher = calculateTop - 1;
            opponent = null;
            while (oneHigher >= 0) {
                opponent = getPlayerByPosition(oneHigher, job);
                if (opponent != null && !opponent.equals(uuid)) break;
                oneHigher--;
            }
        } else {

            leaderboardPlayerCache.get(uuid).getLeaderboardStats().get(job).setLevel(level);

            oneHigher = position - 1;
            if (oneHigher < 0) return;
            opponent = getPlayerByPosition(oneHigher, job);
        }

        if (opponent == null) return;

        if (opponent.equals(uuid)) return;

        int opponentLevel = leaderboardPlayerCache.get(opponent).getLeaderboardStats().get(job).getLevel();
        if (level > opponentLevel) {

            leaderboardPlayerCache.computeIfAbsent(uuid, PlayerLeaderboardData::new)
                    .getLeaderboardStats().computeIfAbsent(job, k -> new PlayerLeaderboardData.LeaderboardStats(-1, 0))
                    .setPosition(oneHigher);
            leaderboardPlayerCache.get(uuid).getLeaderboardStats().get(job).setLevel(level);

            leaderboardPlayerCache.get(opponent).getLeaderboardStats().get(job).setPosition(position);

            leaderboardJobCache.get(job)[oneHigher] = uuid;
            if (position != -1) leaderboardJobCache.get(job)[position] = opponent;

            // Prepare placeholders and messages
            OfflinePlayer opponentPlayer = Bukkit.getOfflinePlayer(opponent);
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);

            Set<TagResolver> placeholderSet = new HashSet<>();
            placeholderSet.add(Placeholder.component("opponent_displayname", opponentPlayer.isOnline() ? opponentPlayer.getPlayer().displayName() : Component.text(opponentPlayer.getName())));
            placeholderSet.add(Placeholder.unparsed("opponent_name", opponentPlayer.getName()));
            placeholderSet.add(Placeholder.component("displayname", player.isOnline() ? player.getPlayer().displayName() : Component.text(player.getName())));
            placeholderSet.add(Placeholder.unparsed("name", player.getName()));
            placeholderSet.add(Formatter.number("level", level));
            placeholderSet.add(Formatter.number("position", oneHigher+1));
            placeholderSet.add(Placeholder.component("job", job.getName()));
            placeholderSet.add(Placeholder.styling("primary", job.getName().children().getFirst().color()));
            placeholderSet.add(Placeholder.styling("secondary", job.getName().children().getLast().color()));

            TagResolver[] placeholders = placeholderSet.toArray(new TagResolver[]{});

            if (player.isOnline()) MessageUtil.send(player.getPlayer(), plugin.getConfigManager().getMessage("leaderboard_take_someones_position"), placeholders);
            if (opponentPlayer.isOnline()) MessageUtil.send(opponentPlayer.getPlayer(), plugin.getConfigManager().getMessage("leaderboard_your_position_taken"), placeholders);

            if (oneHigher == 0) {
                MessageUtil.send(plugin.getServer(), plugin.getConfigManager().getMessage("leaderboard_take_lead"), placeholders);
            } else if (oneHigher == 9) {
                MessageUtil.send(plugin.getServer(), plugin.getConfigManager().getMessage("leaderboard_get_in_top_10"), placeholders);
            }

            save();
        } else {
            //check if one position below opponent is in calculate list and is empty
            int belowOpponent = oneHigher + 1;
            if (belowOpponent < calculateTop && belowOpponent > 0) {
                UUID[] leaderboard = leaderboardJobCache.get(job);
                if (leaderboard[belowOpponent] == null) {
                    leaderboard[belowOpponent] = uuid;

                    leaderboardPlayerCache.computeIfAbsent(uuid, PlayerLeaderboardData::new)
                            .getLeaderboardStats().computeIfAbsent(job, k -> new PlayerLeaderboardData.LeaderboardStats(-1, 0))
                            .setPosition(belowOpponent);
                    leaderboardPlayerCache.get(uuid).getLeaderboardStats().get(job).setLevel(level);

                    save();
                }
            }
        }
    }
}
