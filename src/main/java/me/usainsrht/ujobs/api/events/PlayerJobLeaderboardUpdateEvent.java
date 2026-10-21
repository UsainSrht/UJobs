package me.usainsrht.ujobs.api.events;

import lombok.Getter;
import lombok.Setter;
import me.usainsrht.ujobs.models.Job;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Getter
public class PlayerJobLeaderboardUpdateEvent extends JobEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerUuid;
    private final Job job;
    private final int oldPosition;
    private final int newPosition;
    private final int level;

    private boolean cancelled;

    public PlayerJobLeaderboardUpdateEvent(UUID playerUuid, Job job, int oldPosition, int newPosition, int level) {
        this(playerUuid, job, oldPosition, newPosition, level, !Bukkit.isPrimaryThread());
    }

    public PlayerJobLeaderboardUpdateEvent(UUID playerUuid, Job job, int oldPosition, int newPosition, int level, boolean isAsync) {
        super(isAsync);
        this.playerUuid = playerUuid;
        this.job = job;
        this.oldPosition = oldPosition;
        this.newPosition = newPosition;
        this.level = level;
        this.cancelled = false;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
