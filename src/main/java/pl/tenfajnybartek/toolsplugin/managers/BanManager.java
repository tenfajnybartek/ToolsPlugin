package pl.tenfajnybartek.toolsplugin.managers;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
import pl.tenfajnybartek.toolsplugin.objects.BanRecord;
import pl.tenfajnybartek.toolsplugin.utils.TimeUnit;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BanManager {

    private final ToolsPlugin plugin;
    private final DatabaseManager db;
    private final Pattern TIME_PATTERN = Pattern.compile("(\\d+)([smhdwya])");
    private final String TABLE = "bans";

    public BanManager(ToolsPlugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.db = databaseManager;
        ensureTable();
    }

    private void ensureTable() {
        String sql = db.getType().equals("mysql")
                ? "CREATE TABLE IF NOT EXISTS bans (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "target_uuid VARCHAR(36) NOT NULL," +
                "target_name VARCHAR(16) NOT NULL," +
                "banner_uuid VARCHAR(36) NOT NULL," +
                "banner_name VARCHAR(16) NOT NULL," +
                "ban_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "expire_time TIMESTAMP NULL," +
                "reason TEXT NOT NULL," +
                "active BOOLEAN NOT NULL DEFAULT TRUE," +
                "INDEX (target_uuid), INDEX (active)" +
                ")"
                : "CREATE TABLE IF NOT EXISTS bans (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "target_uuid TEXT NOT NULL," +
                "target_name TEXT NOT NULL," +
                "banner_uuid TEXT NOT NULL," +
                "banner_name TEXT NOT NULL," +
                "ban_time INTEGER NOT NULL," +
                "expire_time INTEGER NULL," +
                "reason TEXT NOT NULL," +
                "active INTEGER NOT NULL DEFAULT 1" +
                ")";
        db.executeUpdate(sql);
    }

    public LocalDateTime parseTime(String timeString) {
        if (timeString == null || timeString.equalsIgnoreCase("perm") || timeString.equalsIgnoreCase("permanent")) {
            return null;
        }
        long totalSeconds = 0;
        Matcher matcher = TIME_PATTERN.matcher(timeString.toLowerCase());
        while (matcher.find()) {
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);
            for (TimeUnit t : TimeUnit.values()) {
                if (t.getShortcut().equals(unit)) {
                    totalSeconds += value * t.getSeconds();
                    break;
                }
            }
        }
        return totalSeconds <= 0 ? null : LocalDateTime.now().plusSeconds(totalSeconds);
    }

    public CompletableFuture<BanRecord> banPlayer(OfflinePlayer target, Player banner, String timeString, String reason) {
        LocalDateTime expire = parseTime(timeString);
        LocalDateTime now = LocalDateTime.now();
        String finalReason = reason == null || reason.isEmpty() ? "Brak podanego powodu" : reason;

        // Najpierw dezaktywacja poprzednich
        return db.updateAsync("UPDATE " + TABLE + " SET active=FALSE WHERE target_uuid=? AND active=TRUE",
                target.getUniqueId().toString()
        ).thenCompose(ignored -> {
            // Wstaw nowy
            String insertSqlMySql = "INSERT INTO " + TABLE + " (target_uuid, target_name, banner_uuid, banner_name, ban_time, expire_time, reason, active) VALUES (?,?,?,?,?,?,?,TRUE)";
            String insertSqlSqlite = "INSERT INTO " + TABLE + " (target_uuid, target_name, banner_uuid, banner_name, ban_time, expire_time, reason, active) VALUES (?,?,?,?,?,?,?,1)";
            boolean mysql = db.getType().equals("mysql");
            return db.updateAsync(
                    mysql ? insertSqlMySql : insertSqlSqlite,
                    target.getUniqueId().toString(),
                    target.getName(),
                    banner.getUniqueId().toString(),
                    banner.getName(),
                    mysql ? Timestamp.valueOf(now) : now.toEpochSecond(java.time.ZoneOffset.UTC),
                    expire == null ? null : (mysql ? Timestamp.valueOf(expire) : expire.toEpochSecond(java.time.ZoneOffset.UTC)),
                    finalReason
            ).thenApply(rows -> {
                if (rows > 0) {
                    return new BanRecord(
                            target.getUniqueId(),
                            target.getName(),
                            banner.getUniqueId(),
                            banner.getName(),
                            now,
                            expire,
                            finalReason
                    );
                }
                return null;
            });
        });
    }

    public CompletableFuture<Boolean> unbanPlayer(UUID targetUuid) {
        return db.updateAsync("UPDATE " + TABLE + " SET active=FALSE WHERE target_uuid=? AND active=TRUE",
                targetUuid.toString()
        ).thenApply(rows -> rows > 0);
    }

    public CompletableFuture<Optional<BanRecord>> getActiveBan(UUID targetUuid) {
        String sql = db.getType().equals("mysql")
                ? "SELECT * FROM " + TABLE + " WHERE target_uuid=? AND active=TRUE ORDER BY ban_time DESC LIMIT 1"
                : "SELECT * FROM " + TABLE + " WHERE target_uuid=? AND active=1 ORDER BY ban_time DESC LIMIT 1";

        return db.queryAsync(sql,
                rs -> {
                    if (rs.next()) {
                        BanRecord rec = map(rs);
                        if (rec.hasExpired()) {
                            // Dezaktywuj w tle
                            unbanPlayer(targetUuid);
                            return Optional.empty();
                        }
                        return Optional.of(rec);
                    }
                    return Optional.empty();
                },
                targetUuid.toString()
        );
    }

    public CompletableFuture<List<BanRecord>> getAllBans(UUID targetUuid) {
        String sql = "SELECT * FROM " + TABLE + " WHERE target_uuid=? ORDER BY ban_time DESC";
        return db.queryAsync(sql,
                rs -> {
                    List<BanRecord> list = new ArrayList<>();
                    while (rs.next()) {
                        list.add(map(rs));
                    }
                    return list;
                },
                targetUuid.toString()
        );
    }

    private BanRecord map(ResultSet rs) throws SQLException {
        UUID targetUuid = UUID.fromString(rs.getString("target_uuid"));
        UUID bannerUuid = UUID.fromString(rs.getString("banner_uuid"));
        boolean mysql = db.getType().equals("mysql");

        LocalDateTime banTime = mysql
                ? rs.getTimestamp("ban_time").toLocalDateTime()
                : LocalDateTime.ofEpochSecond(rs.getLong("ban_time"), 0, java.time.ZoneOffset.UTC);

        LocalDateTime expireTime;
        if (mysql) {
            Timestamp ts = rs.getTimestamp("expire_time");
            expireTime = ts == null ? null : ts.toLocalDateTime();
        } else {
            long raw = rs.getLong("expire_time");
            expireTime = rs.wasNull() ? null : LocalDateTime.ofEpochSecond(raw, 0, java.time.ZoneOffset.UTC);
        }

        boolean active = mysql ? rs.getBoolean("active") : rs.getInt("active") == 1;

        return new BanRecord(
                rs.getInt("id"),
                targetUuid,
                rs.getString("target_name"),
                bannerUuid,
                rs.getString("banner_name"),
                banTime,
                expireTime,
                rs.getString("reason"),
                active
        );
    }
}