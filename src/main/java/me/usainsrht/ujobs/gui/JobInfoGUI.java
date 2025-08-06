package me.usainsrht.ujobs.gui;

import me.usainsrht.ujobs.UJobsPlugin;
import me.usainsrht.ujobs.models.Job;
import me.usainsrht.ujobs.models.JobInfoLine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class JobInfoGUI implements JobGUI {

    private final UJobsPlugin plugin;
    public Inventory inventory;
    public UUID uuid;

    public static final NamespacedKey minKey = new NamespacedKey("ujobs", "expinfo_min");
    public static final NamespacedKey jobKey = new NamespacedKey("ujobs", "job_id");

    public JobInfoGUI(UJobsPlugin plugin) {
        this.plugin = plugin;

        int rows = (int)Math.ceil(plugin.getJobManager().getJobs().size() / 7f) + 2;
        Component title = plugin.getMiniMessage().deserialize(plugin.getConfig().getString("expinfo.gui.title"));
        this.inventory = Bukkit.createInventory(this, rows*9, title);

        String blankMaterial = plugin.getConfig().getString("expinfo.gui.blank_material", null);
        if (blankMaterial != null && !blankMaterial.isEmpty() && !blankMaterial.equalsIgnoreCase("air")) {
            ItemStack blankItem = new ItemStack(Material.matchMaterial(blankMaterial));
            blankItem.editMeta(meta -> meta.setHideTooltip(true));
            for (int i = 0; i < inventory.getSize(); i++) {
                inventory.setItem(i, blankItem);
            }
        }

        addNavigationButtons(rows);

        fill();
    }

    private void addNavigationButtons(int rows) {
        ItemStack leftButton = new ItemStack(Material.matchMaterial(plugin.getConfig().getString("gui.navigation.left.material", "ARROW")));
        leftButton.editMeta(meta -> {
            if (plugin.getConfig().isString("gui.navigation.left.name")) {
                Component name = plugin.getMiniMessage().deserialize(plugin.getConfig().getString("gui.navigation.left.name"))
                        .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
                meta.displayName(name);
            }
            if (plugin.getConfig().isList("gui.navigation.left.lore")) {
                List<Component> lore = new ArrayList<>();
                plugin.getConfig().getStringList("gui.navigation.left.lore").forEach(line -> {
                    Component component = plugin.getMiniMessage().deserialize(line)
                            .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
                    lore.add(component);
                });
                meta.lore(lore);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        });
        inventory.setItem((rows * 9) - 9, leftButton);

        ItemStack rightButton = new ItemStack(Material.matchMaterial(plugin.getConfig().getString("gui.navigation.right.material", "ARROW")));
        rightButton.editMeta(meta -> {
            if (plugin.getConfig().isString("gui.navigation.right.name")) {
                Component name = plugin.getMiniMessage().deserialize(plugin.getConfig().getString("gui.navigation.right.name"))
                        .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
                meta.displayName(name);
            }
            if (plugin.getConfig().isList("gui.navigation.right.lore")) {
                List<Component> lore = new ArrayList<>();
                plugin.getConfig().getStringList("gui.navigation.right.lore").forEach(line -> {
                    Component component = plugin.getMiniMessage().deserialize(line)
                            .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
                    lore.add(component);
                });
                meta.lore(lore);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        });
        inventory.setItem((rows * 9) - 1, rightButton);
    }

    public void fill() {
        int i = 1;
        int baseSlot = 9;

        for (Job job : plugin.getJobManager().getJobs().values()) {
            if (i > 7) baseSlot = 11;
            if (i > 14) baseSlot = 13;

            ItemStack expInfoItem = createJobExpInfoItem(job, 1);
            int slot = baseSlot + i;
            inventory.setItem(slot, expInfoItem);

            i++;
        }
    }

    private ItemStack createJobExpInfoItem(Job job, int min) {
        String jobId = job.getId();
        int listAmount = plugin.getConfig().getInt("expinfo.list_in_lore", 10);

        int maxTop = job.getInfoLines().size();
        int minMax = Math.max(1, maxTop - listAmount);

        // Check bounds of min
        if (min < 1) {
            min = 1;
        } else if (min > minMax) {
            min = minMax;
        }

        // Create item stack
        ItemStack itemStack = new ItemStack(job.getIcon());
        ItemMeta meta = itemStack.getItemMeta();
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        // Store min value in PDC for click handling
        meta.getPersistentDataContainer().set(minKey, PersistentDataType.INTEGER, min);
        meta.getPersistentDataContainer().set(jobKey, PersistentDataType.STRING, jobId);

        Set<TagResolver> placeholderSet = new HashSet<>();

        // Job placeholders
        placeholderSet.add(Placeholder.component("job", job.getName()));

        // Color placeholders
        placeholderSet.add(Placeholder.styling("primary", job.getName().children().getFirst().color()));
        placeholderSet.add(Placeholder.styling("secondary", job.getName().children().getLast().color()));

        // Symbol placeholders
        placeholderSet.add(Placeholder.parsed("symbol_money", plugin.getConfig().getString("symbols.money")));
        placeholderSet.add(Placeholder.parsed("symbol_exp", plugin.getConfig().getString("symbols.exp")));

        // exp info placeholders
        for (int s = 1; s <= listAmount; s++) {
            int r = (s + min) - 2;

            if (job.getInfoLines().size() <= r) {;
                placeholderSet.add(Placeholder.parsed(s + "_action_value", "?"));
                placeholderSet.add(Formatter.number(s + "_money", 0.0));
                placeholderSet.add(Formatter.number(s + "_exp", 0.0));
                continue; // Skip if out of bounds
            }

            JobInfoLine line = job.getInfoLines().get(r);

            double exp = line.getReward().getExp();
            double money = line.getReward().getMoney();

            placeholderSet.add(Placeholder.component(s + "_action_value", line.getActionValue()));
            placeholderSet.add(Formatter.number(s + "_money", money));
            placeholderSet.add(Formatter.number(s + "_exp", exp));
        }

        // GUI viewer placeholders
        placeholderSet.add(Placeholder.parsed("min", String.valueOf(min)));
        placeholderSet.add(Placeholder.parsed("max", String.valueOf((min + listAmount) - 1)));

        TagResolver[] placeholders = placeholderSet.toArray(new TagResolver[0]);

        // Set display name
        String displayNameConfig = plugin.getConfig().getString("expinfo.gui.jobitem.name");
        Component componentDisplayName = plugin.getMiniMessage().deserialize(displayNameConfig, placeholders)
                .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
        meta.displayName(componentDisplayName);

        // Set lore
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty()); // Empty line at start

        plugin.getConfig().getStringList("expinfo.gui.jobitem.lore").forEach(line -> {
            Component component = plugin.getMiniMessage().deserialize(line, placeholders)
                    .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
            lore.add(component);
        });

        lore.add(Component.empty()); // Empty line at end
        meta.lore(lore);

        itemStack.setItemMeta(meta);
        return itemStack;
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

        if (!(e.getWhoClicked() instanceof Player player)) return;

        ItemStack clickedItem = e.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        // Handle navigation buttons
        int rows = inventory.getSize() / 9;
        int leftButtonSlot = (rows * 9) - 9;
        int rightButtonSlot = (rows * 9) - 1;

        if (e.getSlot() == leftButtonSlot) {
            // Open leaderboard GUI
            new LeaderboardGUI(plugin, player.getUniqueId()).open(player);
            return;
        }

        if (e.getSlot() == rightButtonSlot) {
            // Open main jobs gui
            new MainJobGUI(plugin, player.getUniqueId()).open(player);
            return;
        }

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(minKey, PersistentDataType.INTEGER) || !meta.getPersistentDataContainer().has(jobKey, PersistentDataType.STRING)) {
            return;
        }

        int currentMin = meta.getPersistentDataContainer().get(minKey, PersistentDataType.INTEGER);
        int newMin = currentMin;

        ClickType clickType = e.getClick();
        switch (clickType) {
            case LEFT:
                newMin = currentMin + 1;
                break;
            case RIGHT:
                newMin = currentMin - 1;
                break;
            case SHIFT_LEFT:
                newMin = currentMin + 5;
                break;
            case SHIFT_RIGHT:
                newMin = currentMin - 5;
                break;
            default:
                return;
        }

        // Find which job this item represents
        String jobStr = meta.getPersistentDataContainer().get(jobKey, PersistentDataType.STRING);
        Job job = plugin.getJobManager().getJobs().get(jobStr);
        ItemStack newItem = createJobExpInfoItem(job, newMin);
        inventory.setItem(e.getSlot(), newItem);
    }

    @Override
    public void onBottomClick(InventoryClickEvent e) {
        // Handle clicks in player inventory if needed
    }

    @Override
    public void onDrag(InventoryDragEvent e) {
        e.setCancelled(true);
    }

    @Override
    public void onOpen(InventoryOpenEvent e) {
        // Handle GUI open if needed
    }

    @Override
    public void onClose(InventoryCloseEvent e) {
        // Handle GUI close if needed
    }

}
