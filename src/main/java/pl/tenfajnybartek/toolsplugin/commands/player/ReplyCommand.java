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
        super("reply", "Odpowiada ostatniemu rozmówcy", "/r <wiadomość>", "tools.cmd.reply", new String[]{"r"});
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!isPlayer(sender)) {
            sendOnlyPlayer(sender);
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        Player player = getPlayer(sender);
        UserManager userManager = ToolsPlugin.getInstance().getUserManager();
        User user = userManager.getUser(player);

        if (user == null) {
            sendMessage(player, "&cBłąd: Twoje dane użytkownika nie zostały załadowane. Spróbuj się przelogować.");
            return true;
        }

        UUID lastTargetUuid = user.getLastMessageFrom();
        if (lastTargetUuid == null) {
            sendMessage(player, "&cNie masz komu odpisać. Nikt do Ciebie ostatnio nie pisał.");
            return true;
        }

        Player target = Bukkit.getPlayer(lastTargetUuid);
        if (target == null || !target.isOnline()) {
            sendMessage(player, "&cTwój ostatni rozmówca jest offline.");
            user.setLastMessageFrom(null);
            userManager.saveUserAsync(user);
            return true;
        }

        if (target.equals(player)) {
            sendMessage(player, "&cNie możesz wysłać wiadomości sam do siebie.");
            return true;
        }

        String message = String.join(" ", args);
        MessageManager messageManager = ToolsPlugin.getInstance().getMessageManager();
        messageManager.sendPrivateMessage(player, target, message);

        return true;
    }
}