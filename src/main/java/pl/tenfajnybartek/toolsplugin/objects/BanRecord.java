package pl.tenfajnybartek.toolsplugin.objects;

import java.time.LocalDateTime;
import java.util.UUID;

public class BanRecord {

    private final int id;
    private final UUID targetUuid;
    private final String targetName;
    private final UUID bannerUuid;
    private final String bannerName;
    private final LocalDateTime banTime;
    private final LocalDateTime expireTime; // null dla permanentnego
    private final String reason;
    private boolean active;

    // Konstruktor dla NOWEGO bana
    public BanRecord(UUID targetUuid, String targetName, UUID bannerUuid, String bannerName, LocalDateTime banTime, LocalDateTime expireTime, String reason) {
        this.id = -1; // -1 oznacza, że ban nie ma jeszcze ID z bazy
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.bannerUuid = bannerUuid;
        this.bannerName = bannerName;
        this.banTime = banTime;
        this.expireTime = expireTime;
        this.reason = reason;
        this.active = true;
    }

    // Konstruktor dla ładowania z bazy
    public BanRecord(int id, UUID targetUuid, String targetName, UUID bannerUuid, String bannerName, LocalDateTime banTime, LocalDateTime expireTime, String reason, boolean active) {
        this.id = id;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.bannerUuid = bannerUuid;
        this.bannerName = bannerName;
        this.banTime = banTime;
        this.expireTime = expireTime;
        this.reason = reason;
        this.active = active;
    }

    // Gettery
    public int getId() { return id; }
    public UUID getTargetUuid() { return targetUuid; }
    public String getTargetName() { return targetName; }
    public UUID getBannerUuid() { return bannerUuid; }
    public String getBannerName() { return bannerName; }
    public LocalDateTime getBanTime() { return banTime; }
    public LocalDateTime getExpireTime() { return expireTime; }
    public String getReason() { return reason; }
    public boolean isActive() { return active; }

    // Settery
    public void setActive(boolean active) { this.active = active; }

    public boolean isPermanent() {
        return expireTime == null;
    }

    public boolean hasExpired() {
        // 1. Musi być aktywny
        if (!active) {
            return false;
        }
        // 2. Musi mieć czas wygaśnięcia (nie może być permanentny)
        if (isPermanent()) {
            return false;
        }
        // 3. Sprawdzenie czasu
        return expireTime.isBefore(LocalDateTime.now());
    }
}
