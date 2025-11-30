package pl.tenfajnybartek.toolsplugin.objects;

import net.kyori.adventure.text.Component;
import pl.tenfajnybartek.toolsplugin.utils.TimeUtils;

import java.time.Duration;
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
    private final LocalDateTime expireTime; // null = permanentny
    private boolean active;

    public MuteRecord(UUID targetUuid, String targetName, UUID muterUuid, String muterName,
                      String reason, LocalDateTime muteTime, LocalDateTime expireTime) {
        this.id = -1;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.muterUuid = muterUuid;
        this.muterName = muterName;
        this.reason = reason;
        this.muteTime = muteTime;
        this.expireTime = expireTime;
        this.active = true;
    }

    public MuteRecord(int id, UUID targetUuid, String targetName, UUID muterUuid, String muterName,
                      String reason, LocalDateTime muteTime, LocalDateTime expireTime, boolean active) {
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

    public boolean isPermanent() {
        return expireTime == null;
    }

    public boolean hasExpired() {
        if (!active) return false;
        if (isPermanent()) return false;
        return expireTime.isBefore(LocalDateTime.now());
    }

    public boolean isActive() {
        if (!active) return false;
        if (isPermanent()) return true;
        return !hasExpired();
    }

    public boolean getActiveStatus() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public long getRemainingSeconds() {
        if (!isActive()) return 0;
        if (isPermanent()) return -1;
        return Math.max(0, Duration.between(LocalDateTime.now(), expireTime).getSeconds());
    }

    public Component getMuteMessage() {
        return TimeUtils.getMuteMessage(this);
    }

    public int getId() { return id; }
    public UUID getTargetUuid() { return targetUuid; }
    public String getTargetName() { return targetName; }
    public UUID getMuterUuid() { return muterUuid; }
    public String getMuterName() { return muterName; }
    public String getReason() { return reason; }
    public LocalDateTime getMuteTime() { return muteTime; }
    public LocalDateTime getExpireTime() { return expireTime; }
}