package pl.tenfajnybartek.toolsplugin.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class WarpManager {

    private final ToolsPlugin plugin;
    private final DatabaseManager dbManager;
    private final String TABLE = "server_warps";

    private final Map<String, Location> warps = new HashMap<>();

    public WarpManager(ToolsPlugin plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        initializeTable();
        loadWarpsAsync();
    }

    private void initializeTable() {
        if (dbManager.isDisabled()) return;
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
             PreparedStatement ps = connection.prepareStatement(createTableQuery)) {
            ps.execute();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Błąd podczas tworzenia tabeli " + TABLE, e);
        }
    }

    public void loadWarpsAsync() {
        if (dbManager.isDisabled()) {
            plugin.getLogger().warning("DB disabled – pomijam ładowanie warpów.");
            return;
        }
        CompletableFuture.runAsync(() -> {
            Map<String, Location> loaded = new HashMap<>();
            String sql = "SELECT warp_name, world_name, x, y, z, yaw, pitch FROM " + TABLE;
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                int count = 0;
                while (rs.next()) {
                    Location loc = mapResultSet(rs);
                    if (loc != null) {
                        loaded.put(rs.getString("warp_name").toLowerCase(), loc);
                        count++;
                    }
                }
                plugin.getLogger().info("Załadowano " + count + " warpów z DB.");
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Błąd podczas ładowania warpów!", e);
            }

            synchronized (warps) {
                warps.clear();
                warps.putAll(loaded);
            }
        }, ToolsPlugin.getExecutor());
    }

    public CompletableFuture<Boolean> createWarp(String name, Location location) {
        if (dbManager.isDisabled()) return CompletableFuture.completedFuture(false);
        String lower = name.toLowerCase();
        synchronized (warps) {
            if (warps.containsKey(lower)) {
                return CompletableFuture.completedFuture(false);
            }
        }
        String sqlInsert = "INSERT INTO " + TABLE + " (warp_name, world_name, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?)";
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sqlInsert)) {

                ps.setString(1, lower);
                ps.setString(2, location.getWorld().getName());
                ps.setDouble(3, location.getX());
                ps.setDouble(4, location.getY());
                ps.setDouble(5, location.getZ());
                ps.setFloat(6, location.getYaw());
                ps.setFloat(7, location.getPitch());

                if (ps.executeUpdate() > 0) {
                    synchronized (warps) {
                        warps.put(lower, location);
                    }
                    return true;
                }
                return false;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Błąd podczas tworzenia warpa: " + lower, e);
                return false;
            }
        }, ToolsPlugin.getExecutor());
    }

    public CompletableFuture<Boolean> deleteWarp(String name) {
        if (dbManager.isDisabled()) return CompletableFuture.completedFuture(false);
        String lower = name.toLowerCase();
        synchronized (warps) {
            if (!warps.containsKey(lower)) {
                return CompletableFuture.completedFuture(false);
            }
        }
        String sqlDelete = "DELETE FROM " + TABLE + " WHERE warp_name=?";
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sqlDelete)) {

                ps.setString(1, lower);
                if (ps.executeUpdate() > 0) {
                    synchronized (warps) {
                        warps.remove(lower);
                    }
                    return true;
                }
                return false;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Błąd podczas usuwania warpa: " + lower, e);
                return false;
            }
        }, ToolsPlugin.getExecutor());
    }

    public Location getWarp(String name) {
        synchronized (warps) {
            return warps.get(name.toLowerCase());
        }
    }

    public boolean warpExists(String name) {
        synchronized (warps) {
            return warps.containsKey(name.toLowerCase());
        }
    }

    public Set<String> getWarpNames() {
        synchronized (warps) {
            return new HashSet<>(warps.keySet());
        }
    }

    public int getWarpCount() {
        synchronized (warps) {
            return warps.size();
        }
    }

    private Location mapResultSet(ResultSet rs) throws SQLException {
        String worldName = rs.getString("world_name");
        if (worldName == null || Bukkit.getWorld(worldName) == null) {
            plugin.getLogger().warning("Świat '" + worldName + "' nie istnieje dla warpa!");
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