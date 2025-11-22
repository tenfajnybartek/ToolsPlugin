package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
import pl.tenfajnybartek.toolsplugin.managers.CooldownManager;
import pl.tenfajnybartek.toolsplugin.managers.HomeManager;
import pl.tenfajnybartek.toolsplugin.managers.TeleportManager;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

import java.util.List;
import java.util.stream.Collectors;

public class HomeCommand extends BaseCommand {

    public HomeCommand() {
        super("home", "Teleportuje do domu", "/home [nazwa]", "tfbhc.cmd.home", new String[]{"h"});
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!isPlayer(sender)) {
            sendMessage(sender, "&cTa komenda może być użyta tylko przez gracza!");
            return true;
        }

        Player player = getPlayer(sender);
        HomeManager homeManager = ToolsPlugin.getInstance().getHomeManager();
        TeleportManager teleportManager = ToolsPlugin.getInstance().getTeleportManager();
        CooldownManager cooldownManager = ToolsPlugin.getInstance().getCooldownManager();

        // /home - teleportuj do domyślnego home ("home")
        String homeName = args.length == 0 ? "home" : args[0];

        if (!homeManager.hasHome(player, homeName)) {
            if (homeManager.getHomeCount(player) == 0) {
                sendMessage(sender, "&cNie masz żadnych domów! Użyj &e/sethome &caby utworzyć.");
            } else {
                sendMessage(sender, "&cNie masz domu o nazwie &e" + homeName + "&c!");
                sendMessage(sender, "&eTwoje domy: &f" + String.join("&7, &f", homeManager.getHomeNames(player)));
            }
            return true;
        }

        // Sprawdź cooldown
        if (cooldownManager.checkCooldown(player, "home")) {
            return true;
        }

        Location homeLocation = homeManager.getHome(player, homeName);

        // Teleportuj z delay
        teleportManager.teleport(player, homeLocation, "&aPrzeteleportowano do domu &e" + homeName);

        // Ustaw cooldown
        cooldownManager.setCooldown(player, "home");

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
