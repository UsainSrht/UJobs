package me.usainsrht.ujobs.listeners;

import me.usainsrht.ujobs.gui.JobGUI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;

public class InventoryListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory inventory = event.getClickedInventory();
        if (inventory != null && inventory.getHolder() instanceof JobGUI) {
            ((JobGUI)inventory.getHolder()).onClick(event);
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (inventory == event.getView().getBottomInventory() && top != null && top.getHolder() instanceof JobGUI) {
            ((JobGUI)top.getHolder()).onBottomClick(event);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        InventoryView view = event.getView();
        Inventory topInventory = view.getTopInventory();

        if (topInventory == null) return;

        if (topInventory.getHolder() instanceof JobGUI && event.getRawSlots().stream().anyMatch(slot -> slot < topInventory.getSize())) {
            ((JobGUI)topInventory.getHolder()).onDrag(event);
            return;
        }

        if (topInventory.getHolder() instanceof JobGUI && event.getRawSlots().stream().anyMatch(slot -> slot >= topInventory.getSize())) {
            event.setCancelled(true);
            return;
        }

    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        if (inventory != null && inventory.getHolder() instanceof JobGUI) {
            ((JobGUI)inventory.getHolder()).onClose(event);
        }
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent event) {
        Inventory inventory = event.getInventory();
        if (inventory != null && inventory.getHolder() instanceof JobGUI) {
            ((JobGUI)inventory.getHolder()).onOpen(event);
        }
    }

}
