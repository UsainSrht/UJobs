package me.usainsrht.ujobs.api.events;

import lombok.Getter;
import lombok.Setter;
import me.usainsrht.ujobs.models.Action;
import me.usainsrht.ujobs.models.Job;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public class PlayerJobExpGainEvent extends JobEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Job job;
    @Nullable
    private final Action action;
    @Nullable
    private final String actionValue;

    @Setter
    private double exp;
    @Setter
    private double money;
    @Setter
    private int amount;
    private boolean cancelled;

    public PlayerJobExpGainEvent(Player player, Job job, @Nullable Action action, @Nullable String actionValue,
                                 double exp, double money, int amount) {
        this(player, job, action, actionValue, exp, money, amount, !Bukkit.isPrimaryThread());
    }

    public PlayerJobExpGainEvent(Player player, Job job, @Nullable Action action, @Nullable String actionValue,
                                 double exp, double money, int amount, boolean isAsync) {
        super(isAsync);
        this.player = player;
        this.job = job;
        this.action = action;
        this.actionValue = actionValue;
        this.exp = exp;
        this.money = money;
        this.amount = amount;
        this.cancelled = false;
    }

    public double getTotalExp() {
        return exp * amount;
    }

    public double getTotalMoney() {
        return money * amount;
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
