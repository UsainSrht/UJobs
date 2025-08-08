package me.usainsrht.ujobs.listeners.job_actions;

import me.usainsrht.ujobs.managers.JobManager;
import me.usainsrht.ujobs.models.BuiltInActions;
import me.usainsrht.ujobs.models.Job;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.LootGenerateEvent;

public class LootGenerateListener implements Listener {

    JobManager jobManager;

    public LootGenerateListener(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLootGenerate(LootGenerateEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;

        if (jobManager.getActionJobMap().containsKey(BuiltInActions.Special.GENERATE_LOOT)) {
            for (Job job : jobManager.getActionJobMap().get(BuiltInActions.Special.GENERATE_LOOT)) {
                jobManager.processAction(player, BuiltInActions.Special.GENERATE_LOOT, e.getLootTable().key().value(), job, 1);
            }
        }
    }
}
