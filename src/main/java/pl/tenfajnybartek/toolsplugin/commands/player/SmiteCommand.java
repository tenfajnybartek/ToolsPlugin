package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

import java.util.List;
import java.util.stream.Collectors;

public class SmiteCommand extends BaseCommand {

    public SmiteCommand() {
        super("smite", "Uderza piorunem we wskazanego gracza", "/smite <gracz>", "tfbhc.cmd.smite", new String[]{"piorun"});
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {

        // Komenda wymaga dokładnie 1 argumentu
        if (args.length != 1) {
            sendMessage(sender, "&cUżycie: " + getUsage());
            return true;
        }

        // 1. Walidacja gracza docelowego
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sendMessage(sender, "&cGracz &e" + args[0] + " &cnie jest online!");
            return true;
        }

        // 2. Blokada dla admina z bypass
        if (target.hasPermission(perm("bypass"))) {
            sendMessage(sender, "&cNie możesz uderzyć piorunem gracza &e" + target.getName() + " &c(posiada uprawnienie bypass).");
            return true;
        }

        // 3. Uderzenie piorunem
        Location targetLocation = target.getLocation();

        // Uderzenie piorunem: strikeLightning() zadaje obrażenia, strikeLightningEffect() tylko efekt.
        target.getWorld().strikeLightning(targetLocation);

        // 4. Wysyłanie wiadomości zwrotnych
        sendMessage(sender, String.format("&aGracz &e%s &azostał ukarany piorunem!", target.getName()));

        // Opcjonalna wiadomość dla gracza
        if (!target.equals(sender)) {
            sendMessage(target, "&4Zostałeś uderzony piorunem przez &c" + sender.getName() + "!");
        }

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
