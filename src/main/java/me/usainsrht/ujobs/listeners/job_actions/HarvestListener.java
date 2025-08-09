package me.usainsrht.ujobs.listeners.job_actions;

import me.usainsrht.ujobs.managers.JobManager;
import me.usainsrht.ujobs.models.BuiltInActions;
import me.usainsrht.ujobs.models.Job;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerHarvestBlockEvent;
import org.bukkit.inventory.ItemStack;

public class HarvestListener implements Listener {

    JobManager jobManager;

    public HarvestListener(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(PlayerHarvestBlockEvent e) {
        Player player = e.getPlayer();

        if (jobManager.getActionJobMap().containsKey(BuiltInActions.Material.HARVEST)) {
            for (ItemStack itemStack : e.getItemsHarvested()) {
                String value = itemStack.getType().name();
                int amount = itemStack.getAmount();
                for (Job job : jobManager.getJobsWithAction(BuiltInActions.Material.HARVEST)) {
                    jobManager.processAction(player, BuiltInActions.Material.HARVEST, value, job, amount);
                }
            }

        }

    }

}
