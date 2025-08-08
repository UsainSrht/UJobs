package me.usainsrht.ujobs.listeners.job_actions;

import me.usainsrht.ujobs.managers.JobManager;
import me.usainsrht.ujobs.models.BuiltInActions;
import me.usainsrht.ujobs.models.Job;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;

public class EnchantListener  implements Listener {

    JobManager jobManager;

    public EnchantListener(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent e) {
        Player player = e.getEnchanter();

        String value = String.valueOf(e.getExpLevelCost());

        if (jobManager.getActionJobMap().containsKey(BuiltInActions.Special.ENCHANT)) {
            for (Job job : jobManager.getActionJobMap().get(BuiltInActions.Special.ENCHANT)) {
                jobManager.processAction(player, BuiltInActions.Special.ENCHANT, value, job, 1);
            }
        }
    }
}
