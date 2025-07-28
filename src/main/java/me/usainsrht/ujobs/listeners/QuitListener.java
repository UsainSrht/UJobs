package me.usainsrht.ujobs.listeners;

import me.usainsrht.ujobs.UJobsPlugin;
import me.usainsrht.ujobs.models.PlayerJobData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class QuitListener implements Listener {

    UJobsPlugin plugin;

    public QuitListener(UJobsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();
        PlayerJobData playerJobData = plugin.getStorage().getCached(uuid);
        if (playerJobData == null) return;
        plugin.getStorage().save(uuid);
        plugin.getStorage().removeFromCache(uuid);
    }

}
