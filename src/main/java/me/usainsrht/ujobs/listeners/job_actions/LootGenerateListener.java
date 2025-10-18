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

        if (jobManager.shouldIgnore(player)) return;

        if (jobManager.getActionJobMap().containsKey(BuiltInActions.Special.GENERATE_LOOT)) {
            String lootTable = e.getLootTable().key().value().replace("chests/", "");
            for (Job job : jobManager.getJobsWithAction(BuiltInActions.Special.GENERATE_LOOT)) {
                jobManager.processAction(player, BuiltInActions.Special.GENERATE_LOOT, lootTable, job, 1);
            }
        }
    }
}
