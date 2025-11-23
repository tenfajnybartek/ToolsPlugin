package pl.tenfajnybartek.toolsplugin.managers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {
    private final JavaPlugin plugin;
    private HikariDataSource dataSource;


    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
        setupDatabase();
        createTables();
    }

    /**
     * Konfiguracja połączenia z MySQL (HikariCP)
     */
    private void setupDatabase() {
        String host = plugin.getConfig().getString("database.host", "localhost");
        int port = plugin.getConfig().getInt("database.port", 3306);
        String database = plugin.getConfig().getString("database.database", "toolsplugin");
        String username = plugin.getConfig().getString("database.username", "root");
        String password = plugin.getConfig().getString("database.password", "");

        HikariConfig config = new HikariConfig();
        // Używamy formatu DATETIME (MySQL)
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true&serverTimezone=UTC");
        config.setUsername(username);
        config.setPassword(password);

        // Optymalizacja connection pool (OK)
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        // Dodatkowe ustawienia
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        try {
            dataSource = new HikariDataSource(config);
            plugin.getLogger().info("Połączono z bazą danych MySQL!");
        } catch (Exception e) {
            plugin.getLogger().severe("Nie można połączyć z bazą danych!");
            e.printStackTrace();
        }
    }

    /**
     * Pobiera połączenie z bazy danych
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource nie jest zainicjalizowany!");
        }
        return dataSource.getConnection();
    }

    /**
     * Tworzy tabele w bazie danych
     */
    private void createTables() {
        // Tabele użytkowników (bez zmian, używa BIGINT, co jest OK dla timestamp)
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

        // TABELA BANS: Ujednolicenie nazw i użycie DATETIME
        String bansTable = "CREATE TABLE IF NOT EXISTS bans (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "target_uuid VARCHAR(36) NOT NULL," +
                "target_name VARCHAR(16) NOT NULL," +
                "banner_uuid VARCHAR(36) NOT NULL," +
                "banner_name VARCHAR(16) NOT NULL," +
                "reason TEXT NOT NULL," +
                "ban_time DATETIME NOT NULL," +
                "expire_time DATETIME NULL," +
                "active BOOLEAN NOT NULL DEFAULT TRUE," +
                "INDEX idx_target_uuid (target_uuid)," +
                "INDEX idx_active (active)" +
                ")";

        // TABELA MUTES: Ujednolicenie nazw i użycie DATETIME
        String mutesTable = "CREATE TABLE IF NOT EXISTS mutes (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "target_uuid VARCHAR(36) NOT NULL," +
                "target_name VARCHAR(16) NOT NULL," +
                "muter_uuid VARCHAR(36) NOT NULL," +
                "muter_name VARCHAR(16) NOT NULL," +
                "reason TEXT NOT NULL," +
                "mute_time DATETIME NOT NULL," +
                "expire_time DATETIME NULL," +
                "active BOOLEAN NOT NULL DEFAULT TRUE," +
                "INDEX idx_target_uuid (target_uuid)," +
                "INDEX idx_active (active)" +
                ")";

        try (Connection conn = getConnection()) {
            conn.prepareStatement(usersTable).executeUpdate();
            conn.prepareStatement(bansTable).executeUpdate();
            conn.prepareStatement(mutesTable).executeUpdate();
            plugin.getLogger().info("Tabele bazy danych zostały utworzone/sprawdzone!");
        } catch (SQLException e) {
            plugin.getLogger().severe("Błąd podczas tworzenia tabel!");
            e.printStackTrace();
        }
    }

    /**
     * Zamyka połączenie z bazą danych
     */
    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Rozłączono z bazą danych!");
        }
    }

    /**
     * Sprawdza czy połączenie jest aktywne
     */
    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }
}