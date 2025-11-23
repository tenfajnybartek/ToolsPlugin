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
    private final DatabaseManager db;
    private final Map<String, Location> cache = new HashMap<>();
    private final String TABLE = "server_warps";

    public WarpManager(ToolsPlugin plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.db = dbManager;
        ensureTable();      // Dodatkowe zabezpieczenie (tabela i tak tworzona w DatabaseManager)
        loadWarpsAsync();   // Asynchroniczne ładowanie cache
    }

    private void ensureTable() {
        // Dublowanie CREATE TABLE jeśli ktoś wyłączył jego tworzenie w DatabaseManager – harmless
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
                "warp_name " + (db.getType().equals("mysql") ? "VARCHAR(64)" : "TEXT") + " PRIMARY KEY," +
                "world_name " + (db.getType().equals("mysql") ? "VARCHAR(64)" : "TEXT") + " NOT NULL," +
                "x " + (db.getType().equals("mysql") ? "DOUBLE" : "REAL") + " NOT NULL," +
                "y " + (db.getType().equals("mysql") ? "DOUBLE" : "REAL") + " NOT NULL," +
                "z " + (db.getType().equals("mysql") ? "DOUBLE" : "REAL") + " NOT NULL," +
                "yaw " + (db.getType().equals("mysql") ? "FLOAT" : "REAL") + " NOT NULL," +
                "pitch " + (db.getType().equals("mysql") ? "FLOAT" : "REAL") + " NOT NULL" +
                ")";
        db.executeUpdate(sql);
    }

    /**
     * Ładuje wszystkie warpy do pamięci (cache) asynchronicznie.
     */
    public void loadWarpsAsync() {
        db.queryAsync("SELECT warp_name, world_name, x, y, z, yaw, pitch FROM " + TABLE,
                rs -> {
                    Map<String, Location> temp = new HashMap<>();
                    while (rs.next()) {
                        String name = rs.getString("warp_name").toLowerCase();
                        String world = rs.getString("world_name");
                        if (Bukkit.getWorld(world) == null) {
                            plugin.getLogger().warning("Świat '" + world + "' dla warpa '" + name + "' nie istnieje – pomijam.");
                            continue;
                        }
                        Location loc = new Location(
                                Bukkit.getWorld(world),
                                rs.getDouble("x"),
                                rs.getDouble("y"),
                                rs.getDouble("z"),
                                rs.getFloat("yaw"),
                                rs.getFloat("pitch")
                        );
                        temp.put(name, loc);
                    }
                    return temp;
                }
        ).thenAccept(result -> Bukkit.getScheduler().runTask(plugin, () -> {
            cache.clear();
            cache.putAll(result);
            plugin.getLogger().info("Załadowano " + cache.size() + " warpów.");
        })).exceptionally(ex -> {
            plugin.getLogger().log(Level.SEVERE, "Błąd ładowania warpów async!", ex);
            return null;
        });
    }

    /**
     * Tworzy warp asynchronicznie.
     */
    public CompletableFuture<Boolean> createWarpAsync(String name, Location loc) {
        String key = name.toLowerCase();
        if (cache.containsKey(key)) {
            return CompletableFuture.completedFuture(false);
        }

        return db.updateAsync(
                "INSERT INTO " + TABLE + " (warp_name, world_name, x, y, z, yaw, pitch) VALUES (?,?,?,?,?,?,?)",
                key,
                loc.getWorld().getName(),
                loc.getX(),
                loc.getY(),
                loc.getZ(),
                loc.getYaw(),
                loc.getPitch()
        ).thenApply(rows -> {
            if (rows > 0) {
                Bukkit.getScheduler().runTask(plugin, () -> cache.put(key, loc));
                return true;
            }
            return false;
        }).exceptionally(ex -> {
            plugin.getLogger().log(Level.SEVERE, "Błąd tworzenia warpa '" + key + "'", ex);
            return false;
        });
    }

    /**
     * Usuwa warp asynchronicznie.
     */
    public CompletableFuture<Boolean> deleteWarpAsync(String name) {
        String key = name.toLowerCase();
        if (!cache.containsKey(key)) {
            return CompletableFuture.completedFuture(false);
        }

        return db.updateAsync("DELETE FROM " + TABLE + " WHERE warp_name = ?", key)
                .thenApply(rows -> {
                    if (rows > 0) {
                        Bukkit.getScheduler().runTask(plugin, () -> cache.remove(key));
                        return true;
                    }
                    return false;
                }).exceptionally(ex -> {
                    plugin.getLogger().log(Level.SEVERE, "Błąd usuwania warpa '" + key + "'", ex);
                    return false;
                });
    }

    // ======= Odczyt z cache (szybki, synchroniczny) =======

    public Location getWarp(String name) {
        return cache.get(name.toLowerCase());
    }

    public boolean warpExists(String name) {
        return cache.containsKey(name.toLowerCase());
    }

    public Set<String> getWarpNames() {
        return new HashSet<>(cache.keySet());
    }

    public int getWarpCount() {
        return cache.size();
    }
}