package me.usainsrht.ujobs.managers;

import lombok.Getter;
import me.usainsrht.ujobs.UJobsPlugin;
import me.usainsrht.ujobs.models.Job;
import me.usainsrht.ujobs.models.PlayerJobData;
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

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Getter
public class LeaderboardManager {

    private final UJobsPlugin plugin;
    private final Map<UUID, PlayerLeaderboardData> leaderboardPlayerCache;
    private final Map<Job, UUID[]> leaderboardJobCache;
    private final Object leaderboardLock = new Object();
    private final ExecutorService fileWriteExecutor = Executors.newSingleThreadExecutor();

    public LeaderboardManager(UJobsPlugin plugin) {
        this.plugin = plugin;
        this.leaderboardPlayerCache = new ConcurrentHashMap<>();
        this.leaderboardJobCache = new ConcurrentHashMap<>();
    }

    public void load(ConfigurationSection yml) {
        synchronized (leaderboardLock) {
            leaderboardPlayerCache.clear();
            leaderboardJobCache.clear();

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
    }

    public void save() {
        String yamlString;
        synchronized (leaderboardLock) {
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

            yamlString = leaderboardConfig.saveToString();
        }

        fileWriteExecutor.submit(() -> {
            try {
                File file = new File(plugin.getDataFolder(), "leaderboard.yml");
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                java.nio.file.Files.writeString(file.toPath(), yamlString);
            } catch (Exception e) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to save leaderboard.yml asynchronously", e);
            }
        });
    }

    public void shutdown() {
        fileWriteExecutor.shutdown();
        try {
            if (!fileWriteExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                fileWriteExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            fileWriteExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public UUID[] createLeaderboard(Job job) {
        synchronized (leaderboardLock) {
            int top = plugin.getConfig().getInt("leaderboard.calculate_top", 100);
            UUID[] leaderboard = new UUID[top];

            List<PlayerJobData> players = new ArrayList<>(plugin.getStorage().getAllCachedData());
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
    }

    public int getPosition(UUID uuid, Job job) {
        synchronized (leaderboardLock) {
            PlayerLeaderboardData data = leaderboardPlayerCache.get(uuid);
            if (data == null) return -1;
            PlayerLeaderboardData.LeaderboardStats stats = data.getLeaderboardStats().get(job);
            if (stats == null) return -1;
            return stats.getPosition();
        }
    }

    public int getLevel(UUID uuid, Job job) {
        synchronized (leaderboardLock) {
            PlayerLeaderboardData data = leaderboardPlayerCache.get(uuid);
            if (data == null) return -1;
            PlayerLeaderboardData.LeaderboardStats stats = data.getLeaderboardStats().get(job);
            if (stats == null) return -1;
            return stats.getLevel();
        }
    }

    public PlayerLeaderboardData.LeaderboardStats getStats(UUID uuid, Job job) {
        synchronized (leaderboardLock) {
            PlayerLeaderboardData data = leaderboardPlayerCache.get(uuid);
            if (data == null) return null;
            return data.getLeaderboardStats().get(job);
        }
    }

    public UUID getPlayerByPosition(int position, Job job) {
        synchronized (leaderboardLock) {
            if (position < 0) return null;
            UUID[] leaderboard = leaderboardJobCache.get(job);
            if (leaderboard == null || position >= leaderboard.length) return null;
            return leaderboard[position];
        }
    }

    public UUID[] getTopPlayersSnapshot(Job job) {
        synchronized (leaderboardLock) {
            UUID[] leaderboard = leaderboardJobCache.get(job);
            if (leaderboard == null) {
                return new UUID[0];
            }
            return Arrays.copyOf(leaderboard, leaderboard.length);
        }
    }

    public PlayerLeaderboardData getPlayerData(UUID uuid) {
        synchronized (leaderboardLock) {
            if (uuid == null) {
                return null;
            }
            return leaderboardPlayerCache.get(uuid);
        }
    }

    public void checkLeaderboardChange(UUID uuid, Job job, int level) {
        synchronized (leaderboardLock) {
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

                boolean parsedOpponentDisplayName = false;
                UUID opponent = null;
                if (newPos + 1 < leaderboard.length) {
                    opponent = leaderboard[newPos + 1];
                }

                OfflinePlayer opponentPlayer = null;
                if (opponent != null) {
                    opponentPlayer = Bukkit.getOfflinePlayer(opponent);
                    String opponentName = opponentPlayer.getName();
                    if (opponentName != null) {
                        try {
                            Component opponentDisplayName = opponentPlayer.isOnline() ? opponentPlayer.getPlayer().displayName() : Component.text(opponentName);
                            placeholderSet.add(Placeholder.component("opponent_displayname", opponentDisplayName));
                            placeholderSet.add(Placeholder.unparsed("opponent_name", opponentName));
                            parsedOpponentDisplayName = true;
                        } catch (Exception ignored) {}
                    }
                }

                TagResolver[] placeholders = placeholderSet.toArray(new TagResolver[0]);

                if (opponentPlayer != null) {
                    if (player.isOnline() && parsedOpponentDisplayName) {
                        MessageUtil.send(player.getPlayer(), plugin.getConfigManager().getMessage("leaderboard_take_someones_position"), placeholders);
                    }
                    if (opponentPlayer.isOnline()) {
                        MessageUtil.send(opponentPlayer.getPlayer(), plugin.getConfigManager().getMessage("leaderboard_your_position_taken"), placeholders);
                    }
                }

                if (newPos == 0 && opponent != null && parsedOpponentDisplayName) {
                    MessageUtil.send(plugin.getServer(), plugin.getConfigManager().getMessage("leaderboard_take_lead"), placeholders);
                } else if (newPos == 9) {
                    MessageUtil.send(plugin.getServer(), plugin.getConfigManager().getMessage("leaderboard_get_in_top_10"), placeholders);
                }
            }

            save();
        }
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

    public void validateAndFixLeaderboard() {
        plugin.getLogger().info("Leaderboard validation: Starting validation task asynchronously...");
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                Map<Job, Set<UUID>> jobToUuids = new HashMap<>();
                boolean isDatabase = plugin.getStorage() instanceof me.usainsrht.ujobs.storage.DatabaseStorage;

                // Phase 1: Gather UUIDs under synchronization
                synchronized (leaderboardLock) {
                    for (Job job : plugin.getJobManager().getJobs().values()) {
                        UUID[] currentLeaderboard = leaderboardJobCache.get(job);
                        if (currentLeaderboard == null) continue;

                        Set<UUID> uuids = new LinkedHashSet<>();
                        for (UUID uuid : currentLeaderboard) {
                            if (uuid != null) {
                                uuids.add(uuid);
                            }
                        }
                        leaderboardPlayerCache.forEach((uuid, pData) -> {
                            if (pData.getLeaderboardStats().containsKey(job)) {
                                uuids.add(uuid);
                            }
                        });
                        jobToUuids.put(job, uuids);
                    }
                }

                // Phase 2: Asynchronous Validation (No Lock)
                // We resolve names and database levels without holding the lock.
                Map<UUID, String> uuidToName = new HashMap<>();
                Map<Job, Map<UUID, Integer>> jobToValidatedLevels = new HashMap<>();

                for (Map.Entry<Job, Set<UUID>> entry : jobToUuids.entrySet()) {
                    Job job = entry.getKey();
                    Map<UUID, Integer> validatedLevels = new HashMap<>();
                    jobToValidatedLevels.put(job, validatedLevels);

                    for (UUID uuid : entry.getValue()) {
                        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                        String name = offlinePlayer.getName();
                        if (name == null) {
                            continue; // Will be removed in Phase 3
                        }
                        uuidToName.put(uuid, name);

                        int level = -1;
                        if (isDatabase) {
                            try {
                                PlayerJobData jobData = plugin.getStorage().load(uuid).join();
                                if (jobData != null && jobData.getJobStats().containsKey(job.getId())) {
                                    level = jobData.getJobStats(job.getId()).getLevel();
                                }
                            } catch (Exception e) {
                                plugin.getLogger().log(java.util.logging.Level.WARNING, "Failed to load player data from database during validation for UUID: " + uuid, e);
                            }
                        } else {
                            // Trust the current cache level (will be read under lock in Phase 3)
                            level = -2;
                        }
                        validatedLevels.put(uuid, level);
                    }
                }

                // Phase 3: Apply Updates under synchronization
                synchronized (leaderboardLock) {
                    boolean modified = false;

                    for (Job job : plugin.getJobManager().getJobs().values()) {
                        UUID[] currentLeaderboard = leaderboardJobCache.get(job);
                        if (currentLeaderboard == null) continue;

                        Set<UUID> uuids = jobToUuids.getOrDefault(job, Collections.emptySet());
                        Map<UUID, Integer> validatedLevels = jobToValidatedLevels.getOrDefault(job, Collections.emptyMap());

                        List<ValidPlayerEntry> validEntries = new ArrayList<>();

                        for (UUID uuid : uuids) {
                            String name = uuidToName.get(uuid);
                            if (name == null) {
                                plugin.getLogger().warning("Leaderboard validation: Removed player with unknown name/never joined (UUID: " + uuid + ") from job: " + job.getId());
                                modified = true;
                                continue;
                            }

                            int level = -1;
                            if (isDatabase) {
                                int dbLevel = validatedLevels.getOrDefault(uuid, -1);
                                // If the player's level on the server has been updated in the meantime, use the larger/current one
                                int currentServerLevel = getLevel(uuid, job);
                                level = Math.max(dbLevel, currentServerLevel);

                                if (level <= 0) {
                                    plugin.getLogger().warning("Leaderboard validation: Removed player " + name + " (UUID: " + uuid + ") from job: " + job.getId() + " because their level is " + level);
                                    modified = true;
                                    continue;
                                }
                            } else {
                                // Trust cache level if PDC
                                PlayerLeaderboardData pData = leaderboardPlayerCache.get(uuid);
                                if (pData != null && pData.getLeaderboardStats().containsKey(job)) {
                                    level = pData.getLeaderboardStats().get(job).getLevel();
                                }
                                if (level <= 0) {
                                    modified = true;
                                    continue;
                                }
                            }

                            validEntries.add(new ValidPlayerEntry(uuid, level));
                        }

                        // Sort entries by level (descending)
                        validEntries.sort((e1, e2) -> {
                            int c = Integer.compare(e2.level, e1.level);
                            if (c != 0) return c;
                            return e1.uuid.compareTo(e2.uuid); // Stable ordering
                        });

                        // Compare with current leaderboard to see if it changed
                        int top = plugin.getConfig().getInt("leaderboard.calculate_top", 100);
                        UUID[] newLeaderboard = new UUID[top];
                        boolean changed = false;

                        for (int i = 0; i < top; i++) {
                            UUID newUuid = (i < validEntries.size()) ? validEntries.get(i).uuid : null;
                            UUID oldUuid = currentLeaderboard[i];
                            if (!Objects.equals(newUuid, oldUuid)) {
                                changed = true;
                            }
                            newLeaderboard[i] = newUuid;
                        }

                        // Also check if any levels in cache changed
                        for (int i = 0; i < validEntries.size() && i < top; i++) {
                            ValidPlayerEntry entry = validEntries.get(i);
                            PlayerLeaderboardData pData = leaderboardPlayerCache.get(entry.uuid);
                            if (pData != null) {
                                PlayerLeaderboardData.LeaderboardStats stats = pData.getLeaderboardStats().get(job);
                                if (stats == null || stats.getLevel() != entry.level || stats.getPosition() != i) {
                                    changed = true;
                                }
                            } else {
                                changed = true;
                            }
                        }

                        if (changed) {
                            modified = true;

                            // Clear current entries for this job in cache
                            leaderboardPlayerCache.forEach((uuid, pData) -> {
                                pData.getLeaderboardStats().remove(job);
                            });

                            // Rebuild cache for this job
                            for (int i = 0; i < validEntries.size() && i < top; i++) {
                                ValidPlayerEntry entry = validEntries.get(i);
                                leaderboardPlayerCache.computeIfAbsent(entry.uuid, PlayerLeaderboardData::new)
                                        .getLeaderboardStats().put(job, new PlayerLeaderboardData.LeaderboardStats(i, entry.level));
                            }

                            leaderboardJobCache.put(job, newLeaderboard);
                        }
                    }

                    // Cleanup leaderboardPlayerCache for any entries with no stats left
                    leaderboardPlayerCache.entrySet().removeIf(entry -> entry.getValue().getLeaderboardStats().isEmpty());

                    if (modified) {
                        plugin.getLogger().info("Leaderboard validation: Inconsistencies fixed, saving updated leaderboard...");
                        save();
                    } else {
                        plugin.getLogger().info("Leaderboard validation: All leaderboards are valid.");
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE, "An error occurred during async leaderboard validation", e);
            }
        });
    }

    private static class ValidPlayerEntry {
        final UUID uuid;
        final int level;

        ValidPlayerEntry(UUID uuid, int level) {
            this.uuid = uuid;
            this.level = level;
        }
    }
}
