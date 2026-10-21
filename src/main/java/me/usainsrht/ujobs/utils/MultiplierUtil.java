package me.usainsrht.ujobs.utils;

import me.usainsrht.ujobs.models.Job;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves the money multipliers that apply to a player.
 * <p>
 * There are two of them and they stack: the per level one configured in jobs.yml, and a permission
 * one where the value is part of the node itself, e.g. {@code ujobs.multiplier.1.5} for x1.5.
 * When a player has several of those nodes the highest wins, so inheriting a lower rank's node
 * never adds on top of the higher one.
 */
public class MultiplierUtil {

    public static final String PERMISSION_PREFIX = "ujobs.multiplier.";

    /**
     * How long a scanned permission multiplier stays valid. Scanning walks every effective
     * permission of the player, which is too heavy to redo on every block break, and permission
     * plugins can change nodes at runtime without firing an event we could listen to.
     */
    private static final long CACHE_TTL_MS = 3000L;

    private static final Map<UUID, CachedMultiplier> CACHE = new ConcurrentHashMap<>();

    private record CachedMultiplier(double value, long expiresAt) {
    }

    /**
     * Both multipliers combined, which is what the player actually earns per action.
     */
    public static double getTotalMultiplier(Player player, Job job, int level) {
        if (job == null) return getPermissionMultiplier(player);
        return job.getIncomeMultiplier(level) * getPermissionMultiplier(player);
    }

    /**
     * Highest {@code ujobs.multiplier.<value>} node the player has, or 1.0 when they have none.
     * Values below 1.0 are ignored, so a permission can only ever boost income.
     */
    public static double getPermissionMultiplier(Player player) {
        if (player == null) return 1.0;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        CachedMultiplier cached = CACHE.get(uuid);
        if (cached != null && cached.expiresAt() > now) {
            return cached.value();
        }

        double scanned = scanPermissionMultiplier(player);
        CACHE.put(uuid, new CachedMultiplier(scanned, now + CACHE_TTL_MS));
        return scanned;
    }

    private static double scanPermissionMultiplier(Player player) {
        double highest = 1.0;

        try {
            for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
                // effective permissions include explicitly denied nodes too
                if (!info.getValue()) continue;

                String permission = info.getPermission();
                if (permission == null || permission.length() <= PERMISSION_PREFIX.length()) continue;
                if (!permission.regionMatches(true, 0, PERMISSION_PREFIX, 0, PERMISSION_PREFIX.length())) continue;

                try {
                    double value = Double.parseDouble(permission.substring(PERMISSION_PREFIX.length()));
                    if (value > highest) highest = value;
                } catch (NumberFormatException ignored) {
                    // not a numeric node, e.g. ujobs.multiplier.* from a wildcard grant
                }
            }
        } catch (ConcurrentModificationException e) {
            // a permission plugin edited this player's nodes while we walked them. keep whatever we
            // found so far, the next scan is at most CACHE_TTL_MS away and will see the new state.
            return highest;
        }

        return highest;
    }

    public static void clearCache(UUID uuid) {
        if (uuid != null) CACHE.remove(uuid);
    }

    public static void clearCache() {
        CACHE.clear();
    }

}
