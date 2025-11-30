package pl.tenfajnybartek.toolsplugin.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class HomeManager {

    private final ToolsPlugin plugin;
    private final ConfigManager configManager;
    private final DatabaseManager db;
    private final String TABLE = "homes";

    private final Map<UUID, Map<String, Location>> cache = new HashMap<>();

    public HomeManager(ToolsPlugin plugin, ConfigManager configManager, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.db = dbManager;
        ensureTable();
    }

    private void ensureTable() {
        String sql = db.getType().equals("mysql")
                ? "CREATE TABLE IF NOT EXISTS homes (" +
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
                ")"
                : "CREATE TABLE IF NOT EXISTS homes (" +
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
        db.executeUpdate(sql);
    }
    public CompletableFuture<Map<String, Location>> loadPlayerHomesAsync(UUID ownerUuid) {
        return db.queryAsync(
                "SELECT home_name, world_name, x, y, z, yaw, pitch FROM " + TABLE + " WHERE owner_uuid = ?",
                rs -> {
                    Map<String, Location> homes = new HashMap<>();
                    while (rs.next()) {
                        Location loc = toLocation(rs);
                        if (loc != null) {
                            homes.put(rs.getString("home_name").toLowerCase(), loc);
                        }
                    }
                    return homes;
                },
                ownerUuid.toString()
        ).thenApply(map -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                cache.put(ownerUuid, map);
            });
            return map;
        });
    }

    public CompletableFuture<Boolean> setHomeAsync(Player player, String name, Location location) {
        UUID uuid = player.getUniqueId();
        String key = name.toLowerCase();

        int maxHomes = getMaxHomes(player);
        int current = getHomeCount(player);

        boolean isUpdate = hasHome(player, key);
        if (!isUpdate && current >= maxHomes) {
            return CompletableFuture.completedFuture(false);
        }

        String updateSql = "UPDATE " + TABLE + " SET world_name=?, x=?, y=?, z=?, yaw=?, pitch=? WHERE owner_uuid=? AND home_name=?";
        String insertSql = "INSERT INTO " + TABLE + " (owner_uuid, home_name, world_name, x, y, z, yaw, pitch) VALUES (?,?,?,?,?,?,?,?)";

        return db.updateAsync(updateSql,
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch(),
                uuid.toString(),
                key
        ).thenCompose(rows -> {
            if (rows > 0) {
                Bukkit.getScheduler().runTask(plugin, () -> putInCache(uuid, key, location));
                return CompletableFuture.completedFuture(true);
            }
            return db.updateAsync(insertSql,
                    uuid.toString(),
                    key,
                    location.getWorld().getName(),
                    location.getX(),
                    location.getY(),
                    location.getZ(),
                    location.getYaw(),
                    location.getPitch()
            ).thenApply(insertRows -> {
                if (insertRows > 0) {
                    Bukkit.getScheduler().runTask(plugin, () -> putInCache(uuid, key, location));
                    return true;
                }
                return false;
            });
        });
    }

    public CompletableFuture<Boolean> deleteHomeAsync(Player player, String name) {
        UUID uuid = player.getUniqueId();
        String key = name.toLowerCase();
        if (!hasHome(player, key)) {
            return CompletableFuture.completedFuture(false);
        }
        return db.updateAsync("DELETE FROM " + TABLE + " WHERE owner_uuid=? AND home_name=?",
                uuid.toString(),
                key
        ).thenApply(rows -> {
            if (rows > 0) {
                Bukkit.getScheduler().runTask(plugin, () -> removeFromCache(uuid, key));
                return true;
            }
            return false;
        });
    }


    private Location toLocation(ResultSet rs) throws SQLException {
        String world = rs.getString("world_name");
        if (world == null || Bukkit.getWorld(world) == null) return null;
        return new Location(
                Bukkit.getWorld(world),
                rs.getDouble("x"),
                rs.getDouble("y"),
                rs.getDouble("z"),
                rs.getFloat("yaw"),
                rs.getFloat("pitch")
        );
    }

    private void putInCache(UUID uuid, String name, Location loc) {
        cache.computeIfAbsent(uuid, u -> new HashMap<>()).put(name, loc);
    }

    private void removeFromCache(UUID uuid, String name) {
        Map<String, Location> map = cache.get(uuid);
        if (map != null) {
            map.remove(name);
            if (map.isEmpty()) cache.remove(uuid);
        }
    }

    public Location getHome(Player player, String name) {
        Map<String, Location> map = cache.get(player.getUniqueId());
        return map == null ? null : map.get(name.toLowerCase());
    }

    public boolean hasHome(Player player, String name) {
        Map<String, Location> map = cache.get(player.getUniqueId());
        return map != null && map.containsKey(name.toLowerCase());
    }

    public Set<String> getHomeNames(Player player) {
        Map<String, Location> map = cache.get(player.getUniqueId());
        return map == null ? Collections.emptySet() : new HashSet<>(map.keySet());
    }

    public int getHomeCount(Player player) {
        Map<String, Location> map = cache.get(player.getUniqueId());
        return map == null ? 0 : map.size();
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

    public void unloadPlayerHomes(UUID uuid) {
        cache.remove(uuid);
    }
}