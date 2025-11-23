package pl.tenfajnybartek.toolsplugin.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class WarpManager {

    private final ToolsPlugin plugin;
    // Usunięto warpsFile i warpsConfig
    private final DatabaseManager dbManager; // DODANO
    private final String TABLE = "server_warps";

    // Mapa warpów: nazwa -> lokalizacja (CACHE)
    private final Map<String, Location> warps;

    public WarpManager(ToolsPlugin plugin, DatabaseManager dbManager) { // ZMIENIONO KONSTRUKTOR
        this.plugin = plugin;
        this.dbManager = dbManager;
        this.warps = new HashMap<>();

        initializeTable();
        loadWarps(); // Asynchroniczne ładowanie na starcie
    }

    // ====================================================================
    // 1. Inicjalizacja Bazy Danych
    // ====================================================================
    private void initializeTable() {
        String createTableQuery = "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
                "warp_name VARCHAR(64) PRIMARY KEY," +
                "world_name VARCHAR(64) NOT NULL," +
                "x DOUBLE NOT NULL," +
                "y DOUBLE NOT NULL," +
                "z DOUBLE NOT NULL," +
                "yaw FLOAT NOT NULL," +
                "pitch FLOAT NOT NULL" +
                ")";

        try (Connection connection = dbManager.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(createTableQuery);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Błąd podczas tworzenia tabeli " + TABLE, e);
        }
    }

    // ====================================================================
    // 2. Ładowanie Warpów (Asynchroniczne)
    // ====================================================================

    /**
     * Ładuje warpy z DB do cache asynchronicznie. Wywoływane przy starcie.
     */
    public CompletableFuture<Void> loadWarps() {
        return CompletableFuture.runAsync(() -> {
            warps.clear();
            String sqlSelect = "SELECT * FROM " + TABLE;

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sqlSelect);
                 ResultSet rs = ps.executeQuery()) {

                int loadedCount = 0;
                while (rs.next()) {
                    Location location = mapResultSetToLocation(rs);
                    String warpName = rs.getString("warp_name").toLowerCase();

                    if (location != null) {
                        warps.put(warpName, location);
                        loadedCount++;
                    }
                }
                plugin.getLogger().info("Załadowano " + loadedCount + " warpów z DB!");
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Błąd podczas ładowania warpów z bazy danych!", e);
            }
        }, ToolsPlugin.getExecutor());
    }

    // Usunięto saveWarps() - niepotrzebne przy natychmiastowym zapisie do DB

    // ====================================================================
    // 3. Logika Warpów (Asynchroniczna)
    // ====================================================================

    /**
     * Tworzy nowy warp w DB asynchronicznie.
     */
    public CompletableFuture<Boolean> createWarp(String name, Location location) {
        String lowerName = name.toLowerCase();

        if (warps.containsKey(lowerName)) {
            return CompletableFuture.completedFuture(false); // Warp już istnieje w cache
        }

        // INSERT z użyciem ON DUPLICATE KEY UPDATE dla uniknięcia wyjątków
        String sqlInsert = "INSERT INTO " + TABLE +
                " (warp_name, world_name, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?)";

        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sqlInsert)) {

                // Ustawianie 7 parametrów
                ps.setString(1, lowerName);
                ps.setString(2, location.getWorld().getName());
                ps.setDouble(3, location.getX());
                ps.setDouble(4, location.getY());
                ps.setDouble(5, location.getZ());
                ps.setFloat(6, location.getYaw());
                ps.setFloat(7, location.getPitch());

                if (ps.executeUpdate() > 0) {
                    warps.put(lowerName, location); // Aktualizacja cache
                    return true;
                }
                return false;
            } catch (SQLException e) {
                // To powinno obsłużyć głównie wyjątki UNIQUE KEY, jeśli cache zawiedzie
                plugin.getLogger().log(Level.SEVERE, "Błąd podczas tworzenia warpa: " + lowerName, e);
                return false;
            }
        }, ToolsPlugin.getExecutor());
    }

    /**
     * Usuwa warp z DB asynchronicznie.
     */
    public CompletableFuture<Boolean> deleteWarp(String name) {
        String lowerName = name.toLowerCase();

        if (!warps.containsKey(lowerName)) {
            return CompletableFuture.completedFuture(false); // Warp nie istnieje w cache
        }

        String sqlDelete = "DELETE FROM " + TABLE + " WHERE warp_name = ?";

        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sqlDelete)) {

                ps.setString(1, lowerName);

                if (ps.executeUpdate() > 0) {
                    warps.remove(lowerName); // Usunięcie z cache
                    return true;
                }
                return false;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Błąd podczas usuwania warpa: " + lowerName, e);
                return false;
            }
        }, ToolsPlugin.getExecutor());
    }

    // ====================================================================
    // 4. Metody Odczytu (Synchroniczne z Cache)
    // ====================================================================

    public Location getWarp(String name) {
        return warps.get(name.toLowerCase());
    }

    public boolean warpExists(String name) {
        return warps.containsKey(name.toLowerCase());
    }

    public Set<String> getWarpNames() {
        return new HashSet<>(warps.keySet());
    }

    public int getWarpCount() {
        return warps.size();
    }

    // ====================================================================
    // 5. Metody Pomocnicze
    // ====================================================================

    private Location mapResultSetToLocation(ResultSet rs) throws SQLException {
        String worldName = rs.getString("world_name");

        if (worldName == null || Bukkit.getWorld(worldName) == null) {
            plugin.getLogger().warning("Błąd: Świat '" + worldName + "' nie istnieje dla warpa!");
            return null;
        }

        return new Location(
                Bukkit.getWorld(worldName),
                rs.getDouble("x"),
                rs.getDouble("y"),
                rs.getDouble("z"),
                rs.getFloat("yaw"),
                rs.getFloat("pitch")
        );
    }
}