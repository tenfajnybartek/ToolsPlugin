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
        super("socialspy", "Włącza/wyłącza podsłuchiwanie prywatnych wiadomości", "/socialspy", "tools.socialspy", null);
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

        // Zmiana statusu
        boolean newState = !user.isSocialSpy();
        user.setSocialSpy(newState);

        // Asynchroniczny zapis zmiany do bazy danych
        userManager.saveUser(user, false);

        // Wiadomość zwrotna
        String status = newState ? "&aWŁĄCZONE" : "&cWYŁĄCZONE";
        sendMessage(player, "&7Podsłuchiwanie wiadomości jest teraz: " + status);

        return true;
    }
}