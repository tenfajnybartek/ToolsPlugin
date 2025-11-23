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
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BanManager {

    private final ToolsPlugin plugin;
    private final DatabaseManager databaseManager;
    private final Pattern TIME_PATTERN = Pattern.compile("(\\d+)([smhdwya])");
    // USUNIĘTO: private final Executor asyncExecutor;

    public BanManager(ToolsPlugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        // USUNIĘTO: this.asyncExecutor = plugin.getAsyncTaskExecutor();
        initializeTable();
    }

    // ====================================================================
    // 1. Inicjalizacja Bazy Danych
    // ====================================================================

    private void initializeTable() {
        String createTableQuery = "CREATE TABLE IF NOT EXISTS bans (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "target_uuid VARCHAR(36) NOT NULL," +
                "target_name VARCHAR(16) NOT NULL," +
                "banner_uuid VARCHAR(36) NOT NULL," +
                "banner_name VARCHAR(16) NOT NULL," +
                "ban_time DATETIME NOT NULL," +
                "expire_time DATETIME NULL," +
                "reason TEXT NOT NULL," +
                "active BOOLEAN NOT NULL DEFAULT TRUE," +
                "INDEX (target_uuid), INDEX (active)" +
                ")";
        try (Connection connection = databaseManager.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(createTableQuery);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Błąd podczas tworzenia tabeli bans", e);
        }
    }

    // ====================================================================
    // 2. Parsowanie Czasu
    // ====================================================================

    public LocalDateTime parseTime(String timeString) {
        if (timeString == null || timeString.equalsIgnoreCase("perm") || timeString.equalsIgnoreCase("permanent")) {
            return null;
        }

        long totalSeconds = 0;
        Matcher matcher = TIME_PATTERN.matcher(timeString.toLowerCase());

        while (matcher.find()) {
            try {
                long value = Long.parseLong(matcher.group(1));
                String unit = matcher.group(2);

                for (TimeUnit timeUnit : TimeUnit.values()) {
                    if (timeUnit.getShortcut().equals(unit)) {
                        totalSeconds += value * timeUnit.getSeconds();
                        break;
                    }
                }
            } catch (NumberFormatException ignored) { }
        }

        if (totalSeconds <= 0) {
            return null;
        }

        return LocalDateTime.now().plusSeconds(totalSeconds);
    }

    // ====================================================================
    // 3. Logika Banowania / Odbanowywania (ASYNCHRONICZNA)
    // ====================================================================

    public CompletableFuture<BanRecord> banPlayer(OfflinePlayer target, Player banner, String timeString, String reason) {
        return CompletableFuture.supplyAsync(() -> {
            LocalDateTime expireTime = parseTime(timeString);
            LocalDateTime banTime = LocalDateTime.now();
            String resolvedReason = reason.isEmpty() ? "Brak podanego powodu" : reason;

            BanRecord record = new BanRecord(
                    target.getUniqueId(),
                    target.getName(),
                    banner.getUniqueId(),
                    banner.getName(),
                    banTime,
                    expireTime,
                    resolvedReason
            );

            try (Connection conn = databaseManager.getConnection()) {
                // 1. Dezaktywuj wszystkie poprzednie aktywne bany
                String deactivateQuery = "UPDATE bans SET active = FALSE WHERE target_uuid = ? AND active = TRUE";
                try (PreparedStatement ds = conn.prepareStatement(deactivateQuery)) {
                    ds.setString(1, target.getUniqueId().toString());
                    ds.executeUpdate();
                }

                // 2. Wstaw nowy rekord bana
                String insertQuery = "INSERT INTO bans (target_uuid, target_name, banner_uuid, banner_name, ban_time, expire_time, reason, active) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, TRUE)";
                try (PreparedStatement ps = conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, record.getTargetUuid().toString());
                    ps.setString(2, record.getTargetName());
                    ps.setString(3, record.getBannerUuid().toString());
                    ps.setString(4, record.getBannerName());
                    ps.setTimestamp(5, Timestamp.valueOf(record.getBanTime()));

                    if (record.isPermanent()) {
                        ps.setNull(6, Types.TIMESTAMP);
                    } else {
                        ps.setTimestamp(6, Timestamp.valueOf(record.getExpireTime()));
                    }

                    ps.setString(7, record.getReason());
                    ps.executeUpdate();

                    return record;
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Błąd podczas banowania gracza " + target.getName(), e);
                return null;
            }
        }, ToolsPlugin.getExecutor()); // POPRAWIONE WYWOŁANIE
    }

    public CompletableFuture<Boolean> unbanPlayer(UUID targetUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = databaseManager.getConnection()) {
                String updateQuery = "UPDATE bans SET active = FALSE WHERE target_uuid = ? AND active = TRUE";
                try (PreparedStatement ps = conn.prepareStatement(updateQuery)) {
                    ps.setString(1, targetUuid.toString());
                    return ps.executeUpdate() > 0;
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Błąd podczas odbanowywania gracza " + targetUuid, e);
                return false;
            }
        }, ToolsPlugin.getExecutor()); // POPRAWIONE WYWOŁANIE
    }

    // ====================================================================
    // 4. Logika Sprawdzania i Informacji
    // ====================================================================

    public CompletableFuture<Optional<BanRecord>> getActiveBan(UUID targetUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = databaseManager.getConnection()) {
                String query = "SELECT * FROM bans WHERE target_uuid = ? AND active = TRUE ORDER BY ban_time DESC LIMIT 1";
                try (PreparedStatement ps = conn.prepareStatement(query)) {
                    ps.setString(1, targetUuid.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            BanRecord record = mapResultSetToBanRecord(rs);

                            // Sprawdzenie, czy ban wygasł
                            if (record.hasExpired()) {
                                // Deaktywuj ban asynchronicznie, ale nie czekaj na jego zakończenie
                                unbanPlayer(targetUuid);
                                return Optional.empty(); // Zwróć pusty Optional, ban jest nieaktywny
                            }
                            return Optional.of(record); // Ban jest aktywny
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Błąd podczas sprawdzania bana dla " + targetUuid, e);
            }
            return Optional.empty();
        }, ToolsPlugin.getExecutor()); // POPRAWIONE WYWOŁANIE
    }

    public CompletableFuture<List<BanRecord>> getAllBans(UUID targetUuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<BanRecord> records = new ArrayList<>();
            try (Connection conn = databaseManager.getConnection()) {
                String query = "SELECT * FROM bans WHERE target_uuid = ? ORDER BY ban_time DESC";
                try (PreparedStatement ps = conn.prepareStatement(query)) {
                    ps.setString(1, targetUuid.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            records.add(mapResultSetToBanRecord(rs));
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Błąd podczas pobierania wszystkich banów dla " + targetUuid, e);
            }
            return records;
        }, ToolsPlugin.getExecutor()); // POPRAWIONE WYWOŁANIE
    }

    private BanRecord mapResultSetToBanRecord(ResultSet rs) throws SQLException {
        UUID targetUuid = UUID.fromString(rs.getString("target_uuid"));
        UUID bannerUuid = UUID.fromString(rs.getString("banner_uuid"));

        LocalDateTime banTime = rs.getTimestamp("ban_time").toLocalDateTime();
        LocalDateTime expireTime = rs.getTimestamp("expire_time") != null ? rs.getTimestamp("expire_time").toLocalDateTime() : null;

        return new BanRecord(
                rs.getInt("id"),
                targetUuid,
                rs.getString("target_name"),
                bannerUuid,
                rs.getString("banner_name"),
                banTime,
                expireTime,
                rs.getString("reason"),
                rs.getBoolean("active")
        );
    }
}