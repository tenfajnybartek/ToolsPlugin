package pl.tenfajnybartek.toolsplugin.objects;

import net.kyori.adventure.text.Component;
import pl.tenfajnybartek.toolsplugin.utils.TimeUtils;

import java.time.LocalDateTime;
import java.util.UUID;

public class MuteRecord {

    private final int id;
    private final UUID targetUuid;
    private final String targetName;
    private final UUID muterUuid;
    private final String muterName;
    private final String reason;
    private final LocalDateTime muteTime;
    private final LocalDateTime expireTime;
    private boolean active;

    // Konstruktor do tworzenia nowego rekordu (np. z komendy)
    public MuteRecord(UUID targetUuid, String targetName, UUID muterUuid, String muterName, String reason, LocalDateTime muteTime, LocalDateTime expireTime) {
        this.id = -1; // -1 oznacza, że rekord nie ma jeszcze ID z bazy
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.muterUuid = muterUuid;
        this.muterName = muterName;
        this.reason = reason;
        this.muteTime = muteTime;
        this.expireTime = expireTime;
        this.active = true;
    }

    // Konstruktor do ładowania istniejącego rekordu z bazy
    public MuteRecord(int id, UUID targetUuid, String targetName, UUID muterUuid, String muterName, String reason, LocalDateTime muteTime, LocalDateTime expireTime, boolean active) {
        this.id = id;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.muterUuid = muterUuid;
        this.muterName = muterName;
        this.reason = reason;
        this.muteTime = muteTime;
        this.expireTime = expireTime;
        this.active = active;
    }

    // Sprawdza, czy wyciszenie jest nadal aktywne
    public boolean isActive() {
        if (!active) {
            return false;
        }
        if (isPermanent()) {
            return true;
        }
        // Sprawdzenie, czy czas wygaśnięcia minął
        return expireTime.isAfter(LocalDateTime.now());
    }

    public boolean isPermanent() {
        return expireTime == null;
    }

    public Component getMuteMessage() {
        return TimeUtils.getMuteMessage(this);
    }

    // Gettery
    public int getId() { return id; }
    public UUID getTargetUuid() { return targetUuid; }
    public String getTargetName() { return targetName; }
    public UUID getMuterUuid() { return muterUuid; }
    public String getMuterName() { return muterName; }
    public String getReason() { return reason; }
    public LocalDateTime getMuteTime() { return muteTime; }
    public LocalDateTime getExpireTime() { return expireTime; }
    public void setActive(boolean active) { this.active = active; }
    public boolean getActiveStatus() { return active; }
}
