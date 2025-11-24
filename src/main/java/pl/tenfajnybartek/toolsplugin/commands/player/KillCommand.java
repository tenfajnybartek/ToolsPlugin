package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

import java.util.List;
import java.util.stream.Collectors;

public class KillCommand extends BaseCommand {

    public KillCommand() {
        super("kill", "Zabija gracza", "/kill [gracz]", "tools.cmd.kill", new String[]{"zabić"});
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {

        Player target;

        if (args.length == 0) {
            if (!isPlayer(sender)) {
                sendOnlyPlayer(sender);
                return true;
            }
            target = getPlayer(sender);
        }
        else if (args.length == 1) {
            if (!sender.hasPermission(perm("others"))) {
                sendNoPermission(sender);
                return true;
            }

            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sendPlayerOffline(sender, args[0]);
                return true;
            }
        }
        else {
            sendUsage(sender);
            return true;
        }

        if (target.isDead()) {
            sendMessage(sender, "&cGracz &e" + target.getName() + " &cjest już martwy.");
            return true;
        }

        target.setHealth(0.0);

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
