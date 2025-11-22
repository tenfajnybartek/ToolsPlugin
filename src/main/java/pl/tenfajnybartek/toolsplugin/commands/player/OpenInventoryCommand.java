package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

import java.util.List;
import java.util.stream.Collectors;

public class OpenInventoryCommand extends BaseCommand {

    public OpenInventoryCommand() {
        super("openinventory", "Otwiera ekwipunek gracza", "/openinventory <gracz>", "tfbhc.cmd.openinv", new String[]{"openinv", "invsee", "oi"});
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!isPlayer(sender)) {
            sendMessage(sender, "&cTa komenda może być użyta tylko przez gracza!");
            return true;
        }

        if (args.length == 0) {
            sendMessage(sender, "&cUżycie: " + getUsage());
            return true;
        }

        Player player = getPlayer(sender);
        Player target = Bukkit.getPlayer(args[0]);

        if (target == null) {
            sendMessage(sender, "&cGracz &e" + args[0] + " &cnie jest online!");
            return true;
        }

        player.openInventory(target.getInventory());
        sendMessage(sender, "&aOtwarto ekwipunek gracza &e" + target.getName());
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
        return super.tabComplete(sender, args);
    }
}
