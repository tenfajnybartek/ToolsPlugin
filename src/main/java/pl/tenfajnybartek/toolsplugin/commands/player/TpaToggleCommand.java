package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
import pl.tenfajnybartek.toolsplugin.managers.UserManager;
import pl.tenfajnybartek.toolsplugin.objects.User;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

public class TpaToggleCommand extends BaseCommand {

    public TpaToggleCommand() {
        super("tpatoggle", "Włącza/wyłącza otrzymywanie próśb o teleportację", "/tpatoggle", "tools.tpatoggle", new String[]{"tptoggle"});
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!isPlayer(sender)) {
            sendMessage(sender, "&cTylko gracze mogą używać tej komendy.");
            return true;
        }

        Player player = getPlayer(sender);
        UserManager userManager = ToolsPlugin.getInstance().getUserManager();
        User user = userManager.getUser(player);

        if (user == null) {
            sendMessage(player, "&cBłąd: Twoje dane użytkownika nie zostały załadowane. Spróbuj się przelogować.");
            return true;
        }

        boolean newState = !user.isTeleportToggle();
        user.setTeleportToggle(newState);

        // NOWE
        userManager.saveUserAsync(user);

        String status = newState ? "&aWŁĄCZONE" : "&cWYŁĄCZONE";
        sendMessage(player, "&7Otrzymywanie próśb TPA jest teraz: " + status);
        return true;
    }
}