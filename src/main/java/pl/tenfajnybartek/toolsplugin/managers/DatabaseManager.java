package pl.tenfajnybartek.toolsplugin.managers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;

import java.io.File;
import java.sql.*;
import java.util.function.Function;
import java.util.logging.Level;

public class DatabaseManager {

    private final ToolsPlugin plugin;
    private final ConfigManager configManager;

    private final String type;
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final boolean debug;

    // MySQL: Hikari
    private HikariDataSource mysqlDataSource;

    // SQLite: pojedyncze połączenie
    private Connection sqliteConnection;

    public DatabaseManager(ToolsPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();

        this.type = getCfgString("database.type", "mysql").toLowerCase();
        this.host = getCfgString("database.host", "localhost");
        this.port = getCfgInt("database.port", 3306);
        this.database = getCfgString("database.database", "toolsplugin");
        this.username = getCfgString("database.username", "root");
        this.password = getCfgString("database.password", "");
        this.debug = plugin.getConfig().getBoolean("settings.debug", false);

        validateConfig();
        setupBackend();
        createTables();
    }

    private String getCfgString(String path, String def) {
        return plugin.getConfig().getString(path, def);
    }

    private int getCfgInt(String path, int def) {
        return plugin.getConfig().getInt(path, def);
    }

    private void validateConfig() {
        if (!type.equals("mysql") && !type.equals("sqlite")) {
            plugin.getLogger().severe("Nieobsługiwany database.type: " + type + " (dozwolone: mysql, sqlite)");
        }
        if (type.equals("mysql")) {
            if (host.isEmpty()) plugin.getLogger().severe("Konfiguracja bazy: 'database.host' jest pusta!");
            if (database.isEmpty()) plugin.getLogger().severe("Konfiguracja bazy: 'database.database' jest pusta!");
        }
    }

    /**
     * Inicjalizacja odpowiedniego backendu: MySQL (Hikari) lub SQLite (połączenie)
     */
    private void setupBackend() {
        if (type.equals("mysql")) {
            setupMySql();
        } else {
            setupSQLite();
        }
    }

    private void setupMySql() {
        try {
            HikariConfig cfg = new HikariConfig();
            String jdbcUrl = buildMySqlJdbcUrl();
            cfg.setJdbcUrl(jdbcUrl);
            cfg.setUsername(username);
            cfg.setPassword(password);

            // Parametry puli
            cfg.setMaximumPoolSize(10);
            cfg.setMinimumIdle(2);
            cfg.setConnectionTimeout(30_000);
            cfg.setIdleTimeout(600_000);
            cfg.setMaxLifetime(1_800_000);

            // Optymalizacje
            cfg.addDataSourceProperty("cachePrepStmts", "true");
            cfg.addDataSourceProperty("prepStmtCacheSize", "250");
            cfg.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            cfg.addDataSourceProperty("useServerPrepStmts", "true");
            cfg.addDataSourceProperty("useLocalSessionState", "true");
            cfg.addDataSourceProperty("rewriteBatchedStatements", "true");
            cfg.addDataSourceProperty("cacheResultSetMetadata", "true");
            cfg.addDataSourceProperty("cacheServerConfiguration", "true");
            cfg.addDataSourceProperty("elideSetAutoCommits", "true");
            cfg.addDataSourceProperty("maintainTimeStats", "false");

            mysqlDataSource = new HikariDataSource(cfg);
            plugin.getLogger().info("Połączono z MySQL: " + jdbcUrl);
            if (debug) plugin.getLogger().info("[DEBUG] MySQL user=" + username);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Nie udało się skonfigurować połączenia MySQL!", e);
        }
    }

    private void setupSQLite() {
        try {
            // Plik bazy w katalogu pluginu
            File dbFile = new File(plugin.getDataFolder(), database + ".db");
            if (!plugin.getDataFolder().exists()) {
                // Utwórz folder pluginu, jeśli brak
                plugin.getDataFolder().mkdirs();
            }
            String jdbcUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            sqliteConnection = DriverManager.getConnection(jdbcUrl);
            plugin.getLogger().info("Połączono z SQLite: " + jdbcUrl);
            if (debug) plugin.getLogger().info("[DEBUG] SQLite plik=" + dbFile.getAbsolutePath());
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Nie udało się połączyć z SQLite!", e);
        }
    }

    private String buildMySqlJdbcUrl() {
        return "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false"
                + "&allowPublicKeyRetrieval=true"
                + "&autoReconnect=true"
                + "&characterEncoding=UTF-8"
                + "&connectionCollation=utf8mb4_unicode_ci"
                + "&serverTimezone=UTC";
    }

    /**
     * Tworzenie tabel (różnice w składni MySQL vs SQLite)
     */
    private void createTables() {
        if (type.equals("mysql")) {
            createTablesMySql();
        } else {
            createTablesSQLite();
        }
    }

    private void createTablesMySql() {
        String usersTable = "CREATE TABLE IF NOT EXISTS users (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "name VARCHAR(16) NOT NULL," +
                "ip VARCHAR(45)," +
                "first_join BIGINT NOT NULL," +
                "last_join BIGINT NOT NULL," +
                "last_quit BIGINT," +
                "last_message_from VARCHAR(36)," +
                "teleport_toggle BOOLEAN DEFAULT TRUE," +
                "INDEX idx_name (name)" +
                ")";

        String bansTable = "CREATE TABLE IF NOT EXISTS bans (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "target_uuid VARCHAR(36) NOT NULL," +
                "target_name VARCHAR(16) NOT NULL," +
                "banner_uuid VARCHAR(36) NOT NULL," +
                "banner_name VARCHAR(16) NOT NULL," +
                "reason TEXT NOT NULL," +
                "ban_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "expire_time TIMESTAMP NULL," +
                "active BOOLEAN NOT NULL DEFAULT TRUE," +
                "INDEX idx_target_uuid (target_uuid)," +
                "INDEX idx_active (active)" +
                ")";

        String mutesTable = "CREATE TABLE IF NOT EXISTS mutes (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "target_uuid VARCHAR(36) NOT NULL," +
                "target_name VARCHAR(16) NOT NULL," +
                "muter_uuid VARCHAR(36) NOT NULL," +
                "muter_name VARCHAR(16) NOT NULL," +
                "reason TEXT NOT NULL," +
                "mute_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "expire_time TIMESTAMP NULL," +
                "active BOOLEAN NOT NULL DEFAULT TRUE," +
                "INDEX idx_target_uuid (target_uuid)," +
                "INDEX idx_active (active)" +
                ")";

        String warpsTable = "CREATE TABLE IF NOT EXISTS server_warps (" +
                "warp_name VARCHAR(64) PRIMARY KEY," +
                "world_name VARCHAR(64) NOT NULL," +
                "x DOUBLE NOT NULL," +
                "y DOUBLE NOT NULL," +
                "z DOUBLE NOT NULL," +
                "yaw FLOAT NOT NULL," +
                "pitch FLOAT NOT NULL" +
                ")";

        String homesTable = "CREATE TABLE IF NOT EXISTS homes (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "owner_uuid VARCHAR(36) NOT NULL," +
                "home_name VARCHAR(64) NOT NULL," +
                "world_name VARCHAR(64) NOT NULL," +
                "x DOUBLE NOT NULL," +
                "y DOUBLE NOT NULL," +
                "z DOUBLE NOT NULL," +
                "yaw FLOAT NOT NULL," +
                "pitch FLOAT NOT NULL," +
                "UNIQUE KEY unique_home (owner_uuid, home_name)" +
                ")";

        try (Connection conn = getConnection();
             Statement st = conn.createStatement()) {
            st.execute(usersTable);
            st.execute(bansTable);
            st.execute(mutesTable);
            st.execute(warpsTable);
            st.execute(homesTable);
            plugin.getLogger().info("Sprawdzono / utworzono tabele (MySQL).");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Błąd tworzenia tabel (MySQL)!", e);
        }
    }

    private void createTablesSQLite() {
        String usersTable = "CREATE TABLE IF NOT EXISTS users (" +
                "uuid TEXT PRIMARY KEY," +
                "name TEXT NOT NULL," +
                "ip TEXT," +
                "first_join INTEGER NOT NULL," +
                "last_join INTEGER NOT NULL," +
                "last_quit INTEGER," +
                "last_message_from TEXT," +
                "teleport_toggle INTEGER DEFAULT 1" +
                ")";

        String bansTable = "CREATE TABLE IF NOT EXISTS bans (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "target_uuid TEXT NOT NULL," +
                "target_name TEXT NOT NULL," +
                "banner_uuid TEXT NOT NULL," +
                "banner_name TEXT NOT NULL," +
                "reason TEXT NOT NULL," +
                "ban_time INTEGER NOT NULL," +        // epoch millis
                "expire_time INTEGER," +              // NULL = brak wygaśnięcia
                "active INTEGER NOT NULL DEFAULT 1" + // 1 = true
                ")";

        String mutesTable = "CREATE TABLE IF NOT EXISTS mutes (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "target_uuid TEXT NOT NULL," +
                "target_name TEXT NOT NULL," +
                "muter_uuid TEXT NOT NULL," +
                "muter_name TEXT NOT NULL," +
                "reason TEXT NOT NULL," +
                "mute_time INTEGER NOT NULL," +
                "expire_time INTEGER," +
                "active INTEGER NOT NULL DEFAULT 1" +
                ")";

        String warpsTable = "CREATE TABLE IF NOT EXISTS server_warps (" +
                "warp_name TEXT PRIMARY KEY," +
                "world_name TEXT NOT NULL," +
                "x REAL NOT NULL," +
                "y REAL NOT NULL," +
                "z REAL NOT NULL," +
                "yaw REAL NOT NULL," +
                "pitch REAL NOT NULL" +
                ")";

        String homesTable = "CREATE TABLE IF NOT EXISTS homes (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "owner_uuid TEXT NOT NULL," +
                "home_name TEXT NOT NULL," +
                "world_name TEXT NOT NULL," +
                "x REAL NOT NULL," +
                "y REAL NOT NULL," +
                "z REAL NOT NULL," +
                "yaw REAL NOT NULL," +
                "pitch REAL NOT NULL," +
                "UNIQUE(owner_uuid, home_name)" +
                ")";

        try (Connection conn = getConnection();
             Statement st = conn.createStatement()) {
            st.execute(usersTable);
            st.execute(bansTable);
            st.execute(mutesTable);
            st.execute(warpsTable);
            st.execute(homesTable);
            plugin.getLogger().info("Sprawdzono / utworzono tabele (SQLite).");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Błąd tworzenia tabel (SQLite)!", e);
        }
    }

    /**
     * Publiczny dostęp do połączenia
     */
    public Connection getConnection() throws SQLException {
        if (type.equals("mysql")) {
            if (mysqlDataSource == null) throw new SQLException("MySQL DataSource nie zainicjalizowany!");
            return mysqlDataSource.getConnection();
        } else {
            if (sqliteConnection == null || sqliteConnection.isClosed()) {
                throw new SQLException("Połączenie SQLite jest zamknięte!");
            }
            return sqliteConnection;
        }
    }

    /**
     * Proste wykonanie UPDATE/INSERT/DELETE
     */
    public int executeUpdate(String sql, Object... params) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindParams(ps, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Błąd executeUpdate: " + sql, e);
            return -1;
        }
    }

    /**
     * Wykonanie SELECT z mapowaniem na obiekt
     */
    public <T> T executeQuery(String sql, Function<ResultSet, T> mapper, Object... params) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return mapper.apply(rs);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Błąd executeQuery: " + sql, e);
            return null;
        }
    }

    private void bindParams(PreparedStatement ps, Object... params) throws SQLException {
        if (params == null) return;
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }

    /**
     * Test połączenia (krótkie)
     */
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Test połączenia nieudany!", e);
            return false;
        }
    }

    /**
     * Zamknięcie backendu
     */
    public void disconnect() {
        if (type.equals("mysql")) {
            if (mysqlDataSource != null && !mysqlDataSource.isClosed()) {
                mysqlDataSource.close();
                plugin.getLogger().info("Zamknięto pulę MySQL.");
            }
        } else {
            if (sqliteConnection != null) {
                try {
                    sqliteConnection.close();
                    plugin.getLogger().info("Zamknięto połączenie SQLite.");
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.SEVERE, "Błąd przy zamykaniu SQLite!", e);
                }
            }
        }
    }

    public boolean isConnected() {
        if (type.equals("mysql")) {
            return mysqlDataSource != null && !mysqlDataSource.isClosed();
        } else {
            try {
                return sqliteConnection != null && !sqliteConnection.isClosed();
            } catch (SQLException e) {
                return false;
            }
        }
    }

    public String getType() {
        return type;
    }
}