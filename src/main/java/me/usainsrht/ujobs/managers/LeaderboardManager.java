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
        int top = plugin.getConfig().getInt("leaderboard.calculate_top", 100);
        for (Job job : plugin.getJobManager().getJobs().values()) {
            leaderboardJobCache.put(job, new UUID[top]);
        }

        ConfigurationSection leaderboardSection = yml.getConfigurationSection("leaderboard");
        if (leaderboardSection == null) return;
        leaderboardSection.getKeys(false).forEach(jobId -> {
            Job job = plugin.getJobManager().getJobs().get(jobId);
            if (job == null) return;

            ConfigurationSection jobSection = yml.getConfigurationSection("leaderboard." + jobId);
            if (jobSection == null) return;

            List<PlayerLeaderboardData.LeaderboardStats> entries = new ArrayList<>();
            Map<PlayerLeaderboardData.LeaderboardStats, UUID> statsToUuid = new HashMap<>();

            jobSection.getKeys(false).forEach(uuidString -> {
                try {
                    UUID uuid = UUID.fromString(uuidString);
                    int position = jobSection.getInt(uuidString + ".position", -1);
                    int level = jobSection.getInt(uuidString + ".level", -1);

                    if (position >= 0 && level >= 0) {
                        PlayerLeaderboardData playerData = leaderboardPlayerCache.computeIfAbsent(uuid, PlayerLeaderboardData::new);
                        PlayerLeaderboardData.LeaderboardStats stats = new PlayerLeaderboardData.LeaderboardStats(position, level);
                        playerData.getLeaderboardStats().put(job, stats);
                        
                        entries.add(stats);
                        statsToUuid.put(stats, uuid);
                    }
                } catch (IllegalArgumentException ignored) {}
            });

            // Sort to ensure consistency
            entries.sort((s1, s2) -> {
                int c = Integer.compare(s2.getLevel(), s1.getLevel());
                if (c != 0) return c;
                return Integer.compare(s1.getPosition(), s2.getPosition());
            });

            UUID[] leaderboard = leaderboardJobCache.get(job);
            for (int i = 0; i < entries.size() && i < leaderboard.length; i++) {
                PlayerLeaderboardData.LeaderboardStats stats = entries.get(i);
                UUID uuid = statsToUuid.get(stats);
                leaderboard[i] = uuid;
                stats.setPosition(i);
            }
        });
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
            int top = plugin.getConfig().getInt("leaderboard.calculate_top", 100);
            UUID[] leaderboard = new UUID[top];

            List<PlayerJobData> players = new ArrayList<>(pdcStorage.getCache().values());
            players.removeIf(p -> p.getJobStats(job.getId()).getLevel() <= 0);
            players.sort((p1, p2) -> Integer.compare(p2.getJobStats(job.getId()).getLevel(), p1.getJobStats(job.getId()).getLevel()));

            for (int i = 0; i < players.size() && i < top; i++) {
                PlayerJobData p = players.get(i);
                leaderboard[i] = p.getUuid();
                leaderboardPlayerCache.computeIfAbsent(p.getUuid(), PlayerLeaderboardData::new)
                        .getLeaderboardStats().put(job, new PlayerLeaderboardData.LeaderboardStats(i, p.getJobStats(job.getId()).getLevel()));
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
        UUID[] leaderboard = leaderboardJobCache.get(job);
        if (leaderboard == null) return;

        // 1. Find current position
        int currentPos = -1;
        for (int i = 0; i < leaderboard.length; i++) {
            if (uuid.equals(leaderboard[i])) {
                currentPos = i;
                break;
            }
        }

        // 2. Remove from array (shift up)
        if (currentPos != -1) {
            for (int i = currentPos; i < leaderboard.length - 1; i++) {
                leaderboard[i] = leaderboard[i + 1];
                if (leaderboard[i] != null) {
                    updatePosition(leaderboard[i], job, i);
                }
            }
            leaderboard[leaderboard.length - 1] = null;
        }

        // 3. Update level in cache (or create if new)
        PlayerLeaderboardData pData = leaderboardPlayerCache.computeIfAbsent(uuid, PlayerLeaderboardData::new);
        PlayerLeaderboardData.LeaderboardStats stats = pData.getLeaderboardStats().computeIfAbsent(job, k -> new PlayerLeaderboardData.LeaderboardStats(-1, 0));
        stats.setLevel(level);

        // 4. Find new position
        int newPos = 0;
        while (newPos < leaderboard.length) {
            UUID other = leaderboard[newPos];
            if (other == null) break;
            if (level > getLevel(other, job)) break;
            newPos++;
        }

        // 5. Check if qualified
        if (newPos >= leaderboard.length) {
            // Didn't make it. Remove from cache if it was there.
            if (currentPos != -1) {
                pData.getLeaderboardStats().remove(job);
                if (pData.getLeaderboardStats().isEmpty()) {
                    leaderboardPlayerCache.remove(uuid);
                }
            }
            save();
            return;
        }

        // 6. Insert (shift down)
        // Handle the one falling off
        UUID fallen = leaderboard[leaderboard.length - 1];
        if (fallen != null) {
            PlayerLeaderboardData fData = leaderboardPlayerCache.get(fallen);
            if (fData != null) {
                fData.getLeaderboardStats().remove(job);
                if (fData.getLeaderboardStats().isEmpty()) {
                    leaderboardPlayerCache.remove(fallen);
                }
            }
        }

        for (int i = leaderboard.length - 1; i > newPos; i--) {
            leaderboard[i] = leaderboard[i - 1];
            if (leaderboard[i] != null) {
                updatePosition(leaderboard[i], job, i);
            }
        }
        leaderboard[newPos] = uuid;
        stats.setPosition(newPos);

        // 7. Messages
        if (currentPos == -1 || newPos < currentPos) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            Set<TagResolver> placeholderSet = new HashSet<>();
            placeholderSet.add(Placeholder.component("displayname", player.isOnline() ? player.getPlayer().displayName() : Component.text(player.getName())));
            placeholderSet.add(Placeholder.unparsed("name", player.getName()));
            placeholderSet.add(Formatter.number("level", level));
            placeholderSet.add(Formatter.number("position", newPos + 1));
            placeholderSet.add(Placeholder.component("job", job.getName()));
            try {
                placeholderSet.add(Placeholder.styling("primary", job.getName().children().getFirst().color()));
                placeholderSet.add(Placeholder.styling("secondary", job.getName().children().getLast().color()));
            } catch (Exception ignored) {}

            UUID opponent = null;
            if (newPos + 1 < leaderboard.length) {
                opponent = leaderboard[newPos + 1];
            }

            if (opponent != null) {
                OfflinePlayer opponentPlayer = Bukkit.getOfflinePlayer(opponent);
                try {
                    placeholderSet.add(Placeholder.component("opponent_displayname", opponentPlayer.isOnline() ? opponentPlayer.getPlayer().displayName() : Component.text(opponentPlayer.getName())));
                    placeholderSet.add(Placeholder.unparsed("opponent_name", opponentPlayer.getName()));
                } catch (Exception ignored) {}

                TagResolver[] placeholders = placeholderSet.toArray(new TagResolver[0]);

                if (player.isOnline()) MessageUtil.send(player.getPlayer(), plugin.getConfigManager().getMessage("leaderboard_take_someones_position"), placeholders);
                if (opponentPlayer.isOnline()) MessageUtil.send(opponentPlayer.getPlayer(), plugin.getConfigManager().getMessage("leaderboard_your_position_taken"), placeholders);
            }

            TagResolver[] placeholders = placeholderSet.toArray(new TagResolver[0]);

            if (newPos == 0) {
                MessageUtil.send(plugin.getServer(), plugin.getConfigManager().getMessage("leaderboard_take_lead"), placeholders);
            } else if (newPos == 9) {
                MessageUtil.send(plugin.getServer(), plugin.getConfigManager().getMessage("leaderboard_get_in_top_10"), placeholders);
            }
        }

        save();
    }

    private void updatePosition(UUID uuid, Job job, int position) {
        PlayerLeaderboardData data = leaderboardPlayerCache.get(uuid);
        if (data != null) {
            PlayerLeaderboardData.LeaderboardStats stats = data.getLeaderboardStats().get(job);
            if (stats != null) {
                stats.setPosition(position);
            }
        }
    }
}
