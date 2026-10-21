package me.usainsrht.ujobs.api.events;

import lombok.Getter;
import lombok.Setter;
import me.usainsrht.ujobs.models.Job;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class JobRegisterEvent extends JobEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Job job;

    private boolean cancelled;

    public JobRegisterEvent(Job job) {
        this(job, !Bukkit.isPrimaryThread());
    }

    public JobRegisterEvent(Job job, boolean isAsync) {
        super(isAsync);
        this.job = job;
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
