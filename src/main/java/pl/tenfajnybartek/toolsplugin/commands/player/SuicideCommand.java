package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

public class SuicideCommand extends BaseCommand {

    public SuicideCommand() {
        super("suicide", "Zabija samego siebie", "/suicide", "tfbhc.cmd.suicide", new String[]{"samobójstwo"});
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {

        if (!isPlayer(sender)) {
            sendMessage(sender, "&cTa komenda może być użyta tylko przez gracza!");
            return true;
        }

        if (args.length > 0) {
            sendMessage(sender, "&cUżycie: " + getUsage());
            return true;
        }

        Player player = getPlayer(sender);

        // Sprawdzenie, czy gracz nie jest w trybie Boga lub już martwy
        if (player.isInvulnerable() || player.isDead()) {
            sendMessage(sender, "&cNie możesz się zabić w tym momencie.");
            return true;
        }

        // Zabicie gracza (ustawienie HP na 0)
        player.setHealth(0.0);

        sendMessage(sender, "&cPopełniłeś samobójstwo.");

        return true;
    }
}
