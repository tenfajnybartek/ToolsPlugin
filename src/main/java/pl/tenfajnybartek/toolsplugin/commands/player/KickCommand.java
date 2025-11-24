package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;
import pl.tenfajnybartek.toolsplugin.utils.ColorUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class KickCommand extends BaseCommand {

    public KickCommand() {
        super("kick", "Wyrzuca gracza z serwera", "/kick <gracz> [powód]", "tools.cmd.kick", null);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sendPlayerOffline(sender, args[0]);
            return true;
        }

        if (target.equals(sender) || target.hasPermission(perm("bypass"))) {
            if (target.equals(sender)) {
                sendMessage(sender, "&cNie możesz wyrzucić samego siebie tą komendą.");
            } else {
                sendMessage(sender, "&cNie możesz wyrzucić gracza &e" + target.getName() + " &c(posiada uprawnienie bypass).");
            }
            return true;
        }

        String reason;
        if (args.length >= 2) {
            reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        } else {
            reason = "Zostałeś wyrzucony z serwera. Spróbuj dołączyć ponownie!";
        }

        String finalReason = ColorUtils.colorize("&cZostałeś wyrzucony przez &4" + sender.getName() + ":&r\n\n" + reason);
        target.kickPlayer(finalReason);

        sendMessage(sender, "&aGracz &e" + target.getName() + " &azostał wyrzucony z powodu: &f" + reason);

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            return Arrays.asList("Cheating", "Spam", "Offense").stream()
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return super.tabComplete(sender, args);
    }
}
