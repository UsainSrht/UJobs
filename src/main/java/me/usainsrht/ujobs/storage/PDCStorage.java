package me.usainsrht.ujobs.storage;

import lombok.Getter;
import me.usainsrht.ujobs.UJobsPlugin;
import me.usainsrht.ujobs.models.PlayerJobData;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class PDCStorage implements Storage {

    private final UJobsPlugin plugin;
    private final Map<UUID, PlayerJobData> cache;

    public static final NamespacedKey TAG_JOBS_DATA = new NamespacedKey("ujobs", "jobs_data");

    public static final NamespacedKey TAG_LEVEL = new NamespacedKey("ujobs", "level");
    public static final NamespacedKey TAG_EXP = new NamespacedKey("ujobs", "exp");
    public static final NamespacedKey TAG_TOTAL_MONEY = new NamespacedKey("ujobs", "total_money");

    public PDCStorage(UJobsPlugin plugin) {
        this.plugin = plugin;
        cache = new ConcurrentHashMap<>();
    }

    public PersistentDataContainer serialize(PersistentDataAdapterContext context, PlayerJobData playerJobData) {
        PersistentDataContainer container = context.newPersistentDataContainer();

        for (Map.Entry<String, PlayerJobData.JobStats> entry : playerJobData.getJobStats().entrySet()) {
            PlayerJobData.JobStats jobStats = entry.getValue();
            if (jobStats.getExp() == 0 && jobStats.getLevel() == 0 && jobStats.getTotalMoney() == 0) {
                continue; // skip empty job stats
            }
            NamespacedKey jobKey = NamespacedKey.fromString(entry.getKey(), plugin);
            if (jobKey == null) {
                continue;
            }
            PersistentDataContainer jobContainer = serialize(context, jobStats);
            container.set(jobKey, PersistentDataType.TAG_CONTAINER, jobContainer);
        }

        return container;
    }

    public PersistentDataContainer serialize(PersistentDataAdapterContext context, PlayerJobData.JobStats jobStats) {
        PersistentDataContainer container = context.newPersistentDataContainer();

        container.set(TAG_LEVEL, PersistentDataType.INTEGER, jobStats.getLevel());
        container.set(TAG_EXP, PersistentDataType.DOUBLE, jobStats.getExp());
        container.set(TAG_TOTAL_MONEY, PersistentDataType.DOUBLE, jobStats.getTotalMoney());

        return container;
    }

    public PlayerJobData deserialize(UUID uuid, PersistentDataContainer pdc) {
        PlayerJobData playerJobData = new PlayerJobData(uuid);

        for (NamespacedKey jobKey : pdc.getKeys()) {
            if (!pdc.has(jobKey, PersistentDataType.TAG_CONTAINER)) {
                continue; // skip if not a job container
            }
            PersistentDataContainer jobContainer = pdc.get(jobKey, PersistentDataType.TAG_CONTAINER);
            PlayerJobData.JobStats jobStats = deserializeJobStats(jobContainer);
            playerJobData.setJobStats(jobKey.getKey(), jobStats);
        }

        return playerJobData;

    }

    public PlayerJobData.JobStats deserializeJobStats(PersistentDataContainer jobContainer) {
        Integer level = jobContainer.get(TAG_LEVEL, PersistentDataType.INTEGER);
        Double exp = jobContainer.get(TAG_EXP, PersistentDataType.DOUBLE);
        Double totalMoney = jobContainer.get(TAG_TOTAL_MONEY, PersistentDataType.DOUBLE);

        return new PlayerJobData.JobStats(
                level == null ? 0 : level,
                exp == null ? 0 : exp,
                totalMoney == null ? 0 : totalMoney
        );
    }

    @Override
    public void save() {
        //plugin.getLogger().info("Saving cache job data to PDC storage.");
        for (UUID uuid : cache.keySet()) {
            if (plugin.isEnabled()) {
                save(uuid);
            } else {
                saveNow(uuid);
            }
        }
        //plugin.getLogger().info("Saved cache job data to PDC storage.");
    }

    @Override
    public void save(UUID uuid) {
        if (uuid == null) {
            return;
        }
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            PlayerJobData playerJobData = getCached(uuid);
            if (playerJobData == null) return;
            if (!plugin.isEnabled()) {
                set(player, playerJobData);
                return;
            }
            plugin.getMorePaperLib().scheduling().entitySpecificScheduler(player).run(
                    () -> set(player, playerJobData),
                    () -> plugin.getLogger().warning("Can't save data of " + uuid + "! storage: PDC")
            );
        } else {
            //todo implement offlineplayer data save
            //currently not needed
        }
    }

    public void saveNow(Player player) {
        if (player == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        PlayerJobData playerJobData = getCached(uuid);
        if (playerJobData == null) {
            return;
        }
        saveNow(player, playerJobData);
    }

    public void saveNow(Player player, PlayerJobData playerJobData) {
        if (player == null || playerJobData == null) {
            return;
        }
        cache.put(player.getUniqueId(), playerJobData);
        set(player, playerJobData);
    }

    public void saveNow(UUID uuid) {
        if (uuid == null) {
            return;
        }
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) {
            return;
        }
        saveNow(player);
    }

    public void saveNowAll() {
        for (UUID uuid : cache.keySet()) {
            saveNow(uuid);
        }
    }

    @Override
    public CompletableFuture<PlayerJobData> load(UUID uuid) {
        CompletableFuture<PlayerJobData> future = new CompletableFuture<>();

        if (uuid == null) {
            future.complete(new PlayerJobData(new UUID(0L, 0L)));
            return future;
        }

        if (!plugin.isEnabled()) {
            PlayerJobData playerJobData = cache.computeIfAbsent(uuid, PlayerJobData::new);
            future.complete(playerJobData);
            return future;
        }

        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            plugin.getMorePaperLib().scheduling().entitySpecificScheduler(player).run(
                    () -> {
                        PersistentDataContainer pdc = player.getPersistentDataContainer();
                        PlayerJobData playerJobData;
                        if (pdc.has(TAG_JOBS_DATA, PersistentDataType.TAG_CONTAINER)) {
                            playerJobData = deserialize(uuid, pdc.get(TAG_JOBS_DATA, PersistentDataType.TAG_CONTAINER));
                        } else {
                            playerJobData = new PlayerJobData(uuid);
                        }
                        cache.put(uuid, playerJobData);
                        future.complete(playerJobData);
                    },
                    () -> {
                        plugin.getLogger().warning("Can't load data of " + uuid + "! storage: PDC");
                        future.complete(new PlayerJobData(uuid));
                    }
            );
        } else {
            PlayerJobData playerJobData = cache.computeIfAbsent(uuid, PlayerJobData::new);
            future.complete(playerJobData);
        }

        return future;
    }

    @Override
    public boolean isCached(UUID uuid) {
        return uuid != null && cache.containsKey(uuid);
    }

    @Override
    public @Nullable PlayerJobData getCached(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        return cache.get(uuid);
    }

    @Override
    public void set(UUID uuid, PlayerJobData playerJobData) {
        if (uuid == null || playerJobData == null) {
            return;
        }
        if (!plugin.isEnabled()) {
            cache.put(uuid, playerJobData);
            return;
        }
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) {
            return;
        }
        cache.put(uuid, playerJobData);
        plugin.getMorePaperLib().scheduling().entitySpecificScheduler(player).run(
                () -> set(player, playerJobData),
                () -> plugin.getLogger().warning("Can't set data of " + uuid + "! storage: PDC")
        );
    }

    private void set(Player player, PlayerJobData playerJobData) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        pdc.set(TAG_JOBS_DATA, PersistentDataType.TAG_CONTAINER, serialize(pdc.getAdapterContext(), playerJobData));
    }

    @Override
    public void remove(UUID uuid) {
        if (uuid == null) {
            return;
        }
        removeFromCache(uuid);
        if (!plugin.isEnabled()) {
            return;
        }
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) return;
        plugin.getMorePaperLib().scheduling().entitySpecificScheduler(player).run(
                () -> player.getPersistentDataContainer().remove(TAG_JOBS_DATA),
                () -> plugin.getLogger().warning("Can't remove data of " + uuid + "! storage: PDC")
        );
    }

    @Override
    public void removeFromCache(UUID uuid) {
        if (uuid == null) {
            return;
        }
        cache.remove(uuid);
    }

    @Override
    public Collection<PlayerJobData> getAllCachedData() {
        return cache.values();
    }

}
