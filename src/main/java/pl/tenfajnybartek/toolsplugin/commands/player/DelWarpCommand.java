package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.command.CommandSender;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
import pl.tenfajnybartek.toolsplugin.managers.WarpManager;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

import java.util.List;
import java.util.stream.Collectors;

public class DelWarpCommand extends BaseCommand {

    public DelWarpCommand() {
        super("delwarp", "Usuwa warp", "/delwarp <nazwa>", "tfbhc.cmd.delwarp", new String[]{"removewarp"});
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sendMessage(sender, "&cUżycie: " + getUsage());
            return true;
        }

        String warpName = args[0];
        WarpManager warpManager = ToolsPlugin.getInstance().getWarpManager();

        if (!warpManager.warpExists(warpName)) {
            sendMessage(sender, "&cWarp &e" + warpName + " &cnie istnieje!");
            return true;
        }

        warpManager.deleteWarp(warpName);
        sendMessage(sender, "&aUsunięto warp &e" + warpName);
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
