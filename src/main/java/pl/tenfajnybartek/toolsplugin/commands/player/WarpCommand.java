package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
import pl.tenfajnybartek.toolsplugin.managers.CooldownManager;
import pl.tenfajnybartek.toolsplugin.managers.TeleportManager;
import pl.tenfajnybartek.toolsplugin.managers.WarpManager;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

import java.util.List;
import java.util.stream.Collectors;

public class WarpCommand extends BaseCommand {

    public WarpCommand() {
        super("warp", "Teleportuje do warpa", "/warp <nazwa>", "tools.cmd.warp", new String[]{"warps"});
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!isPlayer(sender)) {
            sendOnlyPlayer(sender);
            return true;
        }

        Player player = getPlayer(sender);
        WarpManager warpManager = ToolsPlugin.getInstance().getWarpManager();
        TeleportManager teleportManager = ToolsPlugin.getInstance().getTeleportManager();

        if (args.length == 0) {
            if (warpManager.getWarpCount() == 0) {
                sendMessage(sender, "&cNie ma żadnych warpów na serwerze!");
                return true;
            }

            sendMessage(sender, "&8--- &6&lLista Warpów &8---");
            sendMessage(sender, "&eDostępne warpy: &f" + String.join("&7, &f", warpManager.getWarpNames()));
            sendMessage(sender, "&eUżyj: &f/warp <nazwa>");
            return true;
        }

        String warpName = args[0].toLowerCase();

        if (!warpManager.warpExists(warpName)) {
            sendMessage(sender, "&cWarp &e" + warpName + " &cnie istnieje!");
            return true;
        }

        Location warpLocation = warpManager.getWarp(warpName);

        if (warpLocation == null || warpLocation.getWorld() == null) {
            sendMessage(player, "&cŚwiat warpa &e" + warpName + "&c nie jest załadowany! Zgłoś to administracji.");
            return true;
        }

        teleportManager.teleport(player, warpLocation, "&aPrzeteleportowano do warpa &e" + warpName);

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