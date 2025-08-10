package me.usainsrht.ujobs.listeners.job_actions;

import me.usainsrht.ujobs.managers.JobManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class AnvilMergeListener implements Listener {

    JobManager jobManager;

    public AnvilMergeListener(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAnvilMerge(InventoryClickEvent e) {
        if (e.getInventory().getType() != org.bukkit.event.inventory.InventoryType.ANVIL) return;

        if (e.getSlot() != 2) return; // Check if the clicked slot is the output slot

        if (!(e.getWhoClicked() instanceof org.bukkit.entity.Player player)) return;

        //todo anvil merge success checks

    }

}
