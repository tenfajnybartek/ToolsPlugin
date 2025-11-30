package pl.tenfajnybartek.toolsplugin.managers;

import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.utils.ColorUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {
    private final ConfigManager configManager;

    private final Map<UUID, Map<String, Long>> cooldowns;

    public CooldownManager(ConfigManager configManager) {
        this.configManager = configManager;
        this.cooldowns = new ConcurrentHashMap<>();
    }

    public boolean hasCooldown(Player player, String command) {
        if (!configManager.isCooldownsEnabled()) {
            return false;
        }

        if (player.hasPermission(configManager.getCooldownBypassPermission())) {
            return false;
        }

        UUID uuid = player.getUniqueId();
        Map<String, Long> playerCooldowns = cooldowns.get(uuid);

        if (playerCooldowns == null) {
            return false;
        }

        return playerCooldowns.computeIfPresent(command, (cmd, expireTime) -> {
            long currentTime = System.currentTimeMillis();

            if (currentTime >= expireTime) {
                return null;
            } else {
                return expireTime;
            }
        }) != null;
    }

    public void setCooldown(Player player, String command) {
        int cooldownSeconds = configManager.getCooldown(command);

        if (cooldownSeconds <= 0) {
            return;
        }

        UUID uuid = player.getUniqueId();
        long expireTime = System.currentTimeMillis() + (cooldownSeconds * 1000L);

        cooldowns.putIfAbsent(uuid, new ConcurrentHashMap<>());
        cooldowns.get(uuid).put(command, expireTime);
    }

    public int getRemainingCooldown(Player player, String command) {
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());

        if (playerCooldowns == null) {
            return 0;
        }

        Long expireTime = playerCooldowns.get(command);

        if (expireTime == null) {
            return 0;
        }

        long remaining = expireTime - System.currentTimeMillis();

        if (remaining <= 0) {
            return 0;
        }

        return (int) Math.ceil(remaining / 1000.0);
    }

    public void removeCooldown(Player player, String command) {
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());

        if (playerCooldowns != null) {
            playerCooldowns.remove(command);

            if (playerCooldowns.isEmpty()) {
                cooldowns.remove(player.getUniqueId());
            }
        }
    }

    public void clearPlayerCooldowns(Player player) {
        cooldowns.remove(player.getUniqueId());
    }

    public void sendCooldownMessage(Player player, String command) {
        int remaining = getRemainingCooldown(player, command);
        String message = configManager.getPrefix() + "&cMusisz poczekać jeszcze &e" + remaining + "s &cprzed ponownym użyciem tej komendy!";
        player.sendMessage(ColorUtils.colorize(message));
    }

    public boolean checkCooldown(Player player, String command) {
        if (hasCooldown(player, command)) {
            sendCooldownMessage(player, command);
            return true;
        }
        return false;
    }

    public void cleanupExpiredCooldowns() {
        long currentTime = System.currentTimeMillis();

        cooldowns.forEach((uuid, playerCooldowns) -> {

            playerCooldowns.entrySet().removeIf(entry -> entry.getValue() <= currentTime);

            if (playerCooldowns.isEmpty()) {
                cooldowns.remove(uuid);
            }
        });
    }
}