package me.usainsrht.ujobs.listeners.job_actions;

import me.usainsrht.ujobs.managers.JobManager;
import me.usainsrht.ujobs.models.BuiltInActions;
import me.usainsrht.ujobs.models.Job;
import org.bukkit.Bukkit;
import org.bukkit.Raid;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.raid.RaidFinishEvent;
import org.bukkit.event.raid.RaidSpawnWaveEvent;

import java.util.UUID;

public class RaidListener implements Listener {

    JobManager jobManager;

    public RaidListener(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRaidFinish(RaidFinishEvent e) {
        Raid raid = e.getRaid();

        if (jobManager.getActionJobMap().containsKey(BuiltInActions.Special.RAID)) {
            for (Job job : jobManager.getActionJobMap().get(BuiltInActions.Special.RAID)) {
                for (UUID uuid : raid.getHeroes()) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null || !player.isOnline()) continue;
                    jobManager.processAction(player, BuiltInActions.Special.RAID, "win", job, 1);
                }
            }
        }

    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRaidWave(RaidSpawnWaveEvent e) {
        Raid raid = e.getRaid();

        if (jobManager.getActionJobMap().containsKey(BuiltInActions.Special.RAID)) {
            for (Job job : jobManager.getActionJobMap().get(BuiltInActions.Special.RAID)) {
                for (UUID uuid : raid.getHeroes()) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null || !player.isOnline()) continue;
                    jobManager.processAction(player, BuiltInActions.Special.RAID, "wave", job, 1);
                }
            }
        }

    }

}
