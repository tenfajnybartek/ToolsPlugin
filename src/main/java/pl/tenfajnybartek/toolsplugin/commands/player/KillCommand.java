package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

import java.util.List;
import java.util.stream.Collectors;

public class KillCommand extends BaseCommand {

    public KillCommand() {
        super("kill", "Zabija gracza", "/kill [gracz]", "tfbhc.cmd.kill", new String[]{"zabić"});
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {

        Player target;

        // /kill - dla siebie
        if (args.length == 0) {
            if (!isPlayer(sender)) {
                sendMessage(sender, "&cTa komenda musi być użyta przez gracza lub z argumentem [gracz].");
                return true;
            }
            target = getPlayer(sender);
        }
        // /kill <gracz> - dla innego gracza
        else if (args.length == 1) {
            // Sprawdzenie uprawnienia dla innych
            if (!sender.hasPermission(perm("others"))) {
                sendMessage(sender, "&cNie masz uprawnień do zabijania innych graczy!");
                return true;
            }

            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sendMessage(sender, "&cGracz &e" + args[0] + " &cnie jest online!");
                return true;
            }
        }
        // Błąd użycia
        else {
            sendMessage(sender, "&cUżycie: " + getUsage());
            return true;
        }

        // Sprawdzenie, czy gracz jest już martwy (zapobieganie błędom)
        if (target.isDead()) {
            sendMessage(sender, "&cGracz &e" + target.getName() + " &cjest już martwy.");
            return true;
        }

        // Zabicie gracza
        target.setHealth(0.0);

        // Wysyłanie wiadomości zwrotnych
        if (target.equals(sender)) {
            sendMessage(sender, "&aZostałeś pomyślnie zabity!");
        } else {
            sendMessage(sender, "&aGracz &e" + target.getName() + " &azostał zabity!");
            sendMessage(target, "&cZostałeś zabity przez administratora!");
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender.hasPermission(perm("others"))) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return super.tabComplete(sender, args);
    }
}
