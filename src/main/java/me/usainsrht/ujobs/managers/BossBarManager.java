package me.usainsrht.ujobs.managers;

import me.usainsrht.ujobs.UJobsPlugin;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BossBarManager {
    private final UJobsPlugin plugin;
    private final Map<UUID, Map<String, BossBarData>> playerBossBars;

    public BossBarManager(UJobsPlugin plugin) {
        this.plugin = plugin;
        this.playerBossBars = new ConcurrentHashMap<>();
    }
    //todo fix levelup and xp gain bossbars flickering whn both present

    public void showBossBar(Player player, String key, BossBar bossBar, int durationSeconds) {
        UUID playerId = player.getUniqueId();

        // Get or create player's boss bar map
        Map<String, BossBarData> bossBars = playerBossBars.computeIfAbsent(playerId, k -> new HashMap<>());

        // Remove existing boss bar with same key
        BossBarData existing = bossBars.get(key);
        if (existing != null) {
            hideBossBar(player, existing.bossBar);
            if (existing.task != null && !existing.task.isCancelled()) {
                existing.task.cancel();
            }
        }

        // Show new boss bar
        player.showBossBar(bossBar);

        // Create hide task
        BukkitTask hideTask = new BukkitRunnable() {
            @Override
            public void run() {
                hideBossBar(player, bossBar);
                bossBars.remove(key);
                if (bossBars.isEmpty()) {
                    playerBossBars.remove(playerId);
                }
            }
        }.runTaskLater(plugin, durationSeconds * 20L);

        // Store boss bar data
        bossBars.put(key, new BossBarData(bossBar, hideTask));
    }

    public void hideBossBar(Player player, BossBar bossBar) {
        player.hideBossBar(bossBar);
    }

    public void hideBossBar(Player player, String key) {
        UUID playerId = player.getUniqueId();
        Map<String, BossBarData> bossBars = playerBossBars.get(playerId);

        if (bossBars != null) {
            BossBarData bossBarData = bossBars.remove(key);
            if (bossBarData != null) {
                hideBossBar(player, bossBarData.bossBar);
                if (bossBarData.task != null && !bossBarData.task.isCancelled()) {
                    bossBarData.task.cancel();
                }
            }

            if (bossBars.isEmpty()) {
                playerBossBars.remove(playerId);
            }
        }
    }

    public void removePlayerBossBars(Player player) {
        UUID playerId = player.getUniqueId();
        Map<String, BossBarData> bossBars = playerBossBars.remove(playerId);

        if (bossBars != null) {
            for (BossBarData bossBarData : bossBars.values()) {
                hideBossBar(player, bossBarData.bossBar);
                if (bossBarData.task != null && !bossBarData.task.isCancelled()) {
                    bossBarData.task.cancel();
                }
            }
        }
    }

    public void removeAllBossBars() {
        for (UUID playerId : playerBossBars.keySet()) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null) {
                removePlayerBossBars(player);
            }
        }
        playerBossBars.clear();
    }

    public boolean hasBossBar(Player player, String key) {
        UUID playerId = player.getUniqueId();
        Map<String, BossBarData> bossBars = playerBossBars.get(playerId);
        return bossBars != null && bossBars.containsKey(key);
    }

    public void updateBossBar(Player player, String key, BossBar newBossBar) {
        UUID playerId = player.getUniqueId();
        Map<String, BossBarData> bossBars = playerBossBars.get(playerId);

        if (bossBars != null) {
            BossBarData existing = bossBars.get(key);
            if (existing != null) {
                // Hide old boss bar
                hideBossBar(player, existing.bossBar);

                // Show new boss bar
                player.showBossBar(newBossBar);

                // Update stored boss bar (keep same task)
                bossBars.put(key, new BossBarData(newBossBar, existing.task));
            }
        }
    }

    private static class BossBarData {
        final BossBar bossBar;
        final BukkitTask task;

        BossBarData(BossBar bossBar, BukkitTask task) {
            this.bossBar = bossBar;
            this.task = task;
        }
    }
}