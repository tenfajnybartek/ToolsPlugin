package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
import pl.tenfajnybartek.toolsplugin.managers.MessageManager;
import pl.tenfajnybartek.toolsplugin.managers.UserManager;
import pl.tenfajnybartek.toolsplugin.objects.User;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

import java.util.UUID;

public class ReplyCommand extends BaseCommand {

    public ReplyCommand() {
        // Użycie: /r <wiadomość> (alias "r")
        super("reply", "Odpowiada ostatniemu rozmówcy", "/r <wiadomość>", "tools.msg", new String[]{"r"});
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!isPlayer(sender)) {
            sendMessage(sender, "&cTylko gracze mogą używać tej komendy.");
            return true;
        }

        if (args.length == 0) {
            // Wyświetlenie prawidłowego użycia komendy
            sendMessage(sender, "&cUżycie: " + getUsage());
            return true;
        }

        Player player = getPlayer(sender);
        UserManager userManager = ToolsPlugin.getInstance().getUserManager();
        User user = userManager.getUser(player);

        if (user == null) {
            sendMessage(player, "&cBłąd: Twoje dane użytkownika nie zostały załadowane. Spróbuj się przelogować.");
            return true;
        }

        // 1. Sprawdzenie, czy jest odbiorca /reply
        UUID lastTargetUuid = user.getLastMessageFrom();

        if (lastTargetUuid == null) {
            sendMessage(player, "&cNie masz komu odpisać. Nikt do Ciebie ostatnio nie pisał.");
            return true;
        }

        // 2. Pobranie gracza-odbiorcy (ostatniego rozmówcy)
        Player target = Bukkit.getPlayer(lastTargetUuid);

        if (target == null || !target.isOnline()) {
            sendMessage(player, "&cTwój ostatni rozmówca jest offline.");

            // Czyszczenie pola /reply i zapis asynchroniczny
            user.setLastMessageFrom(null);
            userManager.saveUser(user, false);
            return true;
        }

        // 3. Budowanie wiadomości (Użycie String.join jest czyste i poprawne)
        String message = String.join(" ", args);

        // 4. Wysyłanie przez MessageManager
        MessageManager messageManager = ToolsPlugin.getInstance().getMessageManager();

        // Zabezpieczenie przed pisaniem do samego siebie, choć to mało prawdopodobne w /r
        if (target.equals(player)) {
            sendMessage(player, "&cNie możesz wysłać wiadomości sam do siebie.");
            return true;
        }

        messageManager.sendPrivateMessage(player, target, message);

        return true;
    }
}