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
        super("setwarp", "Tworzy nowy warp", "/setwarp <nazwa>", "tools.cmd.setwarp", null);
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
        WarpManager warpManager = plugin.getWarpManager();
        String warpName = args[0].toLowerCase();

        if (warpManager.warpExists(warpName)) {
            sendMessage(sender, "&cWarp &e" + warpName + " &cjuż istnieje! Użyj &e/delwarp &caby go usunąć.");
            return true;
        }

        Location location = player.getLocation();

        CompletableFuture<Boolean> future = warpManager.createWarp(warpName, location);

        future.thenAccept(success -> {

            Bukkit.getScheduler().runTask(plugin, () -> {

                if (!player.isOnline()) {
                    return;
                }

                if (success) {
                    sendMessage(player, "&aUtworzono warp &e" + warpName + " &ana tej lokalizacji!");
                } else {
                    sendMessage(player, "&cWystąpił błąd podczas tworzenia warpa &e" + warpName + "&c. Spróbuj ponownie później.");
                }
            });
        });

        return true;
    }
}
