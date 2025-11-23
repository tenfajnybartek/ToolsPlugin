package pl.tenfajnybartek.toolsplugin.objects;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class User {
    private final UUID uuid;
    private String name;
    private String ip;
    private long firstJoin;
    private long lastJoin;
    private long lastQuit;
    private UUID lastMessageFrom;
    private boolean teleportToggle;

    // --- POLA DLA SYSTEMU WIADOMOŚCI ---
    private boolean msgToggle;
    private boolean socialSpy;
    // ----------------------------------------

    // KONSTRUKTOR A: Dla NOWEGO gracza (3 argumenty)
    public User(UUID uuid, String name, String ip) {
        this.uuid = uuid;
        this.name = name;
        this.ip = ip;
        this.firstJoin = System.currentTimeMillis();
        this.lastJoin = System.currentTimeMillis();
        this.lastQuit = 0;
        this.lastMessageFrom = null;
        this.teleportToggle = true;

        // Domyślne wartości dla nowych pól
        this.msgToggle = true;
        this.socialSpy = false;
    }

    // KONSTRUKTOR B: Dla ładowania z bazy (ZAKTUALIZOWANY - DODANE WSZYSTKIE POLA)
    public User(UUID uuid, String name, String ip, long firstJoin, long lastJoin, long lastQuit, UUID lastMessageFrom, boolean teleportToggle,
                boolean msgToggle, boolean socialSpy) {
        this.uuid = uuid;
        this.name = name;
        this.ip = ip;
        this.firstJoin = firstJoin;
        this.lastJoin = lastJoin;
        this.lastQuit = lastQuit;
        this.lastMessageFrom = lastMessageFrom;
        this.teleportToggle = teleportToggle;

        // Ustawianie nowych pól
        this.msgToggle = msgToggle;
        this.socialSpy = socialSpy;
    }

    // Gettery i Settery (BEZ ZMIAN)

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public long getFirstJoin() {
        return firstJoin;
    }

    public long getLastJoin() {
        return lastJoin;
    }

    public void setLastJoin(long lastJoin) {
        this.lastJoin = lastJoin;
    }

    public long getLastQuit() {
        return lastQuit;
    }

    public void setLastQuit(long lastQuit) {
        this.lastQuit = lastQuit;
    }

    public UUID getLastMessageFrom() {
        return lastMessageFrom;
    }

    public void setLastMessageFrom(UUID lastMessageFrom) {
        this.lastMessageFrom = lastMessageFrom;
    }

    public boolean isTeleportToggle() {
        return teleportToggle;
    }

    public void setTeleportToggle(boolean teleportToggle) {
        this.teleportToggle = teleportToggle;
    }

    public boolean isMsgToggle() {
        return msgToggle;
    }

    public void setMsgToggle(boolean msgToggle) {
        this.msgToggle = msgToggle;
    }

    public boolean isSocialSpy() {
        return socialSpy;
    }

    public void setSocialSpy(boolean socialSpy) {
        this.socialSpy = socialSpy;
    }

    // --- Metody pomocnicze (ZAKTUALIZOWANE) ---

    /**
     * Zwraca obiekt Player, jeśli gracz jest obecnie online.
     */
    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    /**
     * Sprawdza, czy gracz jest online.
     * @return true, jeśli getPlayer() zwróci nie-null.
     */
    public boolean isOnline() {
        return getPlayer() != null;
    }

    /**
     * Aktualizuje last join na obecny czas
     */
    public void updateLastJoin() {
        this.lastJoin = System.currentTimeMillis();
    }

    /**
     * Aktualizuje last quit na obecny czas
     */
    public void updateLastQuit() {
        this.lastQuit = System.currentTimeMillis();
    }
}