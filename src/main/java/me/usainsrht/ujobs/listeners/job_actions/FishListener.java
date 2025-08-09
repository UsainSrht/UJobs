package me.usainsrht.ujobs.listeners.job_actions;

import me.usainsrht.ujobs.managers.JobManager;
import me.usainsrht.ujobs.models.BuiltInActions;
import me.usainsrht.ujobs.models.Job;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.player.PlayerFishEvent;

public class FishListener implements Listener {

    JobManager jobManager;

    public FishListener(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent e) {
        Player player = e.getPlayer();

        if (e.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;

        if (!(e.getCaught() instanceof Item item)) return;
        String name = item.getItemStack().getType().name();

        if (jobManager.getActionJobMap().containsKey(BuiltInActions.Material.FISH)) {
            for (Job job : jobManager.getJobsWithAction(BuiltInActions.Material.FISH)) {
                jobManager.processAction(player, BuiltInActions.Material.FISH, name, job, 1);
            }
        }
    }
}
