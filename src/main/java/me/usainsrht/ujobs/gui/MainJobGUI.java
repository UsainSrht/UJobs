package me.usainsrht.ujobs.gui;

import me.usainsrht.ujobs.UJobsPlugin;
import me.usainsrht.ujobs.models.Job;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class MainJobGUI implements JobGUI {

    public Inventory inventory;

    public MainJobGUI(UJobsPlugin plugin) {
        int rows = (int)Math.ceil(plugin.getJobManager().getJobs().size() / 7f) + 2;
        Component title = plugin.getMiniMessage().deserialize(plugin.getConfig().getString("gui.title"));
        this.inventory = Bukkit.createInventory(this, rows*9, title);

        String blankMaterial = plugin.getConfig().getString("gui.blank_material", null);
        if (blankMaterial != null && !blankMaterial.isEmpty() && !blankMaterial.equalsIgnoreCase("air")) {
            ItemStack blankItem = plugin.getServer().getItemFactory().createItemStack(blankMaterial);
            blankItem.editMeta(meta -> meta.setHideTooltip(true));
            for (int i = 0; i < inventory.getSize(); i++) {
                inventory.setItem(i, blankItem);
            }
        }

        int i = 1;
        int baseSlot = 9;
        for (Job job : plugin.getJobManager().getJobs().values()) {

            if (i > 7) baseSlot = 11;
            if (i > 14) baseSlot = 13;

            ItemStack item = new ItemStack(job.getIcon());
            item.editMeta(meta -> {
                meta.addItemFlags(ItemFlag.);
            });

            int slot = baseSlot + i;
            inventory.setItem(slot, item);

            i++;
        }
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        e.setCancelled(true);
    }

    @Override
    public void onBottomClick(InventoryClickEvent e) {

    }

    @Override
    public void onDrag(InventoryDragEvent e) {
        e.setCancelled(true);
    }

    @Override
    public void onOpen(InventoryOpenEvent e) {

    }

    @Override
    public void onClose(InventoryCloseEvent e) {

    }
}
