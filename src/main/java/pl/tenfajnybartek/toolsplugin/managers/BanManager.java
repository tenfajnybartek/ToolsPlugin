package pl.tenfajnybartek.toolsplugin.managers;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
import pl.tenfajnybartek.toolsplugin.objects.BanRecord;
import pl.tenfajnybartek.toolsplugin.utils.TimeUnit;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Level;

public class BanManager {

    private final ToolsPlugin plugin;
    private final DatabaseManager databaseManager;

    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)([smhdwy])", Pattern.CASE_INSENSITIVE);
    private static final String TABLE = "bans";

    public BanManager(ToolsPlugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        initializeTable();
    }

    private void initializeTable() {
        if (databaseManager.isDisabled()) {
            plugin.getLogger().warning("Database disabled – pomijam tworzenie tabeli bans.");
            return;
        }
        // Jeśli tworzone centralnie, usuń ten blok.
        String createTableQuery = "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "target_uuid VARCHAR(36) NOT NULL," +
                "target_name VARCHAR(16) NOT NULL," +
                "banner_uuid VARCHAR(36) NOT NULL," +
                "banner_name VARCHAR(16) NOT NULL," +
                "ban_time DATETIME NOT NULL," +
                "expire_time DATETIME NULL," +
                "reason TEXT NOT NULL," +
                "active BOOLEAN NOT NULL DEFAULT TRUE," +
                "INDEX idx_target_uuid (target_uuid)," +
                "INDEX idx_active (active)" +
                ")";
        try (Connection connection = databaseManager.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(createTableQuery);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Błąd podczas tworzenia tabeli bans", e);
        }
    }

    public LocalDateTime parseTime(String timeString) {
        if (timeString == null ||
                timeString.equalsIgnoreCase("perm") ||
                timeString.equalsIgnoreCase("permanent")) {
            return null;
        }

        long totalSeconds = 0;
        Matcher matcher = TIME_PATTERN.matcher(timeString.replace(" ", "").toLowerCase());

        while (matcher.find()) {
            long value;
            try {
                value = Long.parseLong(matcher.group(1));
            } catch (NumberFormatException ignored) {
                continue;
            }
            String unit = matcher.group(2);
            for (TimeUnit tu : TimeUnit.values()) {
                if (tu.getShortcut().equalsIgnoreCase(unit)) {
                    totalSeconds += value * tu.getSeconds();
                    break;
                }
            }
        }

        if (totalSeconds <= 0) return null;
        return LocalDateTime.now().plusSeconds(totalSeconds);
    }

    public CompletableFuture<BanRecord> banPlayer(OfflinePlayer target,
                                                  Player banner,
                                                  String timeString,
                                                  String reason) {
        return CompletableFuture.supplyAsync(() -> {
            if (databaseManager.isDisabled()) {
                throw new CompletionException(new IllegalStateException("Baza danych wyłączona – nie można banować."));
            }

            LocalDateTime expireTime = parseTime(timeString);
            LocalDateTime banTime = LocalDateTime.now();
            String resolvedReason = (reason == null || reason.isBlank()) ? "Brak podanego powodu" : reason.trim();
            String targetName = safeName(target.getName(), target.getUniqueId());
            String bannerName = safeName(banner.getName(), banner.getUniqueId());

            try (Connection conn = databaseManager.getConnection()) {
                try (PreparedStatement deactivate = conn.prepareStatement(
                        "UPDATE " + TABLE + " SET active = FALSE WHERE target_uuid = ? AND active = TRUE")) {
                    deactivate.setString(1, target.getUniqueId().toString());
                    deactivate.executeUpdate();
                }

                String insert = "INSERT INTO " + TABLE +
                        " (target_uuid, target_name, banner_uuid, banner_name, ban_time, expire_time, reason, active) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, TRUE)";
                try (PreparedStatement ps = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, target.getUniqueId().toString());
                    ps.setString(2, targetName);
                    ps.setString(3, banner.getUniqueId().toString());
                    ps.setString(4, bannerName);
                    ps.setTimestamp(5, Timestamp.valueOf(banTime));
                    if (expireTime == null) {
                        ps.setNull(6, Types.TIMESTAMP);
                    } else {
                        ps.setTimestamp(6, Timestamp.valueOf(expireTime));
                    }
                    ps.setString(7, resolvedReason);

                    ps.executeUpdate();

                    int id = -1;
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            id = rs.getInt(1);
                        }
                    }

                    return new BanRecord(
                            id,
                            target.getUniqueId(),
                            targetName,
                            banner.getUniqueId(),
                            bannerName,
                            banTime,
                            expireTime,
                            resolvedReason,
                            true
                    );
                }

            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE,
                        "Błąd podczas banowania gracza " + targetName + " (" + target.getUniqueId() + ")", e);
                throw new CompletionException(e);
            }
        }, ToolsPlugin.getExecutor());
    }

    public CompletableFuture<Boolean> unbanPlayer(UUID targetUuid) {
        return CompletableFuture.supplyAsync(() -> {
            if (databaseManager.isDisabled()) return false;
            String sql = "UPDATE " + TABLE + " SET active = FALSE WHERE target_uuid = ? AND active = TRUE";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, targetUuid.toString());
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Błąd podczas odbanowywania gracza " + targetUuid, e);
                return false;
            }
        }, ToolsPlugin.getExecutor());
    }

    public CompletableFuture<Optional<BanRecord>> getActiveBan(UUID targetUuid) {
        return CompletableFuture.supplyAsync(() -> {
            if (databaseManager.isDisabled()) return Optional.empty();
            String sql = "SELECT * FROM " + TABLE +
                    " WHERE target_uuid = ? AND active = TRUE ORDER BY ban_time DESC LIMIT 1";

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, targetUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        BanRecord record = map(rs);
                        if (record.hasExpired()) {
                            // Fire & forget
                            unbanPlayer(targetUuid);
                            return Optional.empty();
                        }
                        return Optional.of(record);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Błąd podczas sprawdzania bana dla " + targetUuid, e);
            }
            return Optional.empty();
        }, ToolsPlugin.getExecutor());
    }

    public CompletableFuture<List<BanRecord>> getAllBans(UUID targetUuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<BanRecord> list = new ArrayList<>();
            if (databaseManager.isDisabled()) return list;

            String sql = "SELECT * FROM " + TABLE + " WHERE target_uuid = ? ORDER BY ban_time DESC";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, targetUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(map(rs));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Błąd podczas pobierania banów dla " + targetUuid, e);
            }
            return list;
        }, ToolsPlugin.getExecutor());
    }

    public CompletableFuture<Boolean> isCurrentlyBanned(UUID targetUuid) {
        return getActiveBan(targetUuid).thenApply(Optional::isPresent);
    }

    public CompletableFuture<Integer> countActiveBans(UUID targetUuid) {
        return CompletableFuture.supplyAsync(() -> {
            if (databaseManager.isDisabled()) return 0;
            String sql = "SELECT COUNT(*) FROM " + TABLE + " WHERE target_uuid = ? AND active = TRUE";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, targetUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Błąd countActiveBans dla " + targetUuid, e);
            }
            return 0;
        }, ToolsPlugin.getExecutor());
    }

    private BanRecord map(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        UUID targetUuid = UUID.fromString(rs.getString("target_uuid"));
        UUID bannerUuid = UUID.fromString(rs.getString("banner_uuid"));
        String targetName = rs.getString("target_name");
        String bannerName = rs.getString("banner_name");
        String reason = rs.getString("reason");
        boolean active = rs.getBoolean("active");

        Timestamp banTs = rs.getTimestamp("ban_time");
        Timestamp expTs = rs.getTimestamp("expire_time");
        LocalDateTime banTime = banTs != null ? banTs.toLocalDateTime() : LocalDateTime.now();
        LocalDateTime expireTime = expTs != null ? expTs.toLocalDateTime() : null;

        return new BanRecord(
                id,
                targetUuid,
                targetName,
                bannerUuid,
                bannerName,
                banTime,
                expireTime,
                reason,
                active
        );
    }

    private String safeName(String raw, UUID uuid) {
        if (raw == null || raw.isBlank()) {
            return uuid.toString().substring(0, 8);
        }
        return raw;
    }
}