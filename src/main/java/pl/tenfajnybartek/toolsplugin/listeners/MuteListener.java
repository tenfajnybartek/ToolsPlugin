package pl.tenfajnybartek.toolsplugin.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
import pl.tenfajnybartek.toolsplugin.managers.MuteManager;
import pl.tenfajnybartek.toolsplugin.objects.MuteRecord;
import pl.tenfajnybartek.toolsplugin.utils.ColorUtils;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;

public class MuteListener implements Listener {

    private final MuteManager muteManager;
    private final ToolsPlugin plugin;

    public MuteListener(MuteManager muteManager, ToolsPlugin plugin) {
        this.muteManager = muteManager;
        this.plugin = plugin;
    }

    @EventHandler
    // Tłumienie ostrzeżenia o użyciu przestarzałego zdarzenia AsyncPlayerChatEvent
    @SuppressWarnings("deprecation")
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        // Krok 1: Anuluj zdarzenie natychmiast
        event.setCancelled(true);

        // Krok 2: Wykonaj asynchroniczną operację odczytu z DB
        CompletableFuture<Optional<MuteRecord>> future = muteManager.getActiveMute(player.getUniqueId());

        // Krok 3: Przetwórz wynik, wracając na wątek główny
        future.thenAccept(optionalMute -> {

            Bukkit.getScheduler().runTask(plugin, () -> {

                if (!player.isOnline()) {
                    return;
                }

                if (optionalMute.isPresent()) {
                    MuteRecord activeMute = optionalMute.get();

                    // Wyciszony: wysyłamy tylko wiadomość o mutowaniu.
                    player.sendMessage(activeMute.getMuteMessage());

                } else {
                    // NIE wyciszony: przywracamy zdarzenie.
                    event.setCancelled(false);
                }
            });
        }).exceptionally(ex -> {
            // Obsługa błędów DB w wątku asynchronicznym
            ToolsPlugin.getInstance().getLogger().log(Level.SEVERE,
                    "Błąd DB podczas sprawdzania muta dla " + player.getName(), ex);

            // Wracamy na wątek główny, aby poinformować gracza o błędzie
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    player.sendMessage(ColorUtils.toComponent("&cWystąpił błąd podczas sprawdzania statusu wyciszenia. Spróbuj ponownie."));
                }
                // Pozwalamy na wysłanie wiadomości.
                event.setCancelled(false);
            });
            return null;
        });
    }
}