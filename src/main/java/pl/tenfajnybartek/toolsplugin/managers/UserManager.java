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
    private final DatabaseManager databaseManager;

    private final Map<UUID, User> users;

    public UserManager(ToolsPlugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.users = new HashMap<>();
        initializeTable(); // DODANO: Inicjalizacja tabeli przy starcie
    }

    /**
     * Inicjalizuje tabelę 'users' w bazie danych.
     */
    private void initializeTable() {
        String createTableQuery = "CREATE TABLE IF NOT EXISTS users (" +
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
                ")";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(createTableQuery)) {
            statement.execute();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Błąd podczas tworzenia tabeli users!", e);
        }
    }


    /**
     * Ładuje użytkownika z bazy danych asynchronicznie, używając ExecutorService.
     */
    public void loadUser(Player player) {
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        String ip = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "unknown";

        // ASYNCHRONICZNE POBIERANIE DANYCH Z DB
        CompletableFuture.supplyAsync(() -> {
                    try (Connection conn = databaseManager.getConnection()) {
                        String query = "SELECT * FROM users WHERE uuid = ?";
                        PreparedStatement stmt = conn.prepareStatement(query);
                        stmt.setString(1, uuid.toString());

                        ResultSet rs = stmt.executeQuery();

                        if (rs.next()) {
                            // Gracz istnieje w bazie - załaduj dane
                            long firstJoin = rs.getLong("first_join");
                            long lastJoin = rs.getLong("last_join");
                            long lastQuit = rs.getLong("last_quit");
                            String lastMsgUuid = rs.getString("last_message_from");
                            UUID lastMessageFrom = lastMsgUuid != null ? UUID.fromString(lastMsgUuid) : null;

                            boolean tpToggle = rs.getBoolean("teleport_toggle");
                            boolean msgToggle = rs.getBoolean("msg_toggle");
                            boolean socialSpy = rs.getBoolean("social_spy");

                            // Użycie Konstruktora B
                            User user = new User(uuid, name, ip, firstJoin, lastJoin, lastQuit, lastMessageFrom, tpToggle, msgToggle, socialSpy);

                            // Aktualizacje danych nie wpływające na DB, ale na stan obiektowy
                            user.updateLastJoin();
                            user.setIp(ip);
                            user.setName(name);

                            return user;

                        } else {
                            // Nowy gracz - utwórz
                            User user = new User(uuid, name, ip);
                            plugin.getLogger().info("Utworzono nowego użytkownika: " + name);
                            return user;
                        }

                    } catch (SQLException e) {
                        plugin.getLogger().log(Level.SEVERE, "Błąd podczas ładowania użytkownika: " + name, e);
                        return null;
                    }
                }, ToolsPlugin.getExecutor()) // Używamy naszego Executora
                .thenAccept(user -> {
                    // SYNCHRONICZNE UMIESZCZENIE W MAPIE i ZAPIS
                    if (user != null) {
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            users.put(uuid, user);
                            // Zapis asynchroniczny po załadowaniu (dla nowych graczy/pól)
                            saveUser(user, false);
                        });
                    }
                });
    }

    /**
     * Zapisuje użytkownika do bazy danych
     * @param user Użytkownik do zapisania
     * @param isShutdownSave true, jeśli jest to wywołanie podczas wyłączania (wymusza zapis synchroniczny)
     */
    public void saveUser(User user, boolean isShutdownSave) {
        if (isShutdownSave) {
            // SYNCHRONICZNY ZAPIS (WYMAGANY PRZY SHUTDOWN)
            executeSaveLogic(user);
        } else {
            // ASYNCHRONICZNY ZAPIS, używamy Executora
            CompletableFuture.runAsync(() -> executeSaveLogic(user), ToolsPlugin.getExecutor());
        }
    }

    /**
     * Logika zapisu do bazy danych
     */
    private void executeSaveLogic(User user) {
        try (Connection conn = databaseManager.getConnection()) {
            String query = "INSERT INTO users (uuid, name, ip, first_join, last_join, last_quit, last_message_from, teleport_toggle, msg_toggle, social_spy) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "name = VALUES(name), " +
                    "ip = VALUES(ip), " +
                    "last_join = VALUES(last_join), " +
                    "last_quit = VALUES(last_quit), " +
                    "last_message_from = VALUES(last_message_from), " +
                    "teleport_toggle = VALUES(teleport_toggle), " +
                    "msg_toggle = VALUES(msg_toggle), " +
                    "social_spy = VALUES(social_spy)";

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, user.getUuid().toString());
            stmt.setString(2, user.getName());
            stmt.setString(3, user.getIp());
            stmt.setLong(4, user.getFirstJoin());
            stmt.setLong(5, user.getLastJoin());
            stmt.setLong(6, user.getLastQuit());

            // Last Message From
            stmt.setString(7, user.getLastMessageFrom() != null ? user.getLastMessageFrom().toString() : null);

            // Pola Boolean
            stmt.setBoolean(8, user.isTeleportToggle());
            stmt.setBoolean(9, user.isMsgToggle());
            stmt.setBoolean(10, user.isSocialSpy());

            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Błąd podczas zapisywania użytkownika: " + user.getName(), e);
        }
    }

    /**
     * Pobiera użytkownika z DB po nazwie. Używa callback do zwrócenia wyniku na wątku głównym.
     */
    public void getUserByName(String name, UserCallback callback) {
        // ASYNCHRONICZNE POBIERANIE
        CompletableFuture.supplyAsync(() -> {
                    try (Connection conn = databaseManager.getConnection()) {
                        String query = "SELECT * FROM users WHERE name = ? ORDER BY last_join DESC LIMIT 1";
                        PreparedStatement stmt = conn.prepareStatement(query);
                        stmt.setString(1, name);

                        ResultSet rs = stmt.executeQuery();

                        if (rs.next()) {
                            UUID uuid = UUID.fromString(rs.getString("uuid"));
                            String ip = rs.getString("ip");
                            long firstJoin = rs.getLong("first_join");
                            long lastJoin = rs.getLong("last_join");
                            long lastQuit = rs.getLong("last_quit");
                            String lastMsgUuid = rs.getString("last_message_from");
                            UUID lastMessageFrom = lastMsgUuid != null ? UUID.fromString(lastMsgUuid) : null;

                            boolean tpToggle = rs.getBoolean("teleport_toggle");
                            boolean msgToggle = rs.getBoolean("msg_toggle");
                            boolean socialSpy = rs.getBoolean("social_spy");

                            // Użycie Konstruktora B
                            return new User(uuid, name, ip, firstJoin, lastJoin, lastQuit, lastMessageFrom, tpToggle, msgToggle, socialSpy);
                        }
                        return null;

                    } catch (SQLException e) {
                        plugin.getLogger().log(Level.SEVERE, "Błąd podczas pobierania użytkownika po nazwie: " + name, e);
                        return null;
                    }
                }, ToolsPlugin.getExecutor())
                .thenAccept(user -> {
                    // ZWRÓCENIE WYNIKU NA GŁÓWNYM WĄTKU POPRZEZ CALLBACK
                    plugin.getServer().getScheduler().runTask(plugin, () -> callback.onUserLoaded(user));
                });
    }

    // --- Reszta metod ---

    public void unloadUser(Player player) {
        UUID uuid = player.getUniqueId();
        User user = users.get(uuid);

        if (user != null) {
            user.updateLastQuit();
            saveUser(user, false);
            users.remove(uuid);
        }
    }

    public User getUser(Player player) {
        return users.get(player.getUniqueId());
    }

    public User getUser(UUID uuid) {
        return users.get(uuid);
    }

    public boolean hasUser(Player player) {
        return users.containsKey(player.getUniqueId());
    }

    /**
     * Zapisuje wszystkich użytkowników (przy wyłączeniu serwera)
     */
    public void saveAllUsers() {
        plugin.getLogger().info("Zapisywanie " + users.size() + " użytkowników...");

        for (User user : users.values()) {
            // Ustawia ostatni czas wyjścia, jeśli gracz jest jeszcze online
            if (user.isOnline()) {
                user.updateLastQuit();
            }
            saveUser(user, true); // Zapis synchroniczny
        }

        users.clear();
        plugin.getLogger().info("Wszyscy użytkownicy zostali zapisani!");
    }

    public interface UserCallback {
        void onUserLoaded(User user);
    }
}