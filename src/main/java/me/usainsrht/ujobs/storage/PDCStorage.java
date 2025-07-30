package me.usainsrht.ujobs.storage;

import me.usainsrht.ujobs.UJobsPlugin;
import me.usainsrht.ujobs.models.PlayerJobData;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PDCStorage implements Storage {

    UJobsPlugin plugin;
    HashMap<UUID, PlayerJobData> cache;

    public static final NamespacedKey TAG_JOBS_DATA = NamespacedKey.fromString("ujobs");

    public static final NamespacedKey TAG_LEVEL = NamespacedKey.fromString("level");
    public static final NamespacedKey TAG_EXP = NamespacedKey.fromString("exp");
    public static final NamespacedKey TAG_TOTAL_MONEY = NamespacedKey.fromString("total_money");

    public PDCStorage(UJobsPlugin plugin) {
        this.plugin = plugin;
        cache = new HashMap<>();
    }

    public PersistentDataContainer serialize(PersistentDataAdapterContext context, PlayerJobData playerJobData) {
        PersistentDataContainer container = context.newPersistentDataContainer();

        for (Map.Entry<String, PlayerJobData.JobStats> entry : playerJobData.getJobStats().entrySet()) {
            PlayerJobData.JobStats jobStats = entry.getValue();
            NamespacedKey jobKey = NamespacedKey.fromString(entry.getKey(), plugin);
            PersistentDataContainer jobContainer = serialize(context, jobStats);
            container.set(jobKey, PersistentDataType.TAG_CONTAINER, jobContainer);
        }

        return container;
    }

    public PersistentDataContainer serialize(PersistentDataAdapterContext context, PlayerJobData.JobStats jobStats) {
        PersistentDataContainer container = context.newPersistentDataContainer();

        container.set(TAG_LEVEL, PersistentDataType.INTEGER, jobStats.getLevel());
        container.set(TAG_EXP, PersistentDataType.LONG, jobStats.getExp());
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
            playerJobData.setJobStats(jobKey.namespace(), jobStats);
        }

        return playerJobData;

    }

    public PlayerJobData.JobStats deserializeJobStats(PersistentDataContainer jobContainer) {
        int level = jobContainer.get(TAG_LEVEL, PersistentDataType.INTEGER);
        long exp = jobContainer.get(TAG_EXP, PersistentDataType.LONG);
        double totalMoney = jobContainer.get(TAG_TOTAL_MONEY, PersistentDataType.DOUBLE);

        return new PlayerJobData.JobStats(level, exp, totalMoney);
    }

    @Override
    public void save() {
        plugin.getLogger().info("Saving cache job data to PDC storage.");
        for (UUID uuid : cache.keySet()) {
            save(uuid);
        }
        plugin.getLogger().info("Saved cache job data to PDC storage.");
    }

    @Override
    public void save(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            player.getScheduler().run(plugin, task -> {
                //success
                PlayerJobData playerJobData = getCached(uuid);
                if (playerJobData == null) return;

                set(player, playerJobData);
            }, () -> {
                //fail
                plugin.getLogger().warning("Can't save data of " + uuid + "! storage: PDC");
            });
        } else {
            //todo implement offlineplayer data save
            //currently not needed
        }
    }

    @Override
    public CompletableFuture<PlayerJobData> load(UUID uuid) {
        CompletableFuture<PlayerJobData> future = new CompletableFuture<>();

        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            player.getScheduler().run(plugin, task -> {
                //success
                PersistentDataContainer pdc = player.getPersistentDataContainer();
                PlayerJobData playerJobData;
                if (pdc.has(TAG_JOBS_DATA)) {
                    playerJobData = deserialize(uuid, pdc.get(TAG_JOBS_DATA, PersistentDataType.TAG_CONTAINER));
                } else {
                    playerJobData = new PlayerJobData(uuid);
                }
                cache.put(uuid, playerJobData);
                future.complete(playerJobData);
            }, () -> {
                //fail
                plugin.getLogger().warning("Can't load data of " + uuid + "! storage: PDC");
                future.cancel(false);
            });
        } else {
            //todo implement offlineplayer data load
            //currently not needed
        }

        return future;
    }

    @Override
    public boolean isCached(UUID uuid) {
        return cache.containsKey(uuid);
    }

    @Override
    public @Nullable PlayerJobData getCached(UUID uuid) {
        return cache.getOrDefault(uuid, null);
    }

    @Override
    public void set(UUID uuid, PlayerJobData playerJobData) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) {

            return;
        }
        cache.put(uuid, playerJobData);
        set(player, playerJobData);
    }

    private void set(Player player, PlayerJobData playerJobData) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        pdc.set(TAG_JOBS_DATA, PersistentDataType.TAG_CONTAINER, serialize(pdc.getAdapterContext(), playerJobData));
    }

    @Override
    public void remove(UUID uuid) {
        removeFromCache(uuid);
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) return;
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        pdc.remove(TAG_JOBS_DATA);
    }

    @Override
    public void removeFromCache(UUID uuid) {
        cache.remove(uuid);
    }
    

}
