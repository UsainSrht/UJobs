package me.usainsrht.ujobs.gui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.InventoryHolder;

public interface JobGUI extends InventoryHolder {

    void onClick(InventoryClickEvent e);

    void onBottomClick(InventoryClickEvent e);

    void onDrag(InventoryDragEvent e);

    void onOpen(InventoryOpenEvent e);

    void onClose(InventoryCloseEvent e);

}
