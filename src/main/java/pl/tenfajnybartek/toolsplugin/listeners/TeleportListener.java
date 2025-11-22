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
     * Anuluje teleportację gdy gracz się ruszy
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!configManager.cancelTeleportOnMove()) {
            return; // Funkcja wyłączona w config
        }

        Player player = event.getPlayer();

        if (!teleportManager.hasPendingTeleport(player)) {
            return; // Gracz nie ma oczekującej teleportacji
        }

        Location from = event.getFrom();
        Location to = event.getTo();

        // Sprawdź czy gracz się poruszył (ignoruj obrót głowy)
        if (to != null && (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ())) {
            teleportManager.cancelTeleport(player, "Poruszyłeś się");
        }
    }

    /**
     * Anuluje teleportację gdy gracz otrzyma obrażenia
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!configManager.cancelTeleportOnDamage()) {
            return; // Funkcja wyłączona w config
        }

        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();

        if (teleportManager.hasPendingTeleport(player)) {
            teleportManager.cancelTeleport(player, "Otrzymałeś obrażenia");
        }
    }

    /**
     * Anuluje teleportację gdy gracz wyjdzie z serwera
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (teleportManager.hasPendingTeleport(player)) {
            teleportManager.cancelTeleport(player, "Wylogowano");
        }
    }
}
