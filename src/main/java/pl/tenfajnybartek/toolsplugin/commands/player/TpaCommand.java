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
        super("tpa", "Wysyła prośbę o teleportację do gracza", "/tpa <gracz>", "tools.tpa", null);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!isPlayer(sender)) {
            sendMessage(sender, "&cTylko gracze mogą używać tej komendy.");
            return true;
        }

        if (args.length != 1) {
            sendMessage(sender, "&cUżycie: " + getUsage());
            return true;
        }

        Player player = getPlayer(sender);
        Player target = Bukkit.getPlayer(args[0]);
        TeleportManager tm = ToolsPlugin.getInstance().getTeleportManager();
        UserManager um = ToolsPlugin.getInstance().getUserManager();

        if (target == null || !target.isOnline()) {
            sendMessage(player, "&cGracz &e" + args[0] + "&c jest offline lub nie istnieje.");
            return true;
        }

        if (target.equals(player)) {
            sendMessage(player, "&cNie możesz wysłać prośby sam do siebie.");
            return true;
        }

        // 1. Sprawdzenie tpatoggle u celu
        User targetUser = um.getUser(target);
        if (targetUser == null) {
            sendMessage(player, "&cWystąpił wewnętrzny błąd. Spróbuj się przelogować.");
            return true;
        }
        if (!targetUser.isTeleportToggle()) {
            sendMessage(player, "&cTen gracz wyłączył otrzymywanie próśb o teleportację.");
            return true;
        }

        // 2. Sprawdzenie, czy prośba już istnieje
        if (tm.getTpaRequest(target) != null) {
            sendMessage(player, "&cTen gracz ma już oczekującą prośbę o teleportację.");
            return true;
        }

        // 3. Dodanie prośby i powiadomienie
        tm.addTpaRequest(player, target);

        sendMessage(player, "&aWysłano prośbę o teleportację do &e" + target.getName() + "&a. Wygasa za 60s.");

        // Wiadomość dla odbiorcy z instrukcją
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
