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
    @SuppressWarnings("deprecation")
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        try {
            Optional<BanRecord> optionalActiveBan = banManager.getActiveBan(event.getUniqueId()).join();
            if (optionalActiveBan != null && optionalActiveBan.isPresent()) {
                BanRecord activeBan = optionalActiveBan.get();
                event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_BANNED);
                event.setKickMessage(SERIALIZER.serialize(TimeUtils.getBanMessage(activeBan)));
            }
        } catch (Exception e) {
            handleLoginError(event, "Błąd podczas sprawdzania bana dla gracza " + event.getName(), e);
        }
    }

    @SuppressWarnings("deprecation")
    private void handleLoginError(AsyncPlayerPreLoginEvent event, String message, Exception e) {
        ToolsPlugin.getInstance().getLogger().log(Level.SEVERE, message, e);
        event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
        event.setKickMessage(SERIALIZER.serialize(TimeUtils.getBanMessage(null)));
    }
}