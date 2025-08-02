package me.usainsrht.ujobs.gui;

import me.usainsrht.ujobs.UJobsPlugin;
import me.usainsrht.ujobs.models.Job;
import me.usainsrht.ujobs.models.PlayerJobData;
import me.usainsrht.ujobs.utils.ProgressBarUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MainJobGUI implements JobGUI {

    public Inventory inventory;

    public MainJobGUI(UJobsPlugin plugin, UUID uuid) {
        int rows = (int)Math.ceil(plugin.getJobManager().getJobs().size() / 7f) + 2;
        Component title = plugin.getMiniMessage().deserialize(plugin.getConfig().getString("gui.title"));
        this.inventory = Bukkit.createInventory(this, rows*9, title);

        String blankMaterial = plugin.getConfig().getString("gui.blank_material", null);
        if (blankMaterial != null && !blankMaterial.isEmpty() && !blankMaterial.equalsIgnoreCase("air")) {
            ItemStack blankItem = new ItemStack(Material.matchMaterial(blankMaterial));
            blankItem.editMeta(meta -> meta.setHideTooltip(true));
            for (int i = 0; i < inventory.getSize(); i++) {
                inventory.setItem(i, blankItem);
            }
        }

        PlayerJobData playerJobData;
        if (plugin.getStorage().isCached(uuid)) {
            playerJobData = plugin.getStorage().getCached(uuid);
            fill(plugin, playerJobData);
        } else {
            plugin.getStorage().load(uuid).thenAccept(data -> fill(plugin, data));
        }

    }

    public void fill(UJobsPlugin plugin, PlayerJobData playerJobData) {
        int i = 1;
        int baseSlot = 9;
        for (Job job : plugin.getJobManager().getJobs().values()) {

            if (i > 7) baseSlot = 11;
            if (i > 14) baseSlot = 13;

            String jobId = job.getId();
            PlayerJobData.JobStats jobStats = playerJobData.getJobStats(jobId);

            double exp = jobStats.getExp();
            int level = jobStats.getLevel();
            double nextExp = job.calculateExpForLevel(level);
            double totalMoney = jobStats.getTotalMoney();
            String progress = ProgressBarUtil.getProgressBar(exp, nextExp, 25, "", "|", "<color:dark_gray>", "|");
            TextColor primaryColor = job.getName().children().getFirst().color();
            TextColor secondaryColor = job.getName().children().getLast().color();

            Set<TagResolver> placeholderSet = new HashSet<>();
            placeholderSet.add(Placeholder.component("job", job.getName()));
            placeholderSet.add(Formatter.number("level", level));
            placeholderSet.add(Formatter.number("next_level", level+1));
            placeholderSet.add(Formatter.number("exp", exp));
            placeholderSet.add(Formatter.number("next_exp", nextExp));
            placeholderSet.add(Formatter.number("total_money", totalMoney));
            placeholderSet.add(Placeholder.parsed("progress", progress));
            placeholderSet.add(Placeholder.parsed("money_symbol", plugin.getConfig().getString("symbols.money")));
            placeholderSet.add(Placeholder.parsed("exp_symbol", plugin.getConfig().getString("symbols.exp")));
            placeholderSet.add(Placeholder.styling("primary", primaryColor));
            placeholderSet.add(Placeholder.styling("secondary", secondaryColor));
            int position = plugin.getLeaderboardManager().getPosition(playerJobData.getUuid(), job);
            String positionText = position == -1 ? plugin.getConfig().getString("leaderboard.calculate_top", "100")+"+" : String.valueOf(position);
            placeholderSet.add(Placeholder.unparsed("position", positionText));
            TagResolver[] placeholders = placeholderSet.toArray(new TagResolver[]{});

            ItemStack item = new ItemStack(job.getIcon());
            item.editMeta(meta -> {
                meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);

                Component name = plugin.getMiniMessage().deserialize(plugin.getConfig().getString("gui.jobitem.name"), placeholders)
                        .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
                meta.displayName(name);

                List<Component> lore = new ArrayList<>();
                plugin.getConfig().getStringList("gui.jobitem.lore").forEach(line -> {
                    Component component = plugin.getMiniMessage().deserialize(line, placeholders)
                            .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
                    lore.add(component);
                });
                meta.lore(lore);
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
