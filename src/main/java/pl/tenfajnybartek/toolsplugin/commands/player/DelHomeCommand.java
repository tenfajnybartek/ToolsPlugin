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
        super("delhome", "Usuwa dom", "/delhome <nazwa>", "tools.cmd.delhome", new String[]{"removehome"});
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!isPlayer(sender)) {
            sendOnlyPlayer(sender);
            return true;
        }

        if (args.length != 1) {
            sendUsage(sender);
            return true;
        }

        Player player = getPlayer(sender);
        ToolsPlugin plugin = ToolsPlugin.getInstance();
        HomeManager homeManager = plugin.getHomeManager();

        String homeName = args[0].toLowerCase();

        if (!homeManager.hasHome(player, homeName)) {
            sendMessage(player, "&cNie masz domu o nazwie &e" + homeName + "&c!");
            return true;
        }

        CompletableFuture<Boolean> future = homeManager.deleteHomeAsync(player, homeName);

        future.thenAccept(success -> {

            Bukkit.getScheduler().runTask(plugin, () -> {

                if (!player.isOnline()) {
                    return;
                }

                if (success) {
                    sendMessage(player, "&aPomyślnie usunięto dom &e" + homeName + "&a.");
                } else {
                    sendMessage(player, "&cWystąpił błąd podczas usuwania domu &e" + homeName + "&c. Spróbuj ponownie później.");
                }
            });
        });

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && isPlayer(sender)) {
            Player player = getPlayer(sender);
            HomeManager homeManager = ToolsPlugin.getInstance().getHomeManager();
            return homeManager.getHomeNames(player).stream()
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return super.tabComplete(sender, args);
    }
}