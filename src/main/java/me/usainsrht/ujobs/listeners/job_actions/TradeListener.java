package me.usainsrht.ujobs.listeners.job_actions;

import io.papermc.paper.event.player.PlayerTradeEvent;
import me.usainsrht.ujobs.managers.JobManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class TradeListener implements Listener {

    JobManager jobManager;

    public TradeListener(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTrade(PlayerTradeEvent e) {

    }
}
