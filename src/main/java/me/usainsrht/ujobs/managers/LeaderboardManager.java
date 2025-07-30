package me.usainsrht.ujobs.managers;

import com.mojang.brigadier.Message;
import me.usainsrht.ujobs.UJobsPlugin;
import me.usainsrht.ujobs.models.Job;
import me.usainsrht.ujobs.models.PlayerLeaderboardData;
import me.usainsrht.ujobs.utils.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

public class LeaderboardManager {

    UJobsPlugin plugin;
    Map<UUID, PlayerLeaderboardData> leaderboardPlayerCache;
    Map<Job, List<UUID>> leaderboardJobCache;

    public LeaderboardManager(UJobsPlugin plugin) {
        this.plugin = plugin;
        this.leaderboardPlayerCache = new HashMap<>();
        this.leaderboardJobCache = new HashMap<>();
    }

    public int getPosition(UUID uuid, Job job) {
        PlayerLeaderboardData data = leaderboardPlayerCache.get(uuid);
        if (data == null) return -1;
        return data.getLeaderboardStats().get(job).getPosition();
    }

    public UUID getPlayerByPosition(int position, Job job) {
        List<UUID> leaderboard = leaderboardJobCache.get(job);
        if (leaderboard == null || position < 0 || position >= leaderboard.size()) return null;
        return leaderboard.get(position);
    }

    public void checkLeaderboardChange(UUID uuid, Job job, int level) {
        YamlConfiguration leaderboardConfig = plugin.getConfigManager().getLeaderboardConfig();
        int position = getPosition(uuid, job);
        if (position == -1) return;

        int oneHigher = position - 1;
        UUID opponent = getPlayerByPosition(oneHigher, job);
        if (opponent == null) return;

        int opponentLevel = leaderboardPlayerCache.get(opponent).getLeaderboardStats().get(job).getLevel();
        if (level > opponentLevel) {

            // Prepare placeholders and messages
            OfflinePlayer opponentPlayer = Bukkit.getOfflinePlayer(opponent);
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);

            Set<TagResolver> placeholderSet = new HashSet<>();
            placeholderSet.add(Placeholder.component("opponent_displayname", opponentPlayer.isOnline() ? opponentPlayer.getPlayer().displayName() : Component.text(opponentPlayer.getName())));
            placeholderSet.add(Placeholder.unparsed("opponent_name", opponentPlayer.getName()));
            placeholderSet.add(Placeholder.component("displayname", player.isOnline() ? player.getPlayer().displayName() : Component.text(player.getName())));
            placeholderSet.add(Placeholder.unparsed("name", player.getName()));
            placeholderSet.add(Formatter.number("level", level));
            placeholderSet.add(Formatter.number("position", oneHigher));
            placeholderSet.add(Placeholder.component("job", job.getName()));

            // Colors
            String primaryColor = children != null && !children.isEmpty() ? plugin.getColor(children.get(0)) : "";
            String secondaryColor = children != null && !children.isEmpty() ? plugin.getColor(children.get(children.size() - 1)) : "";
            placeholderSet.add("primary", "<" + primaryColor + ">");
            placeholderSet.add("secondary", "<" + secondaryColor + ">");

            TagResolver[] placeholders = placeholderSet.toArray(new TagResolver[]{});

            String message = plugin.getJobsConfig().getString("messages.leaderboard.take_someones_position");
            Component component = plugin.deserializeMessage(message, placeholders);
            if (player.isOnline()) {
                ((Player) player).sendMessage(component);
                plugin.playSound((Player) player, plugin.getJobsConfig().getString("sounds.leaderboard.take_someones_position"));
            }

            if (opponentPlayer.isOnline()) {
                MessageUtil.send(opponentPlayer, plugin.getConfigManager().getMessage("leaderboard.your_position_taken"), placeholders);
            }

            if (oneHigher == 1) {
                String leadMsg = plugin.getJobsConfig().getString("messages.leaderboard.take_lead");
                isimEki = plugin.getSuffix(opponent, "den");
                opponentDisplayName = plugin.getDisplayName(opponentPlayer) + "'" + isimEki;
                placeholders.put("opponentdisplayname", opponentDisplayName);
                placeholders.put("opponentname", opponent + "'" + isimEki);
                Component leadComponent = plugin.deserializeMessage(leadMsg, placeholders)
                        .append(plugin.getTebrikButton(player));
                Bukkit.broadcast(leadComponent);
                if (opponentPlayer.isOnline()) {
                    plugin.playSound((Player) opponentPlayer, plugin.getJobsConfig().getString("sounds.leaderboard.take_lead"));
                }
            } else if (oneHigher == 10) {
                String top10Msg = plugin.getJobsConfig().getString("messages.leaderboard.get_in_top_10");
                Component top10Component = plugin.deserializeMessage(top10Msg, placeholders)
                        .append(plugin.getTebrikButton(player));
                Bukkit.broadcast(top10Component);
                if (opponentPlayer.isOnline()) {
                    plugin.playSound((Player) opponentPlayer, plugin.getJobsConfig().getString("sounds.leaderboard.get_in_top_10"));
                }
            }
        }
    }
}
