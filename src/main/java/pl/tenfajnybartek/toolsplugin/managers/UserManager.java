package pl.tenfajnybartek.toolsplugin.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
import pl.tenfajnybartek.toolsplugin.objects.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class UserManager {
    private final ToolsPlugin plugin;
    private final DatabaseManager db;

    private final Map<UUID, User> cache = new HashMap<>();

    public UserManager(ToolsPlugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.db = databaseManager;
        ensureTable();
    }

    private void ensureTable() {
        String sql = db.getType().equals("mysql")
                ? "CREATE TABLE IF NOT EXISTS users (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "name VARCHAR(16) NOT NULL," +
                "ip VARCHAR(45) NOT NULL," +
                "first_join BIGINT NOT NULL," +
                "last_join BIGINT NOT NULL," +
                "last_quit BIGINT NOT NULL," +
                "last_message_from VARCHAR(36) NULL," +
                "teleport_toggle BOOLEAN NOT NULL DEFAULT TRUE," +
                "msg_toggle BOOLEAN NOT NULL DEFAULT TRUE," +
                "social_spy BOOLEAN NOT NULL DEFAULT FALSE," +
                "INDEX (name)" +
                ")"
                : "CREATE TABLE IF NOT EXISTS users (" +
                "uuid TEXT PRIMARY KEY," +
                "name TEXT NOT NULL," +
                "ip TEXT NOT NULL," +
                "first_join INTEGER NOT NULL," +
                "last_join INTEGER NOT NULL," +
                "last_quit INTEGER NOT NULL," +
                "last_message_from TEXT," +
                "teleport_toggle INTEGER NOT NULL DEFAULT 1," +
                "msg_toggle INTEGER NOT NULL DEFAULT 1," +
                "social_spy INTEGER NOT NULL DEFAULT 0" +
                ")";
        db.executeUpdate(sql);
    }

    public void loadUser(Player player) {
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        String ip = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "unknown";

        db.queryAsync("SELECT * FROM users WHERE uuid=? LIMIT 1",
                rs -> {
                    if (rs.next()) {
                        long firstJoin = rs.getLong("first_join");
                        long lastJoin = rs.getLong("last_join");
                        long lastQuit = rs.getLong("last_quit");
                        String lastMsgUuid = rs.getString("last_message_from");
                        UUID lastMessageFrom = lastMsgUuid != null ? UUID.fromString(lastMsgUuid) : null;

                        boolean tpToggle = getBool(rs, "teleport_toggle");
                        boolean msgToggle = getBool(rs, "msg_toggle");
                        boolean socialSpy = getBool(rs, "social_spy");

                        User user = new User(uuid, name, ip, firstJoin, lastJoin, lastQuit, lastMessageFrom, tpToggle, msgToggle, socialSpy);
                        user.updateLastJoin();
                        user.setIp(ip);
                        user.setName(name);
                        return user;
                    } else {
                        return new User(uuid, name, ip);
                    }
                },
                uuid.toString()
        ).thenAccept(user -> {
            if (user != null) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    cache.put(uuid, user);
                    saveUserAsync(user);
                });
            }
        });
    }

    public void saveUserAsync(User user) {
        String sql = "INSERT INTO users (uuid, name, ip, first_join, last_join, last_quit, last_message_from, teleport_toggle, msg_toggle, social_spy) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "name=VALUES(name), ip=VALUES(ip), last_join=VALUES(last_join), last_quit=VALUES(last_quit), " +
                "last_message_from=VALUES(last_message_from), teleport_toggle=VALUES(teleport_toggle), msg_toggle=VALUES(msg_toggle), social_spy=VALUES(social_spy)";

        if (db.getType().equals("sqlite")) {
            String updateSql = "UPDATE users SET name=?, ip=?, last_join=?, last_quit=?, last_message_from=?, teleport_toggle=?, msg_toggle=?, social_spy=? WHERE uuid=?";
            db.updateAsync(updateSql,
                    user.getName(),
                    user.getIp(),
                    user.getLastJoin(),
                    user.getLastQuit(),
                    user.getLastMessageFrom() != null ? user.getLastMessageFrom().toString() : null,
                    user.isTeleportToggle() ? 1 : 0,
                    user.isMsgToggle() ? 1 : 0,
                    user.isSocialSpy() ? 1 : 0,
                    user.getUuid().toString()
            ).thenCompose(rows -> {
                if (rows > 0) return CompletableFuture.completedFuture(rows);
                String insertSql = "INSERT INTO users (uuid, name, ip, first_join, last_join, last_quit, last_message_from, teleport_toggle, msg_toggle, social_spy) VALUES (?,?,?,?,?,?,?,?,?,?)";
                return db.updateAsync(insertSql,
                        user.getUuid().toString(),
                        user.getName(),
                        user.getIp(),
                        user.getFirstJoin(),
                        user.getLastJoin(),
                        user.getLastQuit(),
                        user.getLastMessageFrom() != null ? user.getLastMessageFrom().toString() : null,
                        user.isTeleportToggle() ? 1 : 0,
                        user.isMsgToggle() ? 1 : 0,
                        user.isSocialSpy() ? 1 : 0
                );
            });
        } else {
            db.updateAsync(sql,
                    user.getUuid().toString(),
                    user.getName(),
                    user.getIp(),
                    user.getFirstJoin(),
                    user.getLastJoin(),
                    user.getLastQuit(),
                    user.getLastMessageFrom() != null ? user.getLastMessageFrom().toString() : null,
                    user.isTeleportToggle(),
                    user.isMsgToggle(),
                    user.isSocialSpy()
            );
        }
    }

    public void unloadUser(Player player) {
        UUID uuid = player.getUniqueId();
        User user = cache.get(uuid);
        if (user != null) {
            user.updateLastQuit();
            saveUserAsync(user);
            cache.remove(uuid);
        }
    }

    public User getUser(Player player) {
        return cache.get(player.getUniqueId());
    }

    public User getUser(UUID uuid) {
        return cache.get(uuid);
    }

    public boolean hasUser(Player player) {
        return cache.containsKey(player.getUniqueId());
    }

    public void saveAllSyncOnShutdown() {
        for (User user : cache.values()) {
            user.updateLastQuit();
            if (db.getType().equals("sqlite")) {
                String updateSql = "UPDATE users SET name=?, ip=?, last_join=?, last_quit=?, last_message_from=?, teleport_toggle=?, msg_toggle=?, social_spy=? WHERE uuid=?";
                db.executeUpdate(updateSql,
                        user.getName(),
                        user.getIp(),
                        user.getLastJoin(),
                        user.getLastQuit(),
                        user.getLastMessageFrom() != null ? user.getLastMessageFrom().toString() : null,
                        user.isTeleportToggle() ? 1 : 0,
                        user.isMsgToggle() ? 1 : 0,
                        user.isSocialSpy() ? 1 : 0,
                        user.getUuid().toString()
                );
            } else {
                db.executeUpdate(
                        "INSERT INTO users (uuid, name, ip, first_join, last_join, last_quit, last_message_from, teleport_toggle, msg_toggle, social_spy) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                                "ON DUPLICATE KEY UPDATE name=VALUES(name), ip=VALUES(ip), last_join=VALUES(last_join), last_quit=VALUES(last_quit), " +
                                "last_message_from=VALUES(last_message_from), teleport_toggle=VALUES(teleport_toggle), msg_toggle=VALUES(msg_toggle), social_spy=VALUES(social_spy)",
                        user.getUuid().toString(),
                        user.getName(),
                        user.getIp(),
                        user.getFirstJoin(),
                        user.getLastJoin(),
                        user.getLastQuit(),
                        user.getLastMessageFrom() != null ? user.getLastMessageFrom().toString() : null,
                        user.isTeleportToggle(),
                        user.isMsgToggle(),
                        user.isSocialSpy()
                );
            }
        }
        cache.clear();
    }

    private boolean getBool(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        if (db.getType().equals("mysql")) {
            return rs.getBoolean(column);
        } else {
            return rs.getInt(column) == 1;
        }
    }
}