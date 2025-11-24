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
        super("smite", "Uderza piorunem we wskazanego gracza", "/smite <gracz>", "tools.cmd.smite", new String[]{"piorun"});
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {

        if (args.length != 1) {
            sendUsage(sender);
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sendPlayerOffline(sender, args[0]);
            return true;
        }

        if (target.hasPermission(perm("bypass"))) {
            sendMessage(sender, "&cNie możesz uderzyć piorunem gracza &e" + target.getName() + " &c(posiada uprawnienie bypass).");
            return true;
        }

        Location targetLocation = target.getLocation();

        target.getWorld().strikeLightning(targetLocation);

        sendMessage(sender, String.format("&aGracz &e%s &azostał ukarany piorunem!", target.getName()));

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
