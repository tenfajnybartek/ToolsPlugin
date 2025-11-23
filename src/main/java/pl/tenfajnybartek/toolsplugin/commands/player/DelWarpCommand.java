package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
import pl.tenfajnybartek.toolsplugin.managers.WarpManager;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class DelWarpCommand extends BaseCommand {

    public DelWarpCommand() {
        super("delwarp", "Usuwa warp", "/delwarp <nazwa>", "tfbhc.cmd.delwarp", new String[]{"removewarp"});
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // Ta komenda jest dla konsoli i graczy, więc nie sprawdzamy isPlayer,
        // ale musimy mieć pewność, że sender ma uprawnienia (zakładamy, że BaseCommand to sprawdza).

        if (args.length != 1) {
            sendMessage(sender, "&cUżycie: " + getUsage());
            return true;
        }

        ToolsPlugin plugin = ToolsPlugin.getInstance();
        WarpManager warpManager = plugin.getWarpManager();

        // Zawsze upewniamy się, że nazwa jest w małych literach dla spójności z managerem
        String warpName = args[0].toLowerCase();

        // 1. Sprawdź czy warp już istnieje (Synchronicznie, z cache)
        if (!warpManager.warpExists(warpName)) {
            sendMessage(sender, "&cWarp &e" + warpName + " &cnie istnieje!");
            return true;
        }

        // 2. Wywołaj asynchroniczną operację usuwania warpa
        CompletableFuture<Boolean> future = warpManager.deleteWarpAsync(warpName);

        // 3. Obsługa wyniku na wątku głównym
        future.thenAccept(success -> {

            // Kod Bukkit API (sendMessage) musi być wykonany na wątku głównym
            Bukkit.getScheduler().runTask(plugin, () -> {

                // Sprawdzenie, czy sender (jeśli jest graczem) jest online
                if (sender instanceof Player && !((Player) sender).isOnline()) {
                    return;
                }

                if (success) {
                    sendMessage(sender, "&aUsunięto warp &e" + warpName);
                } else {
                    // Ten przypadek wystąpi głównie przy błędzie DB
                    sendMessage(sender, "&cWystąpił błąd podczas usuwania warpa &e" + warpName + "&c. Spróbuj ponownie później.");
                }
            });
        });

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            WarpManager warpManager = ToolsPlugin.getInstance().getWarpManager();
            // TabComplete nadal bazuje na szybkim odczycie cache
            return warpManager.getWarpNames().stream()
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return super.tabComplete(sender, args);
    }
}