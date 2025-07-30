package me.usainsrht.ujobs.managers;

import me.usainsrht.ujobs.UJobsPlugin;
import me.usainsrht.ujobs.gui.MainJobGUI;
import org.bukkit.entity.Player;

public class JobGUIManager {

    UJobsPlugin plugin;

    public JobGUIManager(UJobsPlugin plugin) {
        this.plugin = plugin;
    }

    public void openJobGUI(Player player) {
        new MainJobGUI(plugin, player.getUniqueId()).open(player);
    }

}
