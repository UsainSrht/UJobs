package me.usainsrht.ujobs.gui;

import me.usainsrht.ujobs.UJobsPlugin;
import me.usainsrht.ujobs.models.Job;
import me.usainsrht.ujobs.models.PlayerJobData;
import me.usainsrht.ujobs.models.PlayerLeaderboardData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class LeaderboardGUI implements JobGUI {

    private final UJobsPlugin plugin;
    private final UUID viewerUuid;
    public Inventory inventory;
    public static final NamespacedKey minKey = new NamespacedKey("ujobs", "leaderboard_min");
    public static final NamespacedKey jobKey = new NamespacedKey("ujobs", "job_id");

    public LeaderboardGUI(UJobsPlugin plugin, UUID uuid) {
        this.plugin = plugin;
        this.viewerUuid = uuid;

        int rows = (int)Math.ceil(plugin.getJobManager().getJobs().size() / 7f) + 2;
        Component title = plugin.getMiniMessage().deserialize(plugin.getConfig().getString("leaderboard.gui.title"));
        this.inventory = Bukkit.createInventory(this, rows*9, title);

        // Fill with blank items
        String blankMaterial = plugin.getConfig().getString("leaderboard.gui.blank_material", null);
        if (blankMaterial != null && !blankMaterial.isEmpty() && !blankMaterial.equalsIgnoreCase("air")) {
            ItemStack blankItem = new ItemStack(Material.matchMaterial(blankMaterial));
            blankItem.editMeta(meta -> meta.setHideTooltip(true));
            for (int i = 0; i < inventory.getSize(); i++) {
                inventory.setItem(i, blankItem);
            }
        }

        // Add navigation buttons
        addNavigationButtons(rows);

        // Fill with job items
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

            ItemStack leaderboardItem = createJobLeaderboardItem(job, 1);
            int slot = baseSlot + i;
            inventory.setItem(slot, leaderboardItem);

            i++;
        }
    }

    private ItemStack createJobLeaderboardItem(Job job, int min) {
        String jobId = job.getId();
        int listAmount = plugin.getConfig().getInt("leaderboard.list_in_lore", 10);
        int maxTop = plugin.getConfig().getInt("leaderboard.calculate_top", 100);

        // Check bounds of min
        if (min < 1) {
            min = 1;
        } else if (min > (maxTop - listAmount)) {
            min = maxTop - listAmount;
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

        // Time placeholder (placeholder implementation)
        placeholderSet.add(Placeholder.unparsed("time", "1h"));

        // Top list placeholders
        UUID[] topPlayers = plugin.getLeaderboardManager().getLeaderboardJobCache().get(job);
        for (int s = 1; s <= listAmount; s++) {
            int r = (s + min) - 2;

            String playerName = "?";
            String level = "?";

            if (topPlayers != null && r < topPlayers.length && topPlayers[r] != null) {
                UUID playerUuid = topPlayers[r];
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUuid);
                playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : "?";

                PlayerLeaderboardData leaderboardData = plugin.getLeaderboardManager().getLeaderboardPlayerCache().get(playerUuid);
                if (leaderboardData != null && leaderboardData.getLeaderboardStats().containsKey(job)) {
                    level = String.valueOf(leaderboardData.getLeaderboardStats().get(job).getLevel());
                }
            }

            placeholderSet.add(Formatter.number(s + "_position", r+1));
            placeholderSet.add(Placeholder.unparsed(s + "_displayname", playerName));
            placeholderSet.add(Placeholder.unparsed(s + "_level", level));
        }

        // GUI viewer placeholders
        placeholderSet.add(Placeholder.parsed("min", String.valueOf(min)));
        placeholderSet.add(Placeholder.parsed("max", String.valueOf((min + listAmount) - 1)));

        // Viewer's position and stats
        PlayerLeaderboardData viewerLeaderboardData = plugin.getLeaderboardManager().getLeaderboardPlayerCache().get(viewerUuid);
        String viewerPosition = "+" + maxTop;
        String viewerLevel = "0";
        String viewerExp = "0";

        if (viewerLeaderboardData != null && viewerLeaderboardData.getLeaderboardStats().containsKey(job)) {
            int position = viewerLeaderboardData.getLeaderboardStats().get(job).getPosition();
            viewerPosition = position == -1 ? "+" + maxTop : String.valueOf(position);
            viewerLevel = String.valueOf(viewerLeaderboardData.getLeaderboardStats().get(job).getLevel());
        }

        // Get viewer's current exp from PlayerJobData if needed
        if (plugin.getStorage().isCached(viewerUuid)) {
            PlayerJobData playerJobData = plugin.getStorage().getCached(viewerUuid);
            if (playerJobData != null) {
                viewerExp = String.valueOf(playerJobData.getJobStats(jobId).getExp());
                viewerLevel = String.valueOf(playerJobData.getJobStats(jobId).getLevel());
            }
        }

        placeholderSet.add(Placeholder.unparsed("position", viewerPosition));
        placeholderSet.add(Placeholder.unparsed("level", viewerLevel));
        placeholderSet.add(Placeholder.unparsed("exp", viewerExp));

        TagResolver[] placeholders = placeholderSet.toArray(new TagResolver[0]);

        // Set display name
        String displayNameConfig = plugin.getConfig().getString("leaderboard.gui.jobitem.name");
        Component componentDisplayName = plugin.getMiniMessage().deserialize(displayNameConfig, placeholders)
                .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
        meta.displayName(componentDisplayName);

        // Set lore
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty()); // Empty line at start

        plugin.getConfig().getStringList("leaderboard.gui.jobitem.lore").forEach(line -> {
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

        if (!(e.getWhoClicked() instanceof Player)) return;
        Player player = (Player) e.getWhoClicked();

        ItemStack clickedItem = e.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        // Handle navigation buttons
        int rows = inventory.getSize() / 9;
        int leftButtonSlot = (rows * 9) - 9;
        int rightButtonSlot = (rows * 9) - 1;

        if (e.getSlot() == leftButtonSlot) {
            // Open main jobs GUI
            new MainJobGUI(plugin, player.getUniqueId()).open(player);
            return;
        }

        if (e.getSlot() == rightButtonSlot) {
            // Open exp information GUI
            new JobInfoGUI(plugin).open(player);
            return;
        }

        // Handle job leaderboard item clicks
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
        ItemStack newItem = createJobLeaderboardItem(job, newMin);
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