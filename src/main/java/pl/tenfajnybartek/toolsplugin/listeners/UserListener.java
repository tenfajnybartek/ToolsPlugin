package pl.tenfajnybartek.toolsplugin.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.tenfajnybartek.toolsplugin.managers.HomeManager;
import pl.tenfajnybartek.toolsplugin.managers.UserManager;
import pl.tenfajnybartek.toolsplugin.utils.ColorUtils;

public class UserListener implements Listener {

    private final UserManager userManager;
    private final HomeManager homeManager; // DODANO

    public UserListener(UserManager userManager, HomeManager homeManager) { // ZMIENIONO KONSTRUKTOR
        this.userManager = userManager;
        this.homeManager = homeManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPlayedBefore()) {
            player.sendMessage(ColorUtils.colorize("&a&lWitaj po raz pierwszy na serwerze!"));
        }

        userManager.loadUser(player);

        homeManager.loadPlayerHomesAsync(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        userManager.unloadUser(player);
        homeManager.unloadPlayerHomes(player.getUniqueId());
    }
}