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

    /**
     * Ładuje dane użytkownika i home'y asynchronicznie przy dołączeniu.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // 1. Sprawdzenie pierwszego dołączenia (kosmetyczne)
        if (!player.hasPlayedBefore()) {
            player.sendMessage(ColorUtils.colorize("&a&lWitaj po raz pierwszy na serwerze!"));
        }

        // 2. Ładowanie użytkownika z DB (asynchroniczne)
        // Zakładamy, że userManager.loadUser() radzi sobie z pierwszym załadowaniem i tworzeniem obiektu.
        userManager.loadUser(player);

        // 3. Ładowanie Home'ów z DB (asynchroniczne)
        // Uruchamiamy ładowanie w tle; wynik zostanie użyty w HomeManager do zbudowania cache.
        // Cache jest niezbędny do późniejszych szybkich sprawdzeń limitów home'ów.
        homeManager.loadPlayerHomes(player.getUniqueId());
    }

    /**
     * Zapisuje użytkownika i usuwa dane z pamięci przy wyjściu.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // 1. Zapis Usera (np. ostatniej lokacji, balansu)
        userManager.unloadUser(player);

        // 2. Usunięcie home'ów z cache
        // Jest to operacja na pamięci RAM, więc jest synchroniczna i natychmiastowa.
        homeManager.unloadPlayerHomes(player.getUniqueId());
    }
}