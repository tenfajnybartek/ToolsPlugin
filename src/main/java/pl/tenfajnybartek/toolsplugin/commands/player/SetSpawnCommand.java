package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
import pl.tenfajnybartek.toolsplugin.managers.ConfigManager;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

public class SetSpawnCommand extends BaseCommand {

    public SetSpawnCommand() {
        super("setspawn", "Ustawia główny punkt spawnu", "/setspawn", "tools.cmd.setspawn", null);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!isPlayer(sender)) {
            sendOnlyPlayer(sender);
            return true;
        }

        if (args.length != 0) {
            sendUsage(sender);
            return true;
        }

        Player player = getPlayer(sender);
        ConfigManager configManager = ToolsPlugin.getInstance().getConfigManager();

        Location location = player.getLocation();

        configManager.setSpawnLocation(location);

        sendMessage(sender, "&aUstawiono główny Spawn na Twojej aktualnej lokalizacji: &e" + location.getWorld().getName());
        return true;
    }
}
