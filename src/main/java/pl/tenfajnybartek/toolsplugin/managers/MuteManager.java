package pl.tenfajnybartek.toolsplugin.managers;

import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
import pl.tenfajnybartek.toolsplugin.objects.MuteRecord;
import pl.tenfajnybartek.toolsplugin.utils.TimeUtils;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class MuteManager {

    private final ToolsPlugin plugin;
    private final DatabaseManager db;
    private final String TABLE = "mutes";

    public MuteManager(ToolsPlugin plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.db = dbManager;
        ensureTable();
    }

    private void ensureTable() {
        String sql = db.getType().equals("mysql")
                ? "CREATE TABLE IF NOT EXISTS mutes (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "target_uuid VARCHAR(36) NOT NULL," +
                "target_name VARCHAR(16) NOT NULL," +
                "muter_uuid VARCHAR(36) NOT NULL," +
                "muter_name VARCHAR(16) NOT NULL," +
                "mute_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "expire_time TIMESTAMP NULL," +
                "reason TEXT NOT NULL," +
                "active BOOLEAN NOT NULL DEFAULT TRUE," +
                "INDEX (target_uuid), INDEX (active)" +
                ")"
                : "CREATE TABLE IF NOT EXISTS mutes (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "target_uuid TEXT NOT NULL," +
                "target_name TEXT NOT NULL," +
                "muter_uuid TEXT NOT NULL," +
                "muter_name TEXT NOT NULL," +
                "mute_time INTEGER NOT NULL," +
                "expire_time INTEGER NULL," +
                "reason TEXT NOT NULL," +
                "active INTEGER NOT NULL DEFAULT 1" +
                ")";
        db.executeUpdate(sql);
    }

    public CompletableFuture<MuteRecord> mutePlayer(OfflinePlayer target, OfflinePlayer muter, String timeString, String reason) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expire = TimeUtils.parseTime(timeString); // null = perm
        String finalReason = reason == null || reason.isEmpty() ? "Brak powodu" : reason;

        // Dezaktywuj stare
        return db.updateAsync("UPDATE " + TABLE + " SET active=FALSE WHERE target_uuid=? AND active=" + (db.getType().equals("mysql") ? "TRUE" : "1"),
                target.getUniqueId().toString()
        ).thenCompose(ignored -> {
            String insertMySql = "INSERT INTO " + TABLE + " (target_uuid, target_name, muter_uuid, muter_name, reason, mute_time, expire_time, active) VALUES (?,?,?,?,?,?,?,TRUE)";
            String insertSqlite = "INSERT INTO " + TABLE + " (target_uuid, target_name, muter_uuid, muter_name, reason, mute_time, expire_time, active) VALUES (?,?,?,?,?,?,?,1)";
            boolean mysql = db.getType().equals("mysql");
            return db.updateAsync(
                    mysql ? insertMySql : insertSqlite,
                    target.getUniqueId().toString(),
                    target.getName() != null ? target.getName() : "CONSOLE",
                    muter.getUniqueId().toString(),
                    muter.getName() != null ? muter.getName() : "CONSOLE",
                    finalReason,
                    mysql ? Timestamp.valueOf(now) : now.toEpochSecond(java.time.ZoneOffset.UTC),
                    expire == null ? null : (mysql ? Timestamp.valueOf(expire) : expire.toEpochSecond(java.time.ZoneOffset.UTC))
            ).thenApply(rows -> {
                if (rows > 0) {
                    return new MuteRecord(-1, target.getUniqueId(), target.getName(), muter.getUniqueId(), muter.getName(), finalReason, now, expire, true);
                }
                return null;
            });
        });
    }

    public CompletableFuture<Boolean> unmutePlayer(UUID targetUuid) {
        return db.updateAsync("UPDATE " + TABLE + " SET active=FALSE WHERE target_uuid=? AND active=" + (db.getType().equals("mysql") ? "TRUE" : "1"),
                targetUuid.toString()
        ).thenApply(rows -> rows > 0);
    }

    public CompletableFuture<Optional<MuteRecord>> getActiveMute(UUID targetUuid) {
        String sql = "SELECT * FROM " + TABLE + " WHERE target_uuid=? AND active=" + (db.getType().equals("mysql") ? "TRUE" : "1") +
                " ORDER BY mute_time DESC LIMIT 1";

        return db.queryAsync(sql,
                rs -> {
                    if (rs.next()) {
                        MuteRecord record = map(rs);
                        if (record.isActive() && record.hasExpired()) {
                            // W tle dezaktywuj
                            unmutePlayer(targetUuid);
                            return Optional.empty();
                        }
                        return record.isActive() ? Optional.of(record) : Optional.empty();
                    }
                    return Optional.empty();
                },
                targetUuid.toString()
        );
    }

    private MuteRecord map(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        UUID targetUuid = UUID.fromString(rs.getString("target_uuid"));
        UUID muterUuid = UUID.fromString(rs.getString("muter_uuid"));
        String targetName = rs.getString("target_name");
        String muterName = rs.getString("muter_name");
        String reason = rs.getString("reason");
        boolean mysql = db.getType().equals("mysql");

        LocalDateTime muteTime = mysql
                ? rs.getTimestamp("mute_time").toLocalDateTime()
                : LocalDateTime.ofEpochSecond(rs.getLong("mute_time"), 0, java.time.ZoneOffset.UTC);

        LocalDateTime expireTime;
        if (mysql) {
            Timestamp ts = rs.getTimestamp("expire_time");
            expireTime = ts == null ? null : ts.toLocalDateTime();
        } else {
            long raw = rs.getLong("expire_time");
            expireTime = rs.wasNull() ? null : LocalDateTime.ofEpochSecond(raw, 0, java.time.ZoneOffset.UTC);
        }

        boolean active = mysql ? rs.getBoolean("active") : rs.getInt("active") == 1;

        return new MuteRecord(id, targetUuid, targetName, muterUuid, muterName, reason, muteTime, expireTime, active);
    }
}