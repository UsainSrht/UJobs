package me.usainsrht.ujobs.listeners.job_actions;

import me.usainsrht.ujobs.managers.JobManager;
import me.usainsrht.ujobs.models.BuiltInActions;
import me.usainsrht.ujobs.models.Job;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class CraftListener  implements Listener {

    JobManager jobManager;

    public CraftListener(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent e) {
        if (jobManager.getActionJobMap().containsKey(BuiltInActions.Material.CRAFT)) {

            Player player = (Player) e.getWhoClicked();
            ItemStack result = e.getRecipe().getResult();
            int amount;

            switch (e.getAction()) {
                case PICKUP_ALL, PICKUP_HALF, DROP_ONE_SLOT, DROP_ALL_SLOT -> amount = result.getAmount();
                case MOVE_TO_OTHER_INVENTORY -> {
                    if (getRemainingSpace(player.getInventory(), result.asOne()) > 0) {
                        int lowest = 127;
                        ItemStack[] matrix = e.getInventory().getMatrix();
                        for (ItemStack item : matrix) {
                            if (item != null && !item.isEmpty()) {
                                int count = item.getAmount();
                                if (count < lowest) {
                                    lowest = count;
                                }
                            }
                        }
                        amount = lowest * result.getAmount();
                    } else {
                        amount = 0;
                    }
                }
                default -> amount = 0;
            }

            if (amount <= 0) return;

            for (Job job : jobManager.getActionJobMap().get(BuiltInActions.Material.CRAFT)) {
                jobManager.processAction(player, BuiltInActions.Material.CRAFT, result.getType().name(), job, amount);
            }
        }
    }

    public static int getRemainingSpace(Inventory inventory, ItemStack item) {
        int space = 0;
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack == null || itemStack.isEmpty()) space += item.getMaxStackSize();
            else if (item.isSimilar(itemStack)) space += item.getMaxStackSize() - itemStack.getAmount();
        }
        return space;
    }
}
