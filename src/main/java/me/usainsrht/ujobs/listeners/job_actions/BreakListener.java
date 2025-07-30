package me.usainsrht.ujobs.listeners.job_actions;

import me.usainsrht.ujobs.managers.JobManager;
import me.usainsrht.ujobs.models.BuiltInActions;
import me.usainsrht.ujobs.models.Job;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class BreakListener implements Listener {

    JobManager jobManager;

    public BreakListener(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        BlockData blockData = block.getBlockData();
        String value;
        if (blockData instanceof Ageable ageable) {
            value = block.getType().name() + ageable.getAge();
        } else value = block.getType().name();

        if (jobManager.getActionJobMap().containsKey(BuiltInActions.BREAK)) {
            for (Job job : jobManager.getActionJobMap().get(BuiltInActions.BREAK)) {
                jobManager.processAction(e.getPlayer(), BuiltInActions.BREAK, value, job, 1);
            }
        }

    }

}
