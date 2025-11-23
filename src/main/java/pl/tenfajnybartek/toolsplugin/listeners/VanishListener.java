package pl.tenfajnybartek.toolsplugin.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.tenfajnybartek.toolsplugin.managers.VanishManager;

public class VanishListener implements Listener {

    private final VanishManager vanishManager;

    public VanishListener(VanishManager vanishManager) {
        this.vanishManager = vanishManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player joining = event.getPlayer();
        if (vanishManager.isVanished(joining) && vanishManager.shouldHideJoinQuitMessages()) {
            event.joinMessage(null);
        }
        vanishManager.syncOnJoin(joining);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        if (vanishManager.isVanished(p) && vanishManager.shouldHideJoinQuitMessages()) {
            event.quitMessage(null);
        }
    }

    @EventHandler
    public void onMobTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getTarget() instanceof Player target)) return;
        if (vanishManager.shouldPreventMobTarget(target)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (vanishManager.shouldBlockPickup(player)) {
            event.setCancelled(true);
        }
    }
}
