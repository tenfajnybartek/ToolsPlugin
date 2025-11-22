package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
import pl.tenfajnybartek.toolsplugin.managers.HomeManager;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

import java.util.List;
import java.util.stream.Collectors;

public class DelHomeCommand extends BaseCommand {

    public DelHomeCommand() {
        super("delhome", "Usuwa dom", "/delhome <nazwa>", "tfbhc.cmd.delhome", new String[]{"removehome"});
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!isPlayer(sender)) {
            sendMessage(sender, "&cTa komenda może być użyta tylko przez gracza!");
            return true;
        }

        if (args.length != 1) {
            sendMessage(sender, "&cUżycie: " + getUsage());
            return true;
        }

        Player player = getPlayer(sender);
        String homeName = args[0];
        HomeManager homeManager = ToolsPlugin.getInstance().getHomeManager();

        if (!homeManager.hasHome(player, homeName)) {
            sendMessage(sender, "&cNie masz domu o nazwie &e" + homeName + "&c!");
            return true;
        }

        homeManager.deleteHome(player, homeName);
        sendMessage(sender, "&aUsunięto dom &e" + homeName);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && isPlayer(sender)) {
            Player player = getPlayer(sender);
            HomeManager homeManager = ToolsPlugin.getInstance().getHomeManager();
            return homeManager.getHomeNames(player).stream()
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return super.tabComplete(sender, args);
    }
}
