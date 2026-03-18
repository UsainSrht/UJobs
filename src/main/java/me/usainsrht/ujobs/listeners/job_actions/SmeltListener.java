package me.usainsrht.ujobs.listeners.job_actions;

import me.usainsrht.ujobs.managers.JobManager;
import me.usainsrht.ujobs.models.BuiltInActions;
import me.usainsrht.ujobs.models.Job;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceExtractEvent;

public class SmeltListener implements Listener {

    private final JobManager jobManager;

    public SmeltListener(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSmelt(FurnaceExtractEvent e) {
        Player player = e.getPlayer();
        if (jobManager.shouldIgnore(player)) return;

        if (!jobManager.getActionJobMap().containsKey(BuiltInActions.Material.SMELT)) {
            return;
        }

        String value = e.getItemType().name();
        int amount = e.getItemAmount();
        if (amount <= 0) {
            return;
        }

        for (Job job : jobManager.getJobsWithAction(BuiltInActions.Material.SMELT)) {
            jobManager.processAction(player, BuiltInActions.Material.SMELT, value, job, amount);
        }
    }

}
