package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SpeedCommand extends BaseCommand {

    private static final List<String> SPEED_TYPES = Arrays.asList("walk", "fly");

    public SpeedCommand() {
        super("speed", "Zmienia prędkość chodzenia/latania", "/speed <walk/fly> <1-10> [gracz]", "tools.cmd.speed", new String[]{"prędkość"});
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendUsage(sender);
            return true;
        }

        String type = args[0].toLowerCase();
        int speedLevel;

        if (!SPEED_TYPES.contains(type)) {
            sendMessage(sender, "&cNieprawidłowy typ prędkości. Użyj: &e&lwalk &cibądź &e&lfly.");
            return true;
        }

        try {
            speedLevel = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sendMessage(sender, "&cNieprawidłowa wartość prędkości. Oczekiwano liczby z zakresu 1-10.");
            return true;
        }

        if (speedLevel < 1 || speedLevel > 10) {
            sendMessage(sender, "&cPrędkość musi być w zakresie &e1-10.");
            return true;
        }

        Player target;

        if (args.length == 2) {
            if (!isPlayer(sender)) {
                sendMessage(sender, "&cTa komenda musi być użyta przez gracza lub z argumentem [gracz].");
                return true;
            }
            target = getPlayer(sender);

        } else if (args.length == 3) {
            if (!sender.hasPermission(perm("others"))) {
                sendNoPermission(sender);
                return true;
            }

            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sendPlayerOffline(sender, args[2]);
                return true;
            }
        } else {
            sendUsage(sender);
            return true;
        }

        float speedValue = (float) speedLevel / 10.0f;

        if (type.equals("walk")) {
            target.setWalkSpeed(speedValue);
        } else {
            target.setFlySpeed(speedValue);
        }

        String typeName = type.equals("walk") ? "chodzenia" : "latania";

        if (target.equals(sender)) {
            sendMessage(sender, "&aUstawiono Twoją prędkość &e" + typeName + " &ana &6" + speedLevel + "&a/10.");
        } else {
            sendMessage(sender, "&aUstawiono prędkość &e" + typeName + " &agracza &e" + target.getName() + " &ana &6" + speedLevel + "&a/10.");
            sendMessage(target, "&aTwoja prędkość &e" + typeName + " &azostała ustawiona przez administratora na &6" + speedLevel + "&a/10.");
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return SPEED_TYPES.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            return Arrays.asList("1", "5", "10").stream()
                    .filter(s -> s.startsWith(args[1]))
                    .collect(Collectors.toList());
        }
        if (args.length == 3 && sender.hasPermission(perm("others"))) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return super.tabComplete(sender, args);
    }
}
