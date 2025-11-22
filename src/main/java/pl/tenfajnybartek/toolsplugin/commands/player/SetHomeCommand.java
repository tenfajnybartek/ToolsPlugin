package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
import pl.tenfajnybartek.toolsplugin.managers.HomeManager;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

public class SetHomeCommand extends BaseCommand {

    public SetHomeCommand() {
        super("sethome", "Ustawia dom", "/sethome [nazwa]", "tfbhc.cmd.sethome", null);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!isPlayer(sender)) {
            sendMessage(sender, "&cTa komenda może być użyta tylko przez gracza!");
            return true;
        }

        Player player = getPlayer(sender);
        HomeManager homeManager = ToolsPlugin.getInstance().getHomeManager();

        String homeName = args.length == 0 ? "home" : args[0];

        // Sprawdź czy gracz ma już ten home (nadpisywanie)
        boolean isUpdate = homeManager.hasHome(player, homeName);

        // Utwórz/zaktualizuj home
        Location location = player.getLocation();
        boolean success = homeManager.createHome(player, homeName, location);

        if (!success) {
            int maxHomes = homeManager.getMaxHomes(player);
            sendMessage(sender, "&cOsiągnąłeś maksymalną liczbę domów! &7(" + maxHomes + ")");
            sendMessage(sender, "&eTwoje domy: &f" + String.join("&7, &f", homeManager.getHomeNames(player)));
            return true;
        }

        if (isUpdate) {
            sendMessage(sender, "&aZaktualizowano lokalizację domu &e" + homeName);
        } else {
            int currentHomes = homeManager.getHomeCount(player);
            int maxHomes = homeManager.getMaxHomes(player);
            sendMessage(sender, "&aUtworzono dom &e" + homeName + " &7(" + currentHomes + "/" + maxHomes + ")");
        }

        return true;
    }
}
