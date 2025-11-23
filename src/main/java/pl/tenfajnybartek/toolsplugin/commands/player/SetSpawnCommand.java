package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
import pl.tenfajnybartek.toolsplugin.managers.ConfigManager;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

public class SetSpawnCommand extends BaseCommand {

    public SetSpawnCommand() {
        super("setspawn", "Ustawia główny punkt spawnu", "/setspawn", "tfbhc.cmd.setspawn", null);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!isPlayer(sender)) {
            sendMessage(sender, "&cTa komenda może być użyta tylko przez gracza!");
            return true;
        }

        if (args.length != 0) {
            sendMessage(sender, "&cUżycie: " + getUsage());
            return true;
        }

        Player player = getPlayer(sender);
        ConfigManager configManager = ToolsPlugin.getInstance().getConfigManager();

        Location location = player.getLocation();

        // Zapis lokalizacji Spawna
        configManager.setSpawnLocation(location);

        sendMessage(sender, "&aUstawiono główny Spawn na Twojej aktualnej lokalizacji: &e" + location.getWorld().getName());
        return true;
    }
}
