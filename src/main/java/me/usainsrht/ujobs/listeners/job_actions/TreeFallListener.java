package me.usainsrht.ujobs.listeners.job_actions;

import com.songoda.ultimatetimber.events.TreeFallEvent;
import com.songoda.ultimatetimber.tree.TreeBlockSet;
import me.usainsrht.ujobs.managers.JobManager;
import me.usainsrht.ujobs.models.BuiltInActions;
import me.usainsrht.ujobs.models.Job;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashMap;

public class TreeFallListener implements Listener {

    JobManager jobManager;

    public TreeFallListener(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(TreeFallEvent e) {
        TreeBlockSet<Block> blockSet = e.getDetectedTree().getDetectedTreeBlocks();

        HashMap<Material, Integer> amounts = new HashMap<>();
        blockSet.getLogBlocks().forEach(treeBlock -> {
            Material material = treeBlock.getBlock().getType();
            amounts.put(material, amounts.getOrDefault(material, 0) + 1);
        });

        if (jobManager.getActionJobMap().containsKey(BuiltInActions.Material.BREAK)) {
            for (Job job : jobManager.getActionJobMap().get(BuiltInActions.Material.BREAK)) {
                amounts.forEach((log, amount) -> {
                    jobManager.processAction(e.getPlayer(), BuiltInActions.Material.BREAK, log.name(), job, amount);
                });
            }
        }

    }

}
