package me.usainsrht.ujobs.listeners;

import me.usainsrht.ujobs.UJobsPlugin;
import me.usainsrht.ujobs.models.PlayerJobData;
import me.usainsrht.ujobs.storage.DatabaseStorage;
import me.usainsrht.ujobs.storage.PDCStorage;
import me.usainsrht.ujobs.storage.Storage;
import me.usainsrht.ujobs.utils.MultiplierUtil;
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

        // before the early return below, so the entry never outlives the player
        MultiplierUtil.clearCache(uuid);

        Storage storage = plugin.getStorage();
        PlayerJobData playerJobData = storage.getCached(uuid);
        if (playerJobData == null) return;

        if (storage instanceof PDCStorage pdcStorage) {
            pdcStorage.saveNow(player, playerJobData);
        } else if (storage instanceof DatabaseStorage databaseStorage) {
            databaseStorage.saveNow(uuid);
        } else {
            storage.save(uuid);
        }

        storage.removeFromCache(uuid);
    }

}
