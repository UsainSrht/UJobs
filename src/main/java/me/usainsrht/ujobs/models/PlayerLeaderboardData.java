package me.usainsrht.ujobs.models;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class PlayerLeaderboardData {

    private final UUID uuid;
    private final Map<Job, LeaderboardStats> leaderboardStats;

    public PlayerLeaderboardData(UUID uuid) {
        this.uuid = uuid;
        this.leaderboardStats = new ConcurrentHashMap<>();
    }

    @Getter
    @Setter
    public static class LeaderboardStats {
        private int position;
        private int level;

        public LeaderboardStats(int position, int level) {
            this.position = position;
            this.level = level;
        }
    }

}
