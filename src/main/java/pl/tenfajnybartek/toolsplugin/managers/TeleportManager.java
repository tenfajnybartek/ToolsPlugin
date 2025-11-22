package pl.tenfajnybartek.toolsplugin.managers;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
import pl.tenfajnybartek.toolsplugin.utils.ColorUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeleportManager {
    private final ToolsPlugin plugin;
    private final ConfigManager configManager;

    // Mapa graczy oczekujących na teleportację
    private final Map<UUID, PendingTeleport> pendingTeleports;

    // Mapa ostatnich lokalizacji (dla /back)
    private final Map<UUID, Location> lastLocations;

    public TeleportManager(ToolsPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.pendingTeleports = new HashMap<>();
        this.lastLocations = new HashMap<>();
    }

    /**
     * Teleportuje gracza z delay i sprawdzaniem ruchu
     */
    public void teleport(Player player, Location destination, String successMessage) {
        int delay = configManager.getTeleportDelay();

        // Jeśli delay = 0, teleportuj natychmiast
        if (delay <= 0) {
            teleportNow(player, destination, successMessage);
            return;
        }

        // Zapisz ostatnią lokalizację dla /back
        saveLastLocation(player);

        // Zapisz początkową lokalizację dla sprawdzania ruchu
        Location startLocation = player.getLocation().clone();

        // Dodaj do pending teleports
        PendingTeleport pending = new PendingTeleport(player, destination, startLocation, successMessage);
        pendingTeleports.put(player.getUniqueId(), pending);

        // Wyślij wiadomość o rozpoczęciu teleportacji
        player.sendMessage(ColorUtils.colorize(configManager.getPrefix() + "&aTeleportacja za &e" + delay + "s&a. Nie ruszaj się!"));

        // Uruchom task z delay
        new BukkitRunnable() {
            int countdown = delay;

            @Override
            public void run() {
                UUID uuid = player.getUniqueId();

                // Sprawdź czy teleportacja nie została anulowana
                if (!pendingTeleports.containsKey(uuid)) {
                    this.cancel();
                    return;
                }

                // Countdown
                countdown--;

                if (countdown > 0) {
                    // Co sekundę wyświetl countdown
                    if (countdown <= 3) {
                        player.sendMessage(ColorUtils.colorize("&e" + countdown + "..."));
                    }
                } else {
                    // Teleportuj!
                    PendingTeleport tp = pendingTeleports.remove(uuid);
                    if (tp != null) {
                        teleportNow(player, tp.getDestination(), tp.getSuccessMessage());
                    }
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Co sekundę
    }

    /**
     * Teleportuje natychmiast bez delay
     */
    private void teleportNow(Player player, Location destination, String successMessage) {
        // Sprawdź bezpieczną lokalizację jeśli włączone
        if (configManager.isSafeTeleportEnabled()) {
            destination = findSafeLocation(destination);
        }

        // Zapisz ostatnią lokalizację
        saveLastLocation(player);

        // Teleportuj
        player.teleport(destination);

        // Wyślij wiadomość sukcesu
        if (successMessage != null && !successMessage.isEmpty()) {
            player.sendMessage(ColorUtils.colorize(configManager.getPrefix() + successMessage));
        }
    }

    /**
     * Anuluje oczekującą teleportację gracza
     */
    public void cancelTeleport(Player player, String reason) {
        UUID uuid = player.getUniqueId();

        if (pendingTeleports.containsKey(uuid)) {
            pendingTeleports.remove(uuid);
            player.sendMessage(ColorUtils.colorize(configManager.getPrefix() + "&cTeleportacja anulowana! &7(" + reason + ")"));
        }
    }

    /**
     * Sprawdza czy gracz ma oczekującą teleportację
     */
    public boolean hasPendingTeleport(Player player) {
        return pendingTeleports.containsKey(player.getUniqueId());
    }

    /**
     * Pobiera początkową lokalizację dla sprawdzania ruchu
     */
    public Location getStartLocation(Player player) {
        PendingTeleport pending = pendingTeleports.get(player.getUniqueId());
        return pending != null ? pending.getStartLocation() : null;
    }

    /**
     * Zapisuje ostatnią lokalizację gracza (dla /back)
     */
    public void saveLastLocation(Player player) {
        lastLocations.put(player.getUniqueId(), player.getLocation().clone());
    }

    /**
     * Pobiera ostatnią lokalizację gracza (dla /back)
     */
    public Location getLastLocation(Player player) {
        return lastLocations.get(player.getUniqueId());
    }

    /**
     * Sprawdza czy lokalizacja jest bezpieczna i ewentualnie ją poprawia
     */
    private Location findSafeLocation(Location location) {
        Location safe = location.clone();

        // Sprawdź czy blok nad i pod są bezpieczne
        if (!safe.getBlock().getType().isSolid() &&
                !safe.clone().add(0, 1, 0).getBlock().getType().isSolid() &&
                safe.clone().add(0, -1, 0).getBlock().getType().isSolid()) {
            return safe; // Lokalizacja bezpieczna
        }

        // Szukaj bezpiecznej lokalizacji w górę (max 5 bloków)
        for (int i = 1; i <= 5; i++) {
            Location test = safe.clone().add(0, i, 0);
            if (!test.getBlock().getType().isSolid() &&
                    !test.clone().add(0, 1, 0).getBlock().getType().isSolid() &&
                    test.clone().add(0, -1, 0).getBlock().getType().isSolid()) {
                return test;
            }
        }

        // Jeśli nie znaleziono, zwróć oryginalną
        return safe;
    }

    /**
     * Wewnętrzna klasa przechowująca dane oczekującej teleportacji
     */
    private static class PendingTeleport {
        private final Player player;
        private final Location destination;
        private final Location startLocation;
        private final String successMessage;

        public PendingTeleport(Player player, Location destination, Location startLocation, String successMessage) {
            this.player = player;
            this.destination = destination;
            this.startLocation = startLocation;
            this.successMessage = successMessage;
        }

        public Player getPlayer() {
            return player;
        }

        public Location getDestination() {
            return destination;
        }

        public Location getStartLocation() {
            return startLocation;
        }

        public String getSuccessMessage() {
            return successMessage;
        }
    }
}
