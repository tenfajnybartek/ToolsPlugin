package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
import pl.tenfajnybartek.toolsplugin.managers.TeleportManager;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

import java.util.UUID;

public class TpAcceptCommand extends BaseCommand {

    public TpAcceptCommand() {
        super("tpaccept", "Akceptuje prośbę o teleportację", "/tpaccept", "tools.tpaccept", null);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!isPlayer(sender)) {
            sendMessage(sender, "&cTylko gracze mogą używać tej komendy.");
            return true;
        }

        Player target = getPlayer(sender);
        TeleportManager tm = ToolsPlugin.getInstance().getTeleportManager();

        // Pobierz prośbę TPA (dla odbiorcy)
        TeleportManager.TpaRequest request = tm.getTpaRequest(target);

        if (request == null) {
            sendMessage(target, "&cNie masz żadnej oczekującej prośby o teleportację.");
            return true;
        }

        UUID senderId = request.getSenderId();
        Player senderPlayer = Bukkit.getPlayer(senderId);

        if (senderPlayer == null || !senderPlayer.isOnline()) {
            tm.removeTpaRequest(target);
            sendMessage(target, "&cNadawca prośby jest już offline.");
            return true;
        }

        // Usunięcie prośby
        tm.removeTpaRequest(target);

        // Powiadomienie nadawcy, że prośba została przyjęta
        sendMessage(senderPlayer, "&aTwoja prośba do &e" + target.getName() + "&a została zaakceptowana. Rozpoczynanie teleportacji...");

        // Teleportacja (używamy standardowej metody teleport z delay).
        // Wysłana jest prośba o teleportację SENDERa do TARGETa.
        tm.teleport(senderPlayer, target.getLocation(), "&aTeleportacja do &e" + target.getName() + "&a zakończona sukcesem.");

        // Powiadomienia dla odbiorcy
        sendMessage(target, "&aZaakceptowałeś prośbę. &e" + senderPlayer.getName() + "&a jest teleportowany do Ciebie.");

        return true;
    }
}
