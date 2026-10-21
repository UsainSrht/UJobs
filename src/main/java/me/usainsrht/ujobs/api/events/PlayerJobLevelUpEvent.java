package me.usainsrht.ujobs.api.events;

import lombok.Getter;
import lombok.Setter;
import me.usainsrht.ujobs.models.Job;
import me.usainsrht.ujobs.yaml.YamlMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Getter
public class PlayerJobLevelUpEvent extends JobEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Job job;
    private final int fromLevel;

    @Setter
    private int toLevel;

    @Setter
    private List<String> commands;

    @Setter
    @Nullable
    private YamlMessage message;

    private boolean cancelled;

    public PlayerJobLevelUpEvent(Player player, Job job, int fromLevel, int toLevel,
                                 @Nullable List<String> commands, @Nullable YamlMessage message) {
        this(player, job, fromLevel, toLevel, commands, message, !Bukkit.isPrimaryThread());
    }

    public PlayerJobLevelUpEvent(Player player, Job job, int fromLevel, int toLevel,
                                 @Nullable List<String> commands, @Nullable YamlMessage message, boolean isAsync) {
        super(isAsync);
        this.player = player;
        this.job = job;
        this.fromLevel = fromLevel;
        this.toLevel = toLevel;
        this.commands = commands != null ? new ArrayList<>(commands) : new ArrayList<>();
        this.message = message;
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
