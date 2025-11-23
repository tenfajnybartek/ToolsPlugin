package pl.tenfajnybartek.toolsplugin.listeners;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.tenfajnybartek.toolsplugin.managers.ConfigManager;
import pl.tenfajnybartek.toolsplugin.managers.TeleportManager;

public class TeleportListener implements Listener {

    private final TeleportManager teleportManager;
    private final ConfigManager configManager;

    public TeleportListener(TeleportManager teleportManager, ConfigManager configManager) {
        this.teleportManager = teleportManager;
        this.configManager = configManager;
    }

    /**
     * Anuluje teleportację, gdy gracz zmieni swoją pozycję.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!configManager.cancelTeleportOnMove()) {
            return;
        }

        Player player = event.getPlayer();

        if (!teleportManager.hasPendingTeleport(player)) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();

        // Użycie distanceSquared() do sprawdzenia zmiany pozycji w świecie.
        // Jeśli dystans > 0.0, oznacza to, że X, Y lub Z się zmieniły (ruch), ignorując obrót głowy.
        if (to != null && from.distanceSquared(to) > 0.0) {
            teleportManager.cancelTeleport(player, "Poruszyłeś się");
        }
    }

    /**
     * Anuluje teleportację, gdy gracz otrzyma obrażenia.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!configManager.cancelTeleportOnDamage()) {
            return;
        }

        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (teleportManager.hasPendingTeleport(player)) {
            teleportManager.cancelTeleport(player, "Otrzymałeś obrażenia");
        }
    }

    /**
     * Anuluje teleportację, gdy gracz wyjdzie z serwera.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (teleportManager.hasPendingTeleport(player)) {
            teleportManager.cancelTeleport(player, "Wylogowano");
        }
    }
}