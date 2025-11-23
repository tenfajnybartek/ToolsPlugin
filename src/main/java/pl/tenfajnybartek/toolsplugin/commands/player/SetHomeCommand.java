package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
import pl.tenfajnybartek.toolsplugin.managers.HomeManager;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class SetHomeCommand extends BaseCommand {

    public SetHomeCommand() {
        super("sethome", "Ustawia dom", "/sethome [nazwa]", "tfbhc.cmd.sethome", null);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!isPlayer(sender)) {
            sendMessage(sender, "&cTa komenda może być użyta tylko przez gracza!");
            return true;
        }

        Player player = getPlayer(sender);
        ToolsPlugin plugin = ToolsPlugin.getInstance();
        HomeManager homeManager = plugin.getHomeManager();

        String homeName = args.length == 0 ? "home" : args[0].toLowerCase();
        Location location = player.getLocation();

        // Synchroniczne sprawdzenie cache (do celów wiadomości), czy nadpisujemy
        boolean isUpdate = homeManager.hasHome(player, homeName);

        // 1. Wywołanie asynchronicznej operacji tworzenia/aktualizacji home'a
        CompletableFuture<Boolean> future = homeManager.createHome(player, homeName, location);

        // 2. Obsługa wyniku na wątku głównym (za pomocą Bukkit Scheduler)
        future.thenAccept(success -> {

            // Używamy runTask, aby wykonać kod Bukkit na wątku głównym (Main Thread)
            Bukkit.getScheduler().runTask(plugin, () -> {

                if (!player.isOnline()) {
                    return;
                }

                if (success) {
                    // Sukces
                    if (isUpdate) {
                        sendMessage(player, "&aZaktualizowano lokalizację domu &e" + homeName);
                    } else {
                        // Cache jest już zaktualizowany przez HomeManager
                        int currentHomes = homeManager.getHomeCount(player);
                        int maxHomes = homeManager.getMaxHomes(player);
                        sendMessage(player, "&aUtworzono dom &e" + homeName + " &7(" + currentHomes + "/" + maxHomes + ")");
                    }
                } else {
                    // Niepowodzenie (limit home'ów)
                    int maxHomes = homeManager.getMaxHomes(player);
                    sendMessage(player, "&cOsiągnąłeś maksymalną liczbę domów! &7(" + maxHomes + ")");

                    if (!homeManager.getHomeNames(player).isEmpty()) {
                        String homesList = String.join("&7, &f", homeManager.getHomeNames(player));
                        sendMessage(player, "&eTwoje domy: &f" + homesList);
                    }
                }
            }); // Koniec runTask
        }); // Koniec thenAccept

        return true;
    }
}