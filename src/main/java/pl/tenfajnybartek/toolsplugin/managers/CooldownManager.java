package pl.tenfajnybartek.toolsplugin.managers;

import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.utils.ColorUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {
    private final ConfigManager configManager;

    // Struktura: UUID gracza -> (nazwa komendy -> timestamp wygaśnięcia)
    private final Map<UUID, Map<String, Long>> cooldowns;

    public CooldownManager(ConfigManager configManager) {
        this.configManager = configManager;
        this.cooldowns = new HashMap<>();
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

        // Sprawdź czy gracz ma jakiekolwiek cooldowny
        if (!cooldowns.containsKey(uuid)) {
            return false;
        }

        Map<String, Long> playerCooldowns = cooldowns.get(uuid);

        // Sprawdź czy ma cooldown na tej konkretnej komendzie
        if (!playerCooldowns.containsKey(command)) {
            return false;
        }

        long expireTime = playerCooldowns.get(command);
        long currentTime = System.currentTimeMillis();

        // Jeśli cooldown wygasł, usuń go
        if (currentTime >= expireTime) {
            playerCooldowns.remove(command);

            // Usuń całą mapę gracza jeśli pusta
            if (playerCooldowns.isEmpty()) {
                cooldowns.remove(uuid);
            }

            return false;
        }

        // Cooldown aktywny
        return true;
    }

    /**
     * Ustawia cooldown dla gracza na danej komendzie
     */
    public void setCooldown(Player player, String command) {
        int cooldownSeconds = configManager.getCooldown(command);

        // Jeśli cooldown = 0, nie ustawiaj
        if (cooldownSeconds <= 0) {
            return;
        }

        UUID uuid = player.getUniqueId();
        long expireTime = System.currentTimeMillis() + (cooldownSeconds * 1000L);

        // Pobierz lub utwórz mapę cooldownów gracza
        cooldowns.putIfAbsent(uuid, new HashMap<>());
        cooldowns.get(uuid).put(command, expireTime);
    }

    /**
     * Pobiera pozostały czas cooldownu w sekundach
     */
    public int getRemainingCooldown(Player player, String command) {
        UUID uuid = player.getUniqueId();

        if (!cooldowns.containsKey(uuid)) {
            return 0;
        }

        Map<String, Long> playerCooldowns = cooldowns.get(uuid);

        if (!playerCooldowns.containsKey(command)) {
            return 0;
        }

        long expireTime = playerCooldowns.get(command);
        long currentTime = System.currentTimeMillis();
        long remaining = expireTime - currentTime;

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
        UUID uuid = player.getUniqueId();

        if (cooldowns.containsKey(uuid)) {
            cooldowns.get(uuid).remove(command);

            // Usuń całą mapę gracza jeśli pusta
            if (cooldowns.get(uuid).isEmpty()) {
                cooldowns.remove(uuid);
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
            return true; // Blokuj wykonanie
        }
        return false; // Pozwól na wykonanie
    }

    /**
     * Czyści wszystkie wygasłe cooldowny (optymalizacja)
     * Można wywołać co jakiś czas (np. co 5 minut)
     */
    public void cleanupExpiredCooldowns() {
        long currentTime = System.currentTimeMillis();
        int removed = 0;

        for (UUID uuid : new HashMap<>(cooldowns).keySet()) {
            Map<String, Long> playerCooldowns = cooldowns.get(uuid);

            // Usuń wygasłe cooldowny
            playerCooldowns.entrySet().removeIf(entry -> entry.getValue() <= currentTime);

            // Jeśli mapa pusta, usuń całego gracza
            if (playerCooldowns.isEmpty()) {
                cooldowns.remove(uuid);
                removed++;
            }
        }

        if (removed > 0) {
            // Opcjonalnie: log do konsoli
            // plugin.getLogger().info("Wyczyszczono " + removed + " graczy z cooldownów");
        }
    }
}
