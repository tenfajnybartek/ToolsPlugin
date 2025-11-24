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
        super("delwarp", "Usuwa warp", "/delwarp <nazwa>", "tools.cmd.delwarp", new String[]{"removewarp"});
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {

        if (args.length != 1) {
            sendUsage(sender);
            return true;
        }

        ToolsPlugin plugin = ToolsPlugin.getInstance();
        WarpManager warpManager = plugin.getWarpManager();

        String warpName = args[0].toLowerCase();

        if (!warpManager.warpExists(warpName)) {
            sendMessage(sender, "&cWarp &e" + warpName + " &cnie istnieje!");
            return true;
        }

        CompletableFuture<Boolean> future = warpManager.deleteWarp(warpName);

        future.thenAccept(success -> {

            Bukkit.getScheduler().runTask(plugin, () -> {

                if (sender instanceof Player && !((Player) sender).isOnline()) {
                    return;
                }

                if (success) {
                    sendMessage(sender, "&aUsunięto warp &e" + warpName);
                } else {
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
            return warpManager.getWarpNames().stream()
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return super.tabComplete(sender, args);
    }
}