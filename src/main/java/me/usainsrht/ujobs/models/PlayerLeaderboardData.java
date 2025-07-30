package me.usainsrht.ujobs.models;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
public class PlayerLeaderboardData {

    UUID uuid;
    Map<Job, LeaderboardStats> leaderboardStats;

    public PlayerLeaderboardData(UUID uuid) {
        this.uuid = uuid;
        this.leaderboardStats = new HashMap<>();
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
