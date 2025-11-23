package pl.tenfajnybartek.toolsplugin.listeners;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
import pl.tenfajnybartek.toolsplugin.managers.BanManager;
import pl.tenfajnybartek.toolsplugin.objects.BanRecord;
import pl.tenfajnybartek.toolsplugin.utils.TimeUtils;

import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;

public class BanListener implements Listener {

    private final BanManager banManager;
    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacySection();

    public BanListener(BanManager banManager) {
        this.banManager = banManager;
    }

    @EventHandler
    // Tłumienie ostrzeżenia o użyciu przestarzałej metody setKickMessage(String)
    @SuppressWarnings("deprecation")
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {

        try {
            // Bezpieczne blokowanie na wątku roboczym
            Optional<BanRecord> optionalActiveBan = banManager.getActiveBan(event.getUniqueId()).join();

            if (optionalActiveBan.isPresent()) {
                BanRecord activeBan = optionalActiveBan.get();

                event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_BANNED);

                // Konwersja Component na String wymagany przez przestarzałe API
                event.setKickMessage(SERIALIZER.serialize(TimeUtils.getBanMessage(activeBan)));
            }

        } catch (CompletionException e) {
            // Błąd pochodzący z wątku asynchronicznego (np. SQLException w BanManager)
            handleLoginError(event, "Błąd asynchroniczny podczas sprawdzania bana dla gracza " + event.getName(), e);

        } catch (Exception e) {
            // Inne nieprzewidziane błędy (mniej prawdopodobne, ale dla bezpieczeństwa)
            handleLoginError(event, "Nieprzewidziany błąd podczas sprawdzania bana dla gracza " + event.getName(), e);
        }
    }

    /**
     * Centralna metoda obsługi błędów podczas logowania.
     */
    @SuppressWarnings("deprecation")
    private void handleLoginError(AsyncPlayerPreLoginEvent event, String message, Exception e) {
        // Użycie loggera pluginu do solidnego zapisywania błędu
        ToolsPlugin.getInstance().getLogger().log(Level.SEVERE, message, e);

        // Ustawienie wyniku na KICK_OTHER i wyświetlenie komunikatu o błędzie DB
        event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);

        // Konwersja Component błędu na String
        event.setKickMessage(SERIALIZER.serialize(TimeUtils.getBanMessage(null)));
    }
}