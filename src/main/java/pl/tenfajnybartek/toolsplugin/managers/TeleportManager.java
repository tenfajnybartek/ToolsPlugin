package pl.tenfajnybartek.toolsplugin.managers;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
import pl.tenfajnybartek.toolsplugin.objects.User;
import pl.tenfajnybartek.toolsplugin.utils.ColorUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeleportManager {

    private final ToolsPlugin plugin;
    private final ConfigManager configManager;
    private final UserManager userManager;

    private final Map<UUID, PendingTeleport> pendingTeleports;
    private final Map<UUID, Location> lastLocations;
    private final Map<UUID, TpaRequest> tpaRequests;

    public TeleportManager(ToolsPlugin plugin, ConfigManager configManager, UserManager userManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.userManager = userManager;
        this.pendingTeleports = new HashMap<>();
        this.lastLocations = new HashMap<>();
        this.tpaRequests = new HashMap<>();
    }

    // ====================================================================
    // 1. Logika Teleportacji (Delay, /back, SafeLocation)
    // ====================================================================

    /**
     * Teleportuje gracza z delay i sprawdzaniem ruchu.
     */
    public void teleport(Player player, Location destination, String successMessage) {
        int delay = configManager.getTeleportDelay();

        // Zapisz ostatnią lokalizację tylko raz, PRZED teleportacją.
        saveLastLocation(player);

        // Jeśli delay = 0, teleportuj natychmiast
        if (delay <= 0) {
            teleportNow(player, destination, successMessage);
            return;
        }

        Location startLocation = player.getLocation().clone();

        PendingTeleport pending = new PendingTeleport(player, destination, startLocation, successMessage);
        pendingTeleports.put(player.getUniqueId(), pending);

        player.sendMessage(ColorUtils.colorize(configManager.getPrefix() + "&aTeleportacja za &e" + delay + "s&a. Nie ruszaj się!"));

        new BukkitRunnable() {
            int countdown = delay;

            @Override
            public void run() {
                UUID uuid = player.getUniqueId();

                if (!pendingTeleports.containsKey(uuid)) {
                    this.cancel();
                    return;
                }

                countdown--;

                if (countdown > 0) {
                    if (countdown <= 3) {
                        player.sendMessage(ColorUtils.colorize("&e" + countdown + "..."));
                    }
                } else {
                    PendingTeleport tp = pendingTeleports.remove(uuid);
                    if (tp != null) {
                        teleportNow(player, tp.getDestination(), tp.getSuccessMessage());
                    }
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    /**
     * Teleportuje natychmiast bez delay.
     */
    private void teleportNow(Player player, Location destination, String successMessage) {
        if (configManager.isSafeTeleportEnabled()) {
            destination = findSafeLocation(destination);
        }

        // **USUNIĘTO:** saveLastLocation(player) - ZAPIS JEST JUŻ NA POCZĄTKU metody teleport()

        player.teleport(destination);

        if (successMessage != null && !successMessage.isEmpty()) {
            player.sendMessage(ColorUtils.colorize(configManager.getPrefix() + successMessage));
        }
    }

    /**
     * Anuluje oczekującą teleportację gracza.
     */
    public void cancelTeleport(Player player, String reason) {
        UUID uuid = player.getUniqueId();

        if (pendingTeleports.containsKey(uuid)) {
            pendingTeleports.remove(uuid);
            player.sendMessage(ColorUtils.colorize(configManager.getPrefix() + "&cTeleportacja anulowana! &7(" + reason + ")"));
        }
    }

    /**
     * Sprawdza czy gracz ma oczekującą teleportację.
     */
    public boolean hasPendingTeleport(Player player) {
        return pendingTeleports.containsKey(player.getUniqueId());
    }

    /**
     * Pobiera początkową lokalizację dla sprawdzania ruchu.
     */
    public Location getStartLocation(Player player) {
        PendingTeleport pending = pendingTeleports.get(player.getUniqueId());
        return pending != null ? pending.getStartLocation() : null;
    }

    /**
     * Zapisuje ostatnią lokalizację gracza (dla /back).
     */
    public void saveLastLocation(Player player) {
        User user = userManager.getUser(player);

        // Sprawdź czy gracz nie wyłączył teleportacji (np. by zablokować /back)
        if (user != null && !user.isTeleportToggle()) {
            return;
        }

        lastLocations.put(player.getUniqueId(), player.getLocation().clone());
    }

    /**
     * Pobiera ostatnią lokalizację gracza (dla /back).
     */
    public Location getLastLocation(Player player) {
        return lastLocations.get(player.getUniqueId());
    }

    /**
     * Sprawdza czy lokalizacja jest bezpieczna i ewentualnie ją poprawia.
     */
    private Location findSafeLocation(Location location) {
        Location safe = location.clone();

        // Sprawdź czy blok nad i pod są bezpieczne
        if (!safe.getBlock().getType().isSolid() &&
                !safe.clone().add(0, 1, 0).getBlock().getType().isSolid() &&
                safe.clone().add(0, -1, 0).getBlock().getType().isSolid()) {
            return safe;
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

        return safe;
    }

    // ====================================================================
    // 2. Logika TPA (/tpa, /tpaccept, /tpadeny)
    // ====================================================================

    /**
     * Dodaje nową prośbę TPA.
     */
    public void addTpaRequest(Player sender, Player target) {
        long expirationTime = 60 * 20L; // 60 sekund w tickach (20 ticków = 1 sekunda)

        tpaRequests.put(target.getUniqueId(), new TpaRequest(sender.getUniqueId()));

        new BukkitRunnable() {
            @Override
            public void run() {
                TpaRequest request = tpaRequests.get(target.getUniqueId());
                if (request != null && request.getSenderId().equals(sender.getUniqueId())) {

                    tpaRequests.remove(target.getUniqueId());

                    if (sender.isOnline()) {
                        sender.sendMessage(ColorUtils.colorize(configManager.getPrefix() + "&cTwoja prośba do " + target.getName() + " wygasła."));
                    }
                }
            }
        }.runTaskLater(plugin, expirationTime);
    }

    /**
     * Pobiera prośbę TPA dla odbiorcy.
     */
    public TpaRequest getTpaRequest(Player target) {
        return tpaRequests.get(target.getUniqueId());
    }

    /**
     * Usuwa prośbę TPA.
     */
    public void removeTpaRequest(Player target) {
        tpaRequests.remove(target.getUniqueId());
    }


    // ====================================================================
    // 3. Wewnętrzne klasy danych
    // ====================================================================

    /**
     * Wewnętrzna klasa przechowująca dane oczekującej teleportacji (dla delay)
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

        public Location getDestination() { return destination; }
        public Location getStartLocation() { return startLocation; }
        public String getSuccessMessage() { return successMessage; }
    }

    /**
     * Wewnętrzna klasa przechowująca dane oczekującej prośby TPA
     */
    public static class TpaRequest {
        private final UUID senderId;
        private final long timestamp;

        public TpaRequest(UUID senderId) {
            this.senderId = senderId;
            this.timestamp = System.currentTimeMillis();
        }

        public UUID getSenderId() { return senderId; }
        public long getTimestamp() { return timestamp; }
    }
}
