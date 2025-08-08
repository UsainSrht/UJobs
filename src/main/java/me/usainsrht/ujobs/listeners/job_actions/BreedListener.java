package me.usainsrht.ujobs.listeners.job_actions;

import me.usainsrht.ujobs.managers.JobManager;
import me.usainsrht.ujobs.models.BuiltInActions;
import me.usainsrht.ujobs.models.Job;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;

public class BreedListener implements Listener {

    JobManager jobManager;

    public BreedListener(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreed(EntityBreedEvent e) {
        if (!(e.getBreeder() instanceof Player player)) return;

        EntityType entityType = e.getEntityType();

        if (jobManager.getActionJobMap().containsKey(BuiltInActions.Entity.BREED)) {
            for (Job job : jobManager.getActionJobMap().get(BuiltInActions.Entity.BREED)) {
                jobManager.processAction(player, BuiltInActions.Entity.BREED, entityType.name(), job, 1);
            }
        }
    }
}
