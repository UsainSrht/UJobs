package me.usainsrht.ujobs.listeners.job_actions.timber;

import me.usainsrht.ujobs.managers.JobManager;
import me.usainsrht.ujobs.models.BuiltInActions;
import me.usainsrht.ujobs.models.Job;
import me.usainsrht.utimber.event.TreeDestroyEvent;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashMap;

public class UTimberListener implements Listener {

    private final JobManager jobManager;

    public UTimberListener(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTreeDestroy(TreeDestroyEvent e) {
        Player player = e.getPlayer();
        if (jobManager.shouldIgnore(player)) {
            return;
        }

        // Aggregate amounts of log materials
        HashMap<Material, Integer> amounts = new HashMap<>();
        for (Block logBlock : e.getDetectedTree().logs) {
            Material material = logBlock.getType();
            amounts.put(material, amounts.getOrDefault(material, 0) + 1);
        }

        if (jobManager.getActionJobMap().containsKey(BuiltInActions.Material.BREAK)) {
            for (Job job : jobManager.getJobsWithAction(BuiltInActions.Material.BREAK)) {
                amounts.forEach((log, amount) -> {
                    jobManager.processAction(player, BuiltInActions.Material.BREAK, log.name(), job, amount);
                });
            }
        }
    }
}
