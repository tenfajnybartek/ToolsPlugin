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
    @SuppressWarnings("deprecation")
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        event.setCancelled(true);

        CompletableFuture<Optional<MuteRecord>> future = muteManager.getActiveMute(player.getUniqueId());

        future.thenAccept(optionalMute -> {

            Bukkit.getScheduler().runTask(plugin, () -> {

                if (!player.isOnline()) {
                    return;
                }

                if (optionalMute.isPresent()) {
                    MuteRecord activeMute = optionalMute.get();

                    player.sendMessage(activeMute.getMuteMessage());

                } else {
                    event.setCancelled(false);
                }
            });
        }).exceptionally(ex -> {
            ToolsPlugin.getInstance().getLogger().log(Level.SEVERE,
                    "Błąd DB podczas sprawdzania muta dla " + player.getName(), ex);

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    player.sendMessage(ColorUtils.toComponent("&cWystąpił błąd podczas sprawdzania statusu wyciszenia. Spróbuj ponownie."));
                }
                event.setCancelled(false);
            });
            return null;
        });
    }
}