package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
import pl.tenfajnybartek.toolsplugin.managers.ActionBarManager;
import pl.tenfajnybartek.toolsplugin.managers.TeleportManager;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

public class TpDenyCommand extends BaseCommand {

    public TpDenyCommand() {
        super("tpadeny", "Odrzuca prośbę o teleportację", "/tpadeny", "tools.tpadeny", null);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!isPlayer(sender)) {
            sendMessage(sender, "&cTylko gracze mogą używać tej komendy.");
            return true;
        }

        Player target = getPlayer(sender);
        TeleportManager tm = ToolsPlugin.getInstance().getTeleportManager();
        TeleportManager.TpaRequest request = tm.getTpaRequest(target);

        if (request == null) {
            sendMessage(target, "&cNie masz żadnej oczekującej prośby o teleportację.");
            return true;
        }

        Player senderPlayer = Bukkit.getPlayer(request.getSenderId());
        tm.removeTpaRequest(target);

        sendMessage(target, "&cOdrzuciłeś prośbę o teleportację.");
        if (senderPlayer != null && senderPlayer.isOnline()) {
            sendMessage(senderPlayer, "&cGracz &e" + target.getName() + "&c odrzucił Twoją prośbę.");
        }

        ActionBarManager abm = ToolsPlugin.getInstance().getActionBarManager();
        abm.pushEphemeral(target,
                abm.colored("&cOdrzuciłeś TPA."),
                40, ActionBarManager.ActionPriority.MEDIUM);
        if (senderPlayer != null && senderPlayer.isOnline()) {
            abm.pushEphemeral(senderPlayer,
                    abm.colored("&cTPA odrzucone przez " + target.getName()),
                    60, ActionBarManager.ActionPriority.LOW);
        }

        return true;
    }
}