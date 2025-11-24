package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BurnCommand extends BaseCommand {

    public BurnCommand() {
        super("burn", "Podpala gracza na określoną liczbę sekund", "/burn <gracz> <sekundy>", "tools.cmd.burn", new String[]{"podpal"});
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {

        if (args.length != 2) {
            sendUsage(sender);
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sendPlayerOffline(sender, args[0]);
            return true;
        }

        int seconds;
        try {
            seconds = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sendMessage(sender, "&cNieprawidłowa wartość sekund. Oczekiwano liczby całkowitej.");
            return true;
        }

        if (seconds <= 0) {
            sendMessage(sender, "&cCzas musi być większy od zera.");
            return true;
        }

        if (target.equals(sender) || target.hasPermission(perm("bypass"))) {
            sendMessage(sender, "&cNie możesz podpalić &e" + target.getName() + " &c(posiada uprawnienie bypass lub jest wykonawcą komendy).");
            return true;
        }

        int ticks = seconds * 20;

        target.setFireTicks(ticks);

        sendMessage(sender, String.format("&aPodpaliłeś gracza &e%s &ana &6%d &asekund.", target.getName(), seconds));
        sendMessage(target, String.format("&cZostałeś podpalony przez &4%s &cna &6%d &csekund!", sender.getName(), seconds));

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
        if (args.length == 2) {
            return Arrays.asList("5", "10", "30").stream()
                    .filter(s -> s.startsWith(args[1]))
                    .collect(Collectors.toList());
        }
        return super.tabComplete(sender, args);
    }
}
