package me.usainsrht.ujobs.listeners.job_actions;

import me.usainsrht.ujobs.managers.JobManager;
import me.usainsrht.ujobs.models.BuiltInActions;
import me.usainsrht.ujobs.models.Job;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTameEvent;

public class TameListener implements Listener {

    JobManager jobManager;

    public TameListener(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTame(EntityTameEvent e) {
        if (!(e.getOwner() instanceof Player player)) return;

        LivingEntity entity = e.getEntity();

        if (jobManager.getActionJobMap().containsKey(BuiltInActions.Entity.TAME)) {
            for (Job job : jobManager.getJobsWithAction(BuiltInActions.Entity.TAME)) {
                jobManager.processAction(player, BuiltInActions.Entity.TAME, entity.getType().name(), job, 1);
            }
        }
    }
}
