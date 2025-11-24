package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
import pl.tenfajnybartek.toolsplugin.managers.TeleportManager;
import pl.tenfajnybartek.toolsplugin.managers.UserManager;
import pl.tenfajnybartek.toolsplugin.objects.User;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

import java.util.List;
import java.util.stream.Collectors;

public class TpaCommand extends BaseCommand {

    public TpaCommand() {
        super("tpa", "Wysyła prośbę o teleportację do gracza", "/tpa <gracz>", "tools.cmd.tpa", null);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!isPlayer(sender)) {
            sendOnlyPlayer(sender);
            return true;
        }

        if (args.length != 1) {
            sendUsage(sender);
            return true;
        }

        Player player = getPlayer(sender);
        Player target = Bukkit.getPlayer(args[0]);
        TeleportManager tm = ToolsPlugin.getInstance().getTeleportManager();
        UserManager um = ToolsPlugin.getInstance().getUserManager();

        if (target == null || !target.isOnline()) {
            sendPlayerOffline(sender, args[0]);
            return true;
        }

        if (target.equals(player)) {
            sendMessage(player, "&cNie możesz wysłać prośby sam do siebie.");
            return true;
        }

        User targetUser = um.getUser(target);
        if (targetUser == null) {
            sendMessage(player, "&cWystąpił wewnętrzny błąd. Spróbuj się przelogować.");
            return true;
        }
        if (!targetUser.isTeleportToggle()) {
            sendMessage(player, "&cTen gracz wyłączył otrzymywanie próśb o teleportację.");
            return true;
        }

        if (tm.getTpaRequest(target) != null) {
            sendMessage(player, "&cTen gracz ma już oczekującą prośbę o teleportację.");
            return true;
        }

        tm.addTpaRequest(player, target);

        sendMessage(player, "&aWysłano prośbę o teleportację do &e" + target.getName() + "&a. Wygasa za 60s.");

        String targetMsg = "&e" + player.getName() + "&a prosi o teleportację do Ciebie. Wpisz &e/tpaccept&a, aby zaakceptować, lub &c/tpadeny&a, aby odrzucić.";
        sendMessage(target, targetMsg);

        return true;
    }
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // Podpowiadanie graczy
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return super.tabComplete(sender, args);
    }
}
