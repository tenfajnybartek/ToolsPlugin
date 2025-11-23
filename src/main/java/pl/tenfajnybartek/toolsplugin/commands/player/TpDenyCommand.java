package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
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

        // Usunięcie prośby
        tm.removeTpaRequest(target);

        // Powiadomienia dla odbiorcy (który odrzucił)
        sendMessage(target, "&cOdrzuciłeś prośbę o teleportację.");

        // Powiadomienie dla nadawcy
        if (senderPlayer != null && senderPlayer.isOnline()) {
            sendMessage(senderPlayer, "&cGracz &e" + target.getName() + "&c odrzucił Twoją prośbę.");
        }

        return true;
    }
}
