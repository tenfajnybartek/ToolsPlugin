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

    private boolean msgToggle;
    private boolean socialSpy;

    public User(UUID uuid, String name, String ip) {
        this.uuid = uuid;
        this.name = name;
        this.ip = ip;
        this.firstJoin = System.currentTimeMillis();
        this.lastJoin = System.currentTimeMillis();
        this.lastQuit = 0;
        this.lastMessageFrom = null;
        this.teleportToggle = true;

        this.msgToggle = true;
        this.socialSpy = false;
    }

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

        this.msgToggle = msgToggle;
        this.socialSpy = socialSpy;
    }


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

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public boolean isOnline() {
        return getPlayer() != null;
    }

    public void updateLastJoin() {
        this.lastJoin = System.currentTimeMillis();
    }

    public void updateLastQuit() {
        this.lastQuit = System.currentTimeMillis();
    }
}