package me.usainsrht.ujobs.listeners.job_actions;

import me.usainsrht.ujobs.managers.JobManager;
import me.usainsrht.ujobs.models.BuiltInActions;
import me.usainsrht.ujobs.models.Job;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class PlaceListener implements Listener {

    private final JobManager jobManager;

    public PlaceListener(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        Player player = e.getPlayer();
        if (jobManager.shouldIgnore(player)) return;

        Block block = e.getBlock();
        BlockData blockData = block.getBlockData();

        String value;
        if (blockData instanceof Ageable ageable) {
            value = block.getType().name() + ageable.getAge();
        } else {
            value = block.getType().name();
        }

        // Convert to lowercase to match config keys
        value = value.toLowerCase();

        if (jobManager.getActionJobMap().containsKey(BuiltInActions.Material.PLACE)) {
            for (Job job : jobManager.getJobsWithAction(BuiltInActions.Material.PLACE)) {
                jobManager.processAction(player, BuiltInActions.Material.PLACE, value, job, 1);
            }
        }
    }
}
