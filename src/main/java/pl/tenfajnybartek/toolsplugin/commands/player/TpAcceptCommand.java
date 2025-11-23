package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
import pl.tenfajnybartek.toolsplugin.managers.ActionBarManager;
import pl.tenfajnybartek.toolsplugin.managers.ConfigManager;
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

        tm.removeTpaRequest(target);

        sendMessage(senderPlayer, "&aTwoja prośba do &e" + target.getName() + "&a została zaakceptowana. Rozpoczynanie teleportacji...");
        sendMessage(target, "&aZaakceptowałeś prośbę. &e" + senderPlayer.getName() + "&a jest teleportowany do Ciebie.");

        ConfigManager cfg = ToolsPlugin.getInstance().getConfigManager();
        ActionBarManager abm = ToolsPlugin.getInstance().getActionBarManager();
        int delay = cfg.getTeleportDelay();

        abm.pushEphemeral(senderPlayer,
                abm.colored("&aTPA zaakceptowane – teleport za &e" + delay + "s"),
                60, ActionBarManager.ActionPriority.MEDIUM);
        abm.pushEphemeral(target,
                abm.colored("&aAkceptujesz TPA od &e" + senderPlayer.getName()),
                40, ActionBarManager.ActionPriority.LOW);

        tm.teleport(senderPlayer, target.getLocation(),
                "&aTeleportacja do &e" + target.getName() + "&a zakończona sukcesem.");
        return true;
    }
}