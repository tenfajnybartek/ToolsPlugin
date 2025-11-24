package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
import pl.tenfajnybartek.toolsplugin.managers.ConfigManager;
import pl.tenfajnybartek.toolsplugin.managers.CooldownManager;
import pl.tenfajnybartek.toolsplugin.managers.TeleportManager;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

public class SpawnCommand extends BaseCommand {

    public SpawnCommand() {
        super("spawn", "Teleportuje na główny spawn", "/spawn", "tools.cmd.spawn", null);
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
        ToolsPlugin plugin = ToolsPlugin.getInstance();

        ConfigManager configManager = plugin.getConfigManager();
        TeleportManager teleportManager = plugin.getTeleportManager();

        Location spawnLocation = configManager.getSpawnLocation();

        if (spawnLocation == null) {
            sendMessage(player, "&cGłówny Spawn nie został jeszcze ustawiony! Zgłoś to administracji.");
            return true;
        }

        if (spawnLocation.getWorld() == null) {
            sendMessage(player, "&cŚwiat spawnu nie jest załadowany! Zgłoś to administracji.");
            return true;
        }

        teleportManager.teleport(player, spawnLocation, "&aPrzeteleportowano na główny Spawn!");


        return true;
    }
}
