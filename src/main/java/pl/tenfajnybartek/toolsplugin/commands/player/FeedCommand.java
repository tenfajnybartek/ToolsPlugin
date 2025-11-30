package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
import pl.tenfajnybartek.toolsplugin.managers.CooldownManager;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

import java.util.List;
import java.util.stream.Collectors;

public class FeedCommand extends BaseCommand {

    public FeedCommand() {
        super("feed", "Karmi gracza do pełna", "/feed [gracz]", "tools.cmd.feed", new String[]{"eat"});
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {

        if (args.length == 0) {
            if (!isPlayer(sender)) {
                sendOnlyPlayer(sender);
                sendUsage(sender);
                return true;
            }

            Player player = getPlayer(sender);


            feedPlayer(player);
            sendMessage(sender, "&aZostałeś nakarmiony!");

            return true;
        }

        if (args.length == 1) {
            if (!sender.hasPermission(perm("others"))) {
                sendNoPermission(sender);
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sendPlayerOffline(sender, args[0]);
                return true;
            }

            feedPlayer(target);
            sendMessage(sender, "&aNakarmiono gracza &e" + target.getName());

            if (!target.equals(sender)) {
                sendMessage(target, "&aZostałeś nakarmiony przez administratora!");
            }

            return true;
        }

        sendUsage(sender);
        return true;
    }

    private void feedPlayer(Player player) {
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setExhaustion(0f);
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