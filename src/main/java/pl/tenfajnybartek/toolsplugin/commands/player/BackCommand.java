package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
import pl.tenfajnybartek.toolsplugin.managers.CooldownManager;
import pl.tenfajnybartek.toolsplugin.managers.TeleportManager;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

public class BackCommand extends BaseCommand {

    public BackCommand() {
        super("back", "Teleportuje do ostatniej lokalizacji", "/back", "tools.cmd.back", null);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!isPlayer(sender)) {
            sendOnlyPlayer(sender);
            return true;
        }

        Player player = getPlayer(sender);
        TeleportManager teleportManager = ToolsPlugin.getInstance().getTeleportManager();

        Location lastLocation = teleportManager.getLastLocation(player);

        if (lastLocation == null) {
            sendMessage(sender, "&cNie masz zapisanej ostatniej lokalizacji!");
            return true;
        }

        teleportManager.teleport(player, lastLocation, "&aPowrócono do ostatniej lokalizacji");

        return true;
    }
}
