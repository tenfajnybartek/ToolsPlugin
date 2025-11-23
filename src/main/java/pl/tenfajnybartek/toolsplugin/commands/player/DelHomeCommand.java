package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
import pl.tenfajnybartek.toolsplugin.managers.HomeManager;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class DelHomeCommand extends BaseCommand {

    public DelHomeCommand() {
        super("delhome", "Usuwa dom", "/delhome <nazwa>", "tfbhc.cmd.delhome", new String[]{"removehome"});
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
        HomeManager homeManager = plugin.getHomeManager();

        // Zapewniamy, że nazwa jest w małych literach, aby pasowała do cache
        String homeName = args[0].toLowerCase();

        // 1. Sprawdzenie istnienia home'a (synchronicznie, z cache)
        if (!homeManager.hasHome(player, homeName)) {
            sendMessage(player, "&cNie masz domu o nazwie &e" + homeName + "&c!");
            return true;
        }

        // 2. Wywołanie asynchronicznej operacji usuwania z DB
        CompletableFuture<Boolean> future = homeManager.deleteHomeAsync(player, homeName);

        // 3. Obsługa wyniku na wątku głównym
        future.thenAccept(success -> {

            // Delegowanie operacji Bukkit API na wątek główny
            Bukkit.getScheduler().runTask(plugin, () -> {

                if (!player.isOnline()) {
                    return;
                }

                if (success) {
                    sendMessage(player, "&aPomyślnie usunięto dom &e" + homeName + "&a.");
                } else {
                    // Ten kod wykonuje się tylko w przypadku błędu bazy danych
                    sendMessage(player, "&cWystąpił błąd podczas usuwania domu &e" + homeName + "&c. Spróbuj ponownie później.");
                }
            }); // Koniec runTask
        }); // Koniec thenAccept

        // Opcjonalnie: Poinformuj gracza, że operacja jest przetwarzana.
        // sendMessage(player, "&7Trwa usuwanie domu...");

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && isPlayer(sender)) {
            Player player = getPlayer(sender);
            HomeManager homeManager = ToolsPlugin.getInstance().getHomeManager();

            // Wersja tabComplete jest nadal synchroniczna i oparta na cache (homeManager.getHomeNames(player))
            return homeManager.getHomeNames(player).stream()
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return super.tabComplete(sender, args);
    }
}