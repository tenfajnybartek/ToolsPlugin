package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
import pl.tenfajnybartek.toolsplugin.managers.WarpManager;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

import java.util.concurrent.CompletableFuture;

public class SetWarpCommand extends BaseCommand {

    public SetWarpCommand() {
        super("setwarp", "Tworzy nowy warp", "/setwarp <nazwa>", "tfbhc.cmd.setwarp", null);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!isPlayer(sender)) {
            sendMessage(sender, "&cTa komenda może być użyta tylko przez gracza!");
            return true;
        }

        if (args.length != 1) {
            sendMessage(sender, "&cUżycie: " + getUsage());
            return true;
        }

        Player player = getPlayer(sender);
        ToolsPlugin plugin = ToolsPlugin.getInstance();
        WarpManager warpManager = plugin.getWarpManager();

        // Zawsze upewniamy się, że nazwa jest w małych literach dla spójności z managerem
        String warpName = args[0].toLowerCase();

        // 1. Sprawdź czy warp już istnieje (Synchronicznie, z cache)
        if (warpManager.warpExists(warpName)) {
            sendMessage(sender, "&cWarp &e" + warpName + " &cjuż istnieje! Użyj &e/delwarp &caby go usunąć.");
            return true;
        }

        Location location = player.getLocation();

        // 2. Wywołaj asynchroniczną operację tworzenia warpa
        CompletableFuture<Boolean> future = warpManager.createWarpAsync(warpName, location);

        // 3. Obsługa wyniku na wątku głównym
        future.thenAccept(success -> {

            // Kod API Bukkit musi być wykonany na wątku głównym
            Bukkit.getScheduler().runTask(plugin, () -> {

                if (!player.isOnline()) {
                    return;
                }

                if (success) {
                    sendMessage(player, "&aUtworzono warp &e" + warpName + " &ana tej lokalizacji!");
                } else {
                    // Ten przypadek wystąpi, jeśli operacja DB się nie powiedzie (np. błąd połączenia)
                    sendMessage(player, "&cWystąpił błąd podczas tworzenia warpa &e" + warpName + "&c. Spróbuj ponownie później.");
                }
            });
        });

        // Opcjonalnie: Graczowi można wysłać natychmiastową informację, że operacja jest przetwarzana
        // sendMessage(player, "&7Trwa zapisywanie warpa...");

        return true;
    }
}
