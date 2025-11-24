package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
import pl.tenfajnybartek.toolsplugin.managers.UserManager;
import pl.tenfajnybartek.toolsplugin.objects.User;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

import java.util.Collections;
import java.util.List;

public class SocialSpyCommand extends BaseCommand {

    public SocialSpyCommand() {
        super("socialspy", "Włącza/wyłącza podsłuchiwanie prywatnych wiadomości", "/socialspy", "tools.cmd.socialspy", null);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!isPlayer(sender)) {
            sendOnlyPlayer(sender);
            return true;
        }

        Player player = getPlayer(sender);
        UserManager userManager = ToolsPlugin.getInstance().getUserManager();
        User user = userManager.getUser(player);

        if (user == null) {
            sendMessage(player, "&cBłąd: Twoje dane użytkownika nie zostały załadowane. Spróbuj się przelogować.");
            return true;
        }

        boolean newState = !user.isSocialSpy();
        user.setSocialSpy(newState);

        userManager.saveUserAsync(user);

        String status = newState ? "&aWŁĄCZONE" : "&cWYŁĄCZONE";
        sendMessage(player, "&7Podsłuchiwanie wiadomości jest teraz: " + status);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}