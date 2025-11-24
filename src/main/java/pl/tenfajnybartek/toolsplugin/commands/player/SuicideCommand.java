package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

public class SuicideCommand extends BaseCommand {

    public SuicideCommand() {
        super("suicide", "Zabija samego siebie", "/suicide", "tools.cmd.suicide", new String[]{"samobójstwo"});
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {

        if (!isPlayer(sender)) {
            sendOnlyPlayer(sender);
            return true;
        }

        if (args.length > 0) {
            sendUsage(sender);
            return true;
        }

        Player player = getPlayer(sender);

        if (player.isInvulnerable() || player.isDead()) {
            sendMessage(sender, "&cNie możesz się zabić w tym momencie.");
            return true;
        }

        player.setHealth(0.0);

        sendMessage(sender, "&cPopełniłeś samobójstwo.");

        return true;
    }
}
