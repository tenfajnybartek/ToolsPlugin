package pl.tenfajnybartek.toolsplugin.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class HomeManager {

    private final ToolsPlugin plugin;
    private final ConfigManager configManager;
    private final DatabaseManager dbManager;
    private final String TABLE = "player_homes";

    // Mapa home'ów: UUID gracza -> (nazwa home -> lokalizacja)
    private final Map<UUID, Map<String, Location>> homes;

    public HomeManager(ToolsPlugin plugin, ConfigManager configManager, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.dbManager = dbManager;
        this.homes = new HashMap<>();

        initializeTable();
    }

    // ====================================================================
    // 1. Inicjalizacja Bazy Danych
    // ====================================================================
    private void initializeTable() {
        String createTableQuery = "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "player_uuid VARCHAR(36) NOT NULL," +
                "home_name VARCHAR(64) NOT NULL," +
                "world_name VARCHAR(64) NOT NULL," +
                "x DOUBLE NOT NULL," +
                "y DOUBLE NOT NULL," +
                "z DOUBLE NOT NULL," +
                "yaw FLOAT NOT NULL," +
                "pitch FLOAT NOT NULL," +
                "UNIQUE KEY unique_home (player_uuid, home_name)," +
                "INDEX (player_uuid)" +
                ")";

        try (Connection connection = dbManager.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(createTableQuery);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Błąd podczas tworzenia tabeli " + TABLE, e);
        }
    }

    // ====================================================================
    // 2. Ładowanie Home'ów (Asynchroniczne)
    // ====================================================================

    public CompletableFuture<Void> loadPlayerHomes(UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            Map<String, Location> playerHomes = new HashMap<>();
            String sqlSelect = "SELECT * FROM " + TABLE + " WHERE player_uuid = ?";

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sqlSelect)) {

                ps.setString(1, uuid.toString());

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Location location = mapResultSetToLocation(rs);
                        String homeName = rs.getString("home_name").toLowerCase();
                        // WAŻNE: Dodaj do mapy tylko jeśli lokalizacja nie jest null (świat istnieje)
                        if (location != null) {
                            playerHomes.put(homeName, location);
                        }
                    }
                }
                homes.put(uuid, playerHomes);

            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Błąd podczas ładowania home'ów dla gracza " + uuid, e);
            }
        }, ToolsPlugin.getExecutor());
    }

    public void unloadPlayerHomes(UUID uuid) {
        homes.remove(uuid);
    }

    // ====================================================================
    // 3. Logika Home'ów (Asynchroniczna)
    // ====================================================================

    /**
     * Tworzy lub aktualizuje home dla gracza.
     * NAPRAWIONO: Jawne ustawianie parametrów dla UPDATE/INSERT, bez metody pomocniczej.
     */
    public CompletableFuture<Boolean> createHome(Player player, String name, Location location) {
        UUID uuid = player.getUniqueId();
        String lowerName = name.toLowerCase();

        int maxHomes = getMaxHomes(player);
        int currentHomes = getHomeCount(player);

        if (currentHomes >= maxHomes && !getHomeNames(player).contains(lowerName)) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {

            String sqlUpdate = "UPDATE " + TABLE +
                    " SET world_name=?, x=?, y=?, z=?, yaw=?, pitch=? WHERE player_uuid=? AND home_name=?";

            String sqlInsert = "INSERT INTO " + TABLE +
                    " (player_uuid, home_name, world_name, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

            try (Connection conn = dbManager.getConnection()) {

                // 1. Próba aktualizacji (8 parametrów)
                try (PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate)) {
                    // Ustawianie 6 parametrów SET
                    psUpdate.setString(1, location.getWorld().getName());
                    psUpdate.setDouble(2, location.getX());
                    psUpdate.setDouble(3, location.getY());
                    psUpdate.setDouble(4, location.getZ());
                    psUpdate.setFloat(5, location.getYaw());
                    psUpdate.setFloat(6, location.getPitch());

                    // Ustawianie 2 parametrów WHERE
                    psUpdate.setString(7, uuid.toString());
                    psUpdate.setString(8, lowerName);

                    if (psUpdate.executeUpdate() > 0) {
                        updateHomeCache(uuid, lowerName, location);
                        return true;
                    }
                }

                // 2. Jeśli aktualizacja nie powiodła się, wstaw nowy rekord (8 parametrów)
                try (PreparedStatement psInsert = conn.prepareStatement(sqlInsert)) {
                    // Ustawianie 2 parametrów gracza
                    psInsert.setString(1, uuid.toString());
                    psInsert.setString(2, lowerName);

                    // Ustawianie 6 parametrów lokalizacji
                    psInsert.setString(3, location.getWorld().getName());
                    psInsert.setDouble(4, location.getX());
                    psInsert.setDouble(5, location.getY());
                    psInsert.setDouble(6, location.getZ());
                    psInsert.setFloat(7, location.getYaw());
                    psInsert.setFloat(8, location.getPitch());

                    if (psInsert.executeUpdate() > 0) {
                        updateHomeCache(uuid, lowerName, location);
                        return true;
                    }
                }

                return false;

            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Błąd podczas tworzenia/aktualizacji home: " + lowerName + " dla " + player.getName(), e);
                return false;
            }
        }, ToolsPlugin.getExecutor());
    }

    /**
     * Usuwa home gracza asynchronicznie.
     */
    public CompletableFuture<Boolean> deleteHome(Player player, String name) {
        UUID uuid = player.getUniqueId();
        String lowerName = name.toLowerCase();

        if (!homes.containsKey(uuid) || !homes.get(uuid).containsKey(lowerName)) {
            return CompletableFuture.completedFuture(false);
        }

        // ASYNCHRONICZNA OPERACJA USUWANIA
        return CompletableFuture.supplyAsync(() -> {
            String sqlDelete = "DELETE FROM " + TABLE + " WHERE player_uuid = ? AND home_name = ?";
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sqlDelete)) {

                // Ustawianie 2 parametrów
                ps.setString(1, uuid.toString());
                ps.setString(2, lowerName);

                if (ps.executeUpdate() > 0) {
                    removeHomeFromCache(uuid, lowerName);
                    return true;
                }
                return false;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Błąd podczas usuwania home: " + lowerName + " dla " + player.getName(), e);
                return false;
            }
        }, ToolsPlugin.getExecutor());
    }

    // ... [Metody Synchroniczne getHome, hasHome, getHomeNames, getHomeCount, getMaxHomes bez zmian]

    // ====================================================================
    // 4. Metody Pomocnicze i Cache
    // ====================================================================

    // Metoda mapResultSetToLocation (bez zmian)
    private Location mapResultSetToLocation(ResultSet rs) throws SQLException {
        String worldName = rs.getString("world_name");

        if (worldName == null || Bukkit.getWorld(worldName) == null) {
            plugin.getLogger().warning("Błąd: Świat '" + worldName + "' nie istnieje dla home!");
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

    // USUNIĘTO NIEPOPRAWNĄ METODĘ setHomeLocationParams
    // private void setHomeLocationParams(...) { ... }

    private void updateHomeCache(UUID uuid, String name, Location location) {
        homes.computeIfAbsent(uuid, k -> new HashMap<>()).put(name, location);
    }

    private void removeHomeFromCache(UUID uuid, String name) {
        if (homes.containsKey(uuid)) {
            homes.get(uuid).remove(name);
            if (homes.get(uuid).isEmpty()) {
                homes.remove(uuid);
            }
        }
    }

    // ... [Metoda getMaxHomes bez zmian]
    public Location getHome(Player player, String name) {
        UUID uuid = player.getUniqueId();
        String lowerName = name.toLowerCase();

        if (!homes.containsKey(uuid)) {
            return null;
        }

        return homes.get(uuid).get(lowerName);
    }

    public boolean hasHome(Player player, String name) {
        UUID uuid = player.getUniqueId();
        String lowerName = name.toLowerCase();

        return homes.containsKey(uuid) && homes.get(uuid).containsKey(lowerName);
    }

    public Set<String> getHomeNames(Player player) {
        UUID uuid = player.getUniqueId();

        if (!homes.containsKey(uuid)) {
            return new HashSet<>();
        }

        return new HashSet<>(homes.get(uuid).keySet());
    }

    public int getHomeCount(Player player) {
        UUID uuid = player.getUniqueId();

        if (!homes.containsKey(uuid)) {
            return 0;
        }

        return homes.get(uuid).size();
    }
    public int getMaxHomes(Player player) {
        if (player.hasPermission("tfbhc.homes.admin")) {
            return configManager.getConfig().getInt("homes.rank-limits.admin", 20);
        }
        if (player.hasPermission("tfbhc.homes.mvp")) {
            return configManager.getConfig().getInt("homes.rank-limits.mvp", 10);
        }
        if (player.hasPermission("tfbhc.homes.vip")) {
            return configManager.getConfig().getInt("homes.rank-limits.vip", 5);
        }

        return configManager.getConfig().getInt("homes.max-per-player", 3);
    }
}