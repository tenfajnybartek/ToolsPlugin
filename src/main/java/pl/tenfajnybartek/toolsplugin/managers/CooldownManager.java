package pl.tenfajnybartek.toolsplugin.managers;

import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.utils.ColorUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {
    private final ConfigManager configManager;

    // Zmiana na ConcurrentHashMap dla bezpieczeństwa wątkowego
    private final Map<UUID, Map<String, Long>> cooldowns;

    public CooldownManager(ConfigManager configManager) {
        this.configManager = configManager;
        // Używamy ConcurrentHashMap dla bezpieczeństwa wątkowego
        this.cooldowns = new ConcurrentHashMap<>();
    }

    /**
     * Sprawdza czy gracz ma cooldown na danej komendzie
     * @return true jeśli ma cooldown (nie może użyć), false jeśli może użyć
     */
    public boolean hasCooldown(Player player, String command) {
        // Sprawdź czy cooldowny są włączone
        if (!configManager.isCooldownsEnabled()) {
            return false;
        }

        // Sprawdź czy gracz ma bypass
        if (player.hasPermission(configManager.getCooldownBypassPermission())) {
            return false;
        }

        UUID uuid = player.getUniqueId();
        Map<String, Long> playerCooldowns = cooldowns.get(uuid);

        if (playerCooldowns == null) {
            return false;
        }

        // Używamy computeIfPresent, aby bezpiecznie usunąć wygasłe cooldowny w mapie gracza
        return playerCooldowns.computeIfPresent(command, (cmd, expireTime) -> {
            long currentTime = System.currentTimeMillis();

            if (currentTime >= expireTime) {
                // Cooldown wygasł, usuwamy go (zwracamy null)
                return null;
            } else {
                // Cooldown aktywny, zachowujemy
                return expireTime;
            }
        }) != null;
    }

    /**
     * Ustawia cooldown dla gracza na danej komendzie
     */
    public void setCooldown(Player player, String command) {
        int cooldownSeconds = configManager.getCooldown(command);

        if (cooldownSeconds <= 0) {
            return;
        }

        UUID uuid = player.getUniqueId();
        long expireTime = System.currentTimeMillis() + (cooldownSeconds * 1000L);

        // Używamy computeIfAbsent i put, aby bezpiecznie ustawić wartość
        cooldowns.putIfAbsent(uuid, new ConcurrentHashMap<>());
        cooldowns.get(uuid).put(command, expireTime);
    }

    /**
     * Pobiera pozostały czas cooldownu w sekundach
     */
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

        // Konwersja na sekundy (zaokrąglenie w górę)
        return (int) Math.ceil(remaining / 1000.0);
    }

    /**
     * Ręcznie usuwa cooldown (np. przez admina)
     */
    public void removeCooldown(Player player, String command) {
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());

        if (playerCooldowns != null) {
            playerCooldowns.remove(command);

            // OPTYMALIZACJA: Usuń całego gracza jeśli mapa pusta
            if (playerCooldowns.isEmpty()) {
                cooldowns.remove(player.getUniqueId());
            }
        }
    }

    /**
     * Usuwa wszystkie cooldowny gracza
     */
    public void clearPlayerCooldowns(Player player) {
        cooldowns.remove(player.getUniqueId());
    }

    /**
     * Wysyła wiadomość o cooldownie do gracza
     */
    public void sendCooldownMessage(Player player, String command) {
        int remaining = getRemainingCooldown(player, command);
        String message = configManager.getPrefix() + "&cMusisz poczekać jeszcze &e" + remaining + "s &cprzed ponownym użyciem tej komendy!";
        player.sendMessage(ColorUtils.colorize(message));
    }

    /**
     * Sprawdza cooldown i wysyła wiadomość jeśli aktywny
     * @return true jeśli cooldown aktywny (blokuj wykonanie), false jeśli może wykonać
     */
    public boolean checkCooldown(Player player, String command) {
        if (hasCooldown(player, command)) {
            sendCooldownMessage(player, command);
            return true;
        }
        return false;
    }

    /**
     * Czyści wszystkie wygasłe cooldowny (optymalizacja)
     */
    public void cleanupExpiredCooldowns() {
        long currentTime = System.currentTimeMillis();

        // Iteracja jest bezpieczna dzięki ConcurrentHashMap
        cooldowns.forEach((uuid, playerCooldowns) -> {

            // Usuń wygasłe cooldowny z wewnętrznej mapy
            playerCooldowns.entrySet().removeIf(entry -> entry.getValue() <= currentTime);

            // Jeśli mapa pusta, usuń całego gracza
            if (playerCooldowns.isEmpty()) {
                cooldowns.remove(uuid);
            }
        });
    }
}