package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

import java.util.List;
import java.util.stream.Collectors;

public class EnderChestCommand extends BaseCommand {

    public EnderChestCommand() {
        super("enderchest", "Otwiera ender chest gracza", "/enderchest [gracz]", "tools.cmd.enderchest", new String[]{"ec", "echest"});
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!isPlayer(sender)) {
            sendOnlyPlayer(sender);
            return true;
        }

        Player player = getPlayer(sender);

        if (args.length == 0) {
            player.openInventory(player.getEnderChest());
            sendMessage(sender, "&aOtwarto twój ender chest!");
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

            player.openInventory(target.getEnderChest());
            sendMessage(sender, "&aOtwarto ender chest gracza &e" + target.getName());
            return true;
        }

        sendUsage(sender);
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
