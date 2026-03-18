package fr.cubex.statssync.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import fr.cubex.statssync.CubexStatsSync;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Logger;

public class DatabaseManager {

    private final CubexStatsSync plugin;
    private final Logger log;
    private HikariDataSource dataSource;

    public DatabaseManager(CubexStatsSync plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    public boolean connect() {
        String host     = plugin.getConfig().getString("database.host", "localhost");
        int    port     = plugin.getConfig().getInt("database.port", 3306);
        String dbName   = plugin.getConfig().getString("database.name", "cubexstats");
        String user     = plugin.getConfig().getString("database.username", "pterodactyl");
        String password = plugin.getConfig().getString("database.password", "");
        int    poolSize = plugin.getConfig().getInt("database.pool-size", 5);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mariadb://" + host + ":" + port + "/" + dbName
                + "?useSSL=false&characterEncoding=UTF-8&autoReconnect=true");
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(poolSize);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10_000);
        config.setIdleTimeout(300_000);
        config.setMaxLifetime(600_000);
        config.setPoolName("CubexStatsSync-Pool");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        try {
            dataSource = new HikariDataSource(config);
            return true;
        } catch (Exception e) {
            log.severe("Erreur de connexion HikariCP : " + e.getMessage());
            return false;
        }
    }

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    // ------------------------------------------------------------------ //
    //  DDL
    // ------------------------------------------------------------------ //

    public void createTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS player_stats (
                    uuid             VARCHAR(36)  NOT NULL PRIMARY KEY,
                    username         VARCHAR(16)  NOT NULL,
                    kills            INT          NOT NULL DEFAULT 0,
                    deaths           INT          NOT NULL DEFAULT 0,
                    time_played_hours DOUBLE      NOT NULL DEFAULT 0,
                    last_updated     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
                                                  ON UPDATE CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.execute();
            log.info("Table player_stats vérifiée / créée.");
        } catch (SQLException e) {
            log.severe("Erreur lors de la création de la table : " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ //
    //  DML
    // ------------------------------------------------------------------ //

    /**
     * Insert ou met à jour les stats d'un joueur (UPSERT).
     *
     * @param uuid      UUID du joueur
     * @param username  Nom du joueur
     * @param kills     Nombre de kills
     * @param deaths    Nombre de morts
     * @param hoursPlayed Heures jouées
     */
    public void upsertStats(UUID uuid, String username, int kills, int deaths, double hoursPlayed) {
        String sql = """
                INSERT INTO player_stats (uuid, username, kills, deaths, time_played_hours)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    username         = VALUES(username),
                    kills            = VALUES(kills),
                    deaths           = VALUES(deaths),
                    time_played_hours = VALUES(time_played_hours),
                    last_updated     = CURRENT_TIMESTAMP;
                """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, username);
            ps.setInt(3, kills);
            ps.setInt(4, deaths);
            ps.setDouble(5, hoursPlayed);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.severe("Erreur upsert stats pour " + username + " : " + e.getMessage());
        }
    }
}
