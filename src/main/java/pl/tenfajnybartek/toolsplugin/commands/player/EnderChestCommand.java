package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

import java.util.List;
import java.util.stream.Collectors;

public class EnderChestCommand extends BaseCommand {

    public EnderChestCommand() {
        super("enderchest", "Otwiera ender chest gracza", "/enderchest [gracz]", "tfbhc.cmd.enderchest", new String[]{"ec", "echest"});
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!isPlayer(sender)) {
            sendMessage(sender, "&cTa komenda może być użyta tylko przez gracza!");
            return true;
        }

        Player player = getPlayer(sender);

        // /ec - otwiera swój ender chest
        if (args.length == 0) {
            player.openInventory(player.getEnderChest());
            sendMessage(sender, "&aOtwarto twój ender chest!");
            return true;
        }

        // /ec <gracz> - otwiera ender chest innego gracza
        if (args.length == 1) {
            if (!sender.hasPermission(perm("others"))) {
                sendMessage(sender, "&cNie masz uprawnień do przeglądania ender chestów innych graczy!");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sendMessage(sender, "&cGracz &e" + args[0] + " &cnie jest online!");
                return true;
            }

            player.openInventory(target.getEnderChest());
            sendMessage(sender, "&aOtwarto ender chest gracza &e" + target.getName());
            return true;
        }

        sendMessage(sender, "&cUżycie: " + getUsage());
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
