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
        super("back", "Teleportuje do ostatniej lokalizacji", "/back", "tfbhc.cmd.back", null);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!isPlayer(sender)) {
            sendMessage(sender, "&cTa komenda może być użyta tylko przez gracza!");
            return true;
        }

        Player player = getPlayer(sender);
        TeleportManager teleportManager = ToolsPlugin.getInstance().getTeleportManager();
        CooldownManager cooldownManager = ToolsPlugin.getInstance().getCooldownManager();

        Location lastLocation = teleportManager.getLastLocation(player);

        if (lastLocation == null) {
            sendMessage(sender, "&cNie masz zapisanej ostatniej lokalizacji!");
            return true;
        }

        // Sprawdź cooldown
        if (cooldownManager.checkCooldown(player, "back")) {
            return true;
        }

        // Teleportuj z delay
        teleportManager.teleport(player, lastLocation, "&aPowrócono do ostatniej lokalizacji");

        // Ustaw cooldown
        cooldownManager.setCooldown(player, "back");

        return true;
    }
}
