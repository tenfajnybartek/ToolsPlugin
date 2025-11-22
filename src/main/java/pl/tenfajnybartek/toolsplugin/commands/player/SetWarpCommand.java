package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
import pl.tenfajnybartek.toolsplugin.managers.WarpManager;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

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
        String warpName = args[0];
        WarpManager warpManager = ToolsPlugin.getInstance().getWarpManager();

        // Sprawdź czy warp już istnieje
        if (warpManager.warpExists(warpName)) {
            sendMessage(sender, "&cWarp &e" + warpName + " &cjuż istnieje! Użyj &e/delwarp &caby go usunąć.");
            return true;
        }

        // Utwórz warp
        Location location = player.getLocation();
        warpManager.createWarp(warpName, location);

        sendMessage(sender, "&aUtworzon warp &e" + warpName + " &ana tej lokalizacji!");
        return true;
    }
}
