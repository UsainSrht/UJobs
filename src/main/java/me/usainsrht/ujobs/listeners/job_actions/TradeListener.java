package me.usainsrht.ujobs.listeners.job_actions;

import io.papermc.paper.event.player.PlayerTradeEvent;
import me.usainsrht.ujobs.managers.JobManager;
import me.usainsrht.ujobs.models.BuiltInActions;
import me.usainsrht.ujobs.models.Job;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;

public class TradeListener implements Listener {

    JobManager jobManager;

    public TradeListener(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTrade(PlayerTradeEvent e) {
        Player player = e.getPlayer();

        MerchantRecipe trade = e.getTrade();

        if (jobManager.getActionJobMap().containsKey(BuiltInActions.Material.TRADE)) {
            String result = trade.getResult().getType().name();
            ItemStack adjusted1 = trade.getAdjustedIngredient1();
            String adjusted1Name = null;
            if (adjusted1 != null && !adjusted1.isEmpty()) {
                adjusted1Name = adjusted1.getType().name();
            }
            for (Job job : jobManager.getActionJobMap().get(BuiltInActions.Material.TRADE)) {
                jobManager.processAction(player, BuiltInActions.Material.TRADE, result, job, 1);
                jobManager.processAction(player, BuiltInActions.Material.TRADE, adjusted1Name, job, 1);
            }
        }


    }
}
