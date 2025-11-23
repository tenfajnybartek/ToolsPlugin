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

    private final JavaPlugin plugin;
    private final DatabaseManager dbManager;
    private final String TABLE = "mutes";

    public MuteManager(JavaPlugin plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        initializeTable(); // DODANO: Wywołanie tworzenia tabeli przy starcie
    }

    // ====================================================================
    // 1. Inicjalizacja Bazy Danych
    // ====================================================================

    private void initializeTable() {
        String createTableQuery = "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "target_uuid VARCHAR(36) NOT NULL," +
                "target_name VARCHAR(16) NOT NULL," +
                "muter_uuid VARCHAR(36) NOT NULL," +
                "muter_name VARCHAR(16) NOT NULL," +
                "mute_time DATETIME NOT NULL," +
                "expire_time DATETIME NULL," +
                "reason TEXT NOT NULL," +
                "active BOOLEAN NOT NULL DEFAULT TRUE," +
                "INDEX (target_uuid), INDEX (active)" +
                ")";

        try (Connection connection = dbManager.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(createTableQuery);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Błąd podczas tworzenia tabeli " + TABLE, e);
        }
    }

    // ====================================================================
    // 2. Logika Mutowania
    // ====================================================================

    /**
     * Wycisza gracza asynchronicznie i zwraca rekord.
     */
    public CompletableFuture<MuteRecord> mutePlayer(OfflinePlayer target, OfflinePlayer muter, String timeString, String reason) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dbManager.getConnection()) {

                // 1. Obliczenie czasu wygaśnięcia
                LocalDateTime muteTime = LocalDateTime.now();
                // WAŻNE: parseTime musi być dostępne statycznie w TimeUtils
                LocalDateTime expireTime = TimeUtils.parseTime(timeString);

                // 2. Dezaktywacja wszystkich aktywnych mutów gracza (zawsze nowy mut jest najważniejszy)
                deactivateActiveMutes(conn, target.getUniqueId());

                // 3. Stworzenie nowego rekordu
                String sqlInsert = "INSERT INTO " + TABLE +
                        " (target_uuid, target_name, muter_uuid, muter_name, reason, mute_time, expire_time, active)" +
                        " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

                try (PreparedStatement ps = conn.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, target.getUniqueId().toString());
                    ps.setString(2, target.getName() != null ? target.getName() : "CONSOLE");
                    ps.setString(3, muter.getUniqueId().toString());
                    ps.setString(4, muter.getName() != null ? muter.getName() : "CONSOLE");
                    ps.setString(5, reason);
                    ps.setTimestamp(6, Timestamp.valueOf(muteTime));

                    // Ustawianie expire_time: NULL dla permanentnych, Timestamp dla czasowych
                    if (expireTime == null) {
                        ps.setNull(7, Types.TIMESTAMP);
                    } else {
                        ps.setTimestamp(7, Timestamp.valueOf(expireTime));
                    }

                    ps.setBoolean(8, true);
                    ps.executeUpdate();

                    // 4. Pobranie ID nowo wstawionego rekordu
                    int id = -1;
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            id = rs.getInt(1);
                        }
                    }

                    // 5. Zwrócenie obiektu MuteRecord
                    return new MuteRecord(id, target.getUniqueId(), target.getName(), muter.getUniqueId(), muter.getName(), reason, muteTime, expireTime, true);

                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Błąd podczas wyciszania gracza " + target.getName(), e);
                return null;
            }
        }, ToolsPlugin.getExecutor());
    }

    /**
     * Odcisza gracza poprzez dezaktywację wszystkich aktywnych mutów.
     */
    public CompletableFuture<Boolean> unmutePlayer(UUID targetUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dbManager.getConnection()) {
                return deactivateActiveMutes(conn, targetUuid);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Błąd podczas odciszania gracza " + targetUuid, e);
                return false;
            }
        }, ToolsPlugin.getExecutor());
    }

    /**
     * Wewnętrzna metoda dezaktywująca aktywne muty.
     */
    private boolean deactivateActiveMutes(Connection conn, UUID targetUuid) throws SQLException {
        // Ta metoda była źródłem błędu 'Unknown column'
        String sqlUpdate = "UPDATE " + TABLE + " SET active = FALSE WHERE target_uuid = ? AND active = TRUE";

        try (PreparedStatement ps = conn.prepareStatement(sqlUpdate)) {
            ps.setString(1, targetUuid.toString());
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        }
    }

    /**
     * Pobiera aktywny rekord wyciszenia dla gracza. Jeśli wygasł, dezaktywuje go w DB.
     * @return Opcjonalnie MuteRecord, jeśli aktywny.
     */
    public CompletableFuture<Optional<MuteRecord>> getActiveMute(UUID targetUuid) {
        return CompletableFuture.supplyAsync(() -> {
            MuteRecord activeRecord = null;
            String sqlSelect = "SELECT * FROM " + TABLE +
                    " WHERE target_uuid = ? AND active = TRUE" +
                    " ORDER BY mute_time DESC LIMIT 1";

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sqlSelect)) {

                ps.setString(1, targetUuid.toString());

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        activeRecord = mapResultSetToMuteRecord(rs);

                        // Sprawdzenie, czy mute wygasł
                        if (activeRecord != null && !activeRecord.isActive()) {
                            // Mute wygasł, dezaktywuj go asynchronicznie i ustaw aktywny rekord na null
                            deactivateExpiredMute(activeRecord.getId());
                            return Optional.empty();
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Błąd podczas sprawdzania muta dla " + targetUuid, e);
                return Optional.empty();
            }
            return Optional.ofNullable(activeRecord);
        }, ToolsPlugin.getExecutor());
    }

    /**
     * Asynchroniczna dezaktywacja wygasłego muta.
     */
    private void deactivateExpiredMute(int recordId) {
        // Używamy CompletableFuture, aby nie blokować wątku getActiveMute
        CompletableFuture.runAsync(() -> {
            String sqlUpdate = "UPDATE " + TABLE + " SET active = FALSE WHERE id = ?";
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sqlUpdate)) {

                ps.setInt(1, recordId);
                ps.executeUpdate();
                plugin.getLogger().info("Dezaktywowano wygasły mute: ID " + recordId);

            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Błąd podczas dezaktywacji muta ID " + recordId, e);
            }
        }, ToolsPlugin.getExecutor());
    }

    /**
     * Mapuje ResultSet na obiekt MuteRecord.
     */
    private MuteRecord mapResultSetToMuteRecord(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        UUID targetUuid = UUID.fromString(rs.getString("target_uuid"));
        String targetName = rs.getString("target_name");

        String muterUuidStr = rs.getString("muter_uuid");
        UUID muterUuid = muterUuidStr != null ? UUID.fromString(muterUuidStr) : null;

        String muterName = rs.getString("muter_name");
        String reason = rs.getString("reason");
        Timestamp muteTimestamp = rs.getTimestamp("mute_time");
        Timestamp expireTimestamp = rs.getTimestamp("expire_time");
        boolean active = rs.getBoolean("active");

        LocalDateTime muteTime = muteTimestamp.toLocalDateTime();
        LocalDateTime expireTime = expireTimestamp != null ? expireTimestamp.toLocalDateTime() : null;

        return new MuteRecord(id, targetUuid, targetName, muterUuid, muterName, reason, muteTime, expireTime, active);
    }
}
