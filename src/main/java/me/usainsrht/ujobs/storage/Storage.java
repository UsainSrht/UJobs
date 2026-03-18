package me.usainsrht.ujobs.storage;

import me.usainsrht.ujobs.models.PlayerJobData;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface Storage {

    void save(UUID uuid);

    void save();

    CompletableFuture<PlayerJobData> load(UUID uuid);

    boolean isCached(UUID uuid);

    @Nullable PlayerJobData getCached(UUID uuid);

    void set(UUID uuid, PlayerJobData activeContract);

    void remove(UUID uuid);

    void removeFromCache(UUID uuid);

    Collection<PlayerJobData> getAllCachedData();

    default void close() {
        // optional lifecycle hook for storages that hold external resources
    }

}