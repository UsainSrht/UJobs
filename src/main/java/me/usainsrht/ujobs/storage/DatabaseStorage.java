package me.usainsrht.ujobs.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.usainsrht.ujobs.UJobsPlugin;
import me.usainsrht.ujobs.models.PlayerJobData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class DatabaseStorage implements Storage {

    private static final String TABLE_NAME = "ujobs_player_jobs";

    private final UJobsPlugin plugin;
    private final HikariDataSource dataSource;
    private final Map<UUID, PlayerJobData> cache;
    private final PDCStorage pdcStorage;
    private final boolean pdcFallback;

    public DatabaseStorage(UJobsPlugin plugin, String storageType) {
        this.plugin = plugin;
        this.cache = new ConcurrentHashMap<>();
        this.pdcStorage = new PDCStorage(plugin);
        this.pdcFallback = plugin.getConfig().getBoolean("storage.pdc_fallback", true);
        this.dataSource = createDataSource(storageType);
        initializeSchema();
    }

    @Override
    public void save(UUID uuid) {
        if (uuid == null) {
            return;
        }
        PlayerJobData playerJobData = cache.get(uuid);
        if (playerJobData == null) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                persistPlayerData(uuid, playerJobData);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to save player data to database for " + uuid, e);
            }
        });

        if (pdcFallback) {
            pdcStorage.set(uuid, playerJobData);
        }
    }

    public void saveNow(UUID uuid) {
        if (uuid == null) {
            return;
        }

        PlayerJobData playerJobData = cache.get(uuid);
        if (playerJobData == null) {
            return;
        }

        try {
            persistPlayerData(uuid, playerJobData);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to synchronously save player data to database for " + uuid, e);
        }

        if (pdcFallback) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                pdcStorage.saveNow(player, playerJobData);
            }
        }
    }

    @Override
    public void save() {
        for (Map.Entry<UUID, PlayerJobData> entry : cache.entrySet()) {
            try {
                persistPlayerData(entry.getKey(), entry.getValue());
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to save player data to database for " + entry.getKey(), e);
            }
        }

        if (pdcFallback) {
            pdcStorage.save();
        }
    }

    @Override
    public CompletableFuture<PlayerJobData> load(UUID uuid) {
        if (uuid == null) {
            return CompletableFuture.completedFuture(new PlayerJobData(new UUID(0L, 0L)));
        }

        PlayerJobData cached = cache.get(uuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        CompletableFuture<PlayerJobData> future = new CompletableFuture<>();

        CompletableFuture.supplyAsync(() -> {
            try {
                PlayerJobData playerJobData = loadPlayerDataFromDatabase(uuid);

                if (playerJobData.getJobStats().isEmpty() && pdcFallback) {
                    PlayerJobData fallback = loadFromPdc(uuid);
                    if (!fallback.getJobStats().isEmpty()) {
                        try {
                            persistPlayerData(uuid, fallback);
                            playerJobData = fallback;
                        } catch (SQLException e) {
                            plugin.getLogger().log(Level.WARNING, "Failed to migrate PDC data to database for " + uuid, e);
                        }
                    }
                }

                cache.put(uuid, playerJobData);
                return playerJobData;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load player data from database for " + uuid, e);
                PlayerJobData fallback = pdcFallback ? loadFromPdc(uuid) : new PlayerJobData(uuid);
                cache.put(uuid, fallback);
                return fallback;
            }
        }).thenAccept(data -> completeLoadFuture(uuid, future, data));

        return future;
    }

    @Override
    public boolean isCached(UUID uuid) {
        return uuid != null && cache.containsKey(uuid);
    }

    @Override
    public @Nullable PlayerJobData getCached(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        return cache.get(uuid);
    }

    @Override
    public void set(UUID uuid, PlayerJobData playerJobData) {
        if (uuid == null || playerJobData == null) {
            return;
        }
        cache.put(uuid, playerJobData);
        save(uuid);
    }

    @Override
    public void remove(UUID uuid) {
        if (uuid == null) {
            return;
        }
        removeFromCache(uuid);

        CompletableFuture.runAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement delete = connection.prepareStatement("DELETE FROM " + TABLE_NAME + " WHERE uuid = ?")) {
                delete.setString(1, uuid.toString());
                delete.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to remove player data from database for " + uuid, e);
            }
        });

        if (pdcFallback) {
            pdcStorage.remove(uuid);
        }
    }

    @Override
    public void removeFromCache(UUID uuid) {
        if (uuid == null) {
            return;
        }
        cache.remove(uuid);
        if (pdcFallback) {
            pdcStorage.removeFromCache(uuid);
        }
    }

    @Override
    public Collection<PlayerJobData> getAllCachedData() {
        return cache.values();
    }

    @Override
    public void close() {
        save();
        dataSource.close();
    }

    private HikariDataSource createDataSource(String storageType) {
        ConfigurationSection poolSection = plugin.getConfig().getConfigurationSection("storage.pool");

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("UJobsPool");

        if (poolSection != null) {
            hikariConfig.setMaximumPoolSize(poolSection.getInt("maximum_pool_size", 10));
            hikariConfig.setMinimumIdle(poolSection.getInt("minimum_idle", 2));
            hikariConfig.setConnectionTimeout(poolSection.getLong("connection_timeout_ms", 10000L));
            hikariConfig.setMaxLifetime(poolSection.getLong("max_lifetime_ms", 1800000L));
            hikariConfig.setIdleTimeout(poolSection.getLong("idle_timeout_ms", 600000L));
            hikariConfig.setKeepaliveTime(poolSection.getLong("keepalive_time_ms", 0L));
        }

        switch (storageType.toLowerCase()) {
            case "mysql" -> configureMysql(hikariConfig);
            case "postgresql" -> configurePostgresql(hikariConfig);
            case "sqlite" -> configureSqlite(hikariConfig);
            default -> throw new IllegalArgumentException("Unsupported database type: " + storageType);
        }

        return new HikariDataSource(hikariConfig);
    }

    private void configureSqlite(HikariConfig hikariConfig) {
        String relativeFile = plugin.getConfig().getString("storage.sqlite.file", "storage/ujobs.db");
        File dbFile = new File(plugin.getDataFolder(), relativeFile);
        File parent = dbFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            plugin.getLogger().warning("Failed to create sqlite folder: " + parent.getAbsolutePath());
        }

        hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        hikariConfig.setDriverClassName("org.sqlite.JDBC");
    }

    private void configureMysql(HikariConfig hikariConfig) {
        ConfigurationSection mysql = plugin.getConfig().getConfigurationSection("storage.mysql");
        if (mysql == null) {
            throw new IllegalStateException("Missing storage.mysql configuration section");
        }

        String host = mysql.getString("host", "localhost");
        int port = mysql.getInt("port", 3306);
        String database = mysql.getString("database", "ujobs");
        boolean ssl = mysql.getBoolean("ssl", false);

        hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=" + ssl + "&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikariConfig.setUsername(mysql.getString("username", "root"));
        hikariConfig.setPassword(mysql.getString("password", ""));
    }

    private void configurePostgresql(HikariConfig hikariConfig) {
        ConfigurationSection postgresql = plugin.getConfig().getConfigurationSection("storage.postgresql");
        if (postgresql == null) {
            throw new IllegalStateException("Missing storage.postgresql configuration section");
        }

        String host = postgresql.getString("host", "localhost");
        int port = postgresql.getInt("port", 5432);
        String database = postgresql.getString("database", "ujobs");
        boolean ssl = postgresql.getBoolean("ssl", false);
        String sslMode = ssl ? "require" : "disable";

        hikariConfig.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + database + "?sslmode=" + sslMode);
        hikariConfig.setDriverClassName("org.postgresql.Driver");
        hikariConfig.setUsername(postgresql.getString("username", "postgres"));
        hikariConfig.setPassword(postgresql.getString("password", ""));
    }

    private void initializeSchema() {
        String createSql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
                + "uuid VARCHAR(36) NOT NULL,"
                + "job_id VARCHAR(128) NOT NULL,"
                + "job_level INT NOT NULL,"
                + "job_exp DOUBLE PRECISION NOT NULL,"
                + "total_money DOUBLE PRECISION NOT NULL,"
                + "PRIMARY KEY (uuid, job_id)"
                + ")";

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(createSql);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize database schema", e);
        }
    }

    private PlayerJobData loadPlayerDataFromDatabase(UUID uuid) throws SQLException {
        PlayerJobData playerJobData = new PlayerJobData(uuid);

        String sql = "SELECT job_id, job_level, job_exp, total_money FROM " + TABLE_NAME + " WHERE uuid = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement select = connection.prepareStatement(sql)) {
            select.setString(1, uuid.toString());

            try (ResultSet resultSet = select.executeQuery()) {
                while (resultSet.next()) {
                    String jobId = resultSet.getString("job_id");
                    int level = resultSet.getInt("job_level");
                    double exp = resultSet.getDouble("job_exp");
                    double totalMoney = resultSet.getDouble("total_money");
                    playerJobData.setJobStats(jobId, new PlayerJobData.JobStats(level, exp, totalMoney));
                }
            }
        }

        return playerJobData;
    }

    private void persistPlayerData(UUID uuid, PlayerJobData playerJobData) throws SQLException {
        String deleteSql = "DELETE FROM " + TABLE_NAME + " WHERE uuid = ?";
        String insertSql = "INSERT INTO " + TABLE_NAME + " (uuid, job_id, job_level, job_exp, total_money) VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement delete = connection.prepareStatement(deleteSql)) {
                delete.setString(1, uuid.toString());
                delete.executeUpdate();
            }

            try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
                for (Map.Entry<String, PlayerJobData.JobStats> entry : playerJobData.getJobStats().entrySet()) {
                    PlayerJobData.JobStats stats = entry.getValue();
                    if (stats.getLevel() == 0 && stats.getExp() == 0 && stats.getTotalMoney() == 0) {
                        continue;
                    }
                    insert.setString(1, uuid.toString());
                    insert.setString(2, entry.getKey());
                    insert.setInt(3, stats.getLevel());
                    insert.setDouble(4, stats.getExp());
                    insert.setDouble(5, stats.getTotalMoney());
                    insert.addBatch();
                }
                insert.executeBatch();
            }

            connection.commit();
        }
    }

    private PlayerJobData loadFromPdc(UUID uuid) {
        try {
            return pdcStorage.load(uuid).exceptionally(throwable -> new PlayerJobData(uuid)).join();
        } catch (Exception ignored) {
            return new PlayerJobData(uuid);
        }
    }

    private void completeLoadFuture(UUID uuid, CompletableFuture<PlayerJobData> future, PlayerJobData data) {
        if (uuid == null) {
            future.complete(data);
            return;
        }
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            plugin.getMorePaperLib().scheduling().entitySpecificScheduler(player).run(
                    () -> future.complete(data),
                    () -> future.complete(data)
            );
            return;
        }
        future.complete(data);
    }
}
