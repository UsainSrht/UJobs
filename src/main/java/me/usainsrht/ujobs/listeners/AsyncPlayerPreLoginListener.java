package me.usainsrht.ujobs.listeners;

import me.usainsrht.ujobs.UJobsPlugin;
import me.usainsrht.ujobs.storage.DatabaseStorage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.UUID;

public class AsyncPlayerPreLoginListener implements Listener {

    private final UJobsPlugin plugin;

    public AsyncPlayerPreLoginListener(UJobsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent e) {
        if (e.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }

        UUID uuid = e.getUniqueId();
        String name = e.getName();

        if (plugin.getLeaderboardManager() != null) {
            plugin.getLeaderboardManager().cachePlayerName(uuid, name);
        }

        // Asynchronously pre-load database data into cache before player joins
        if (plugin.getStorage() instanceof DatabaseStorage) {
            plugin.getStorage().load(uuid);
        }
    }
}
