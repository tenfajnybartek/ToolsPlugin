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
        super("spawn", "Teleportuje na główny spawn", "/spawn", "tfbhc.cmd.spawn", null);
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
        ToolsPlugin plugin = ToolsPlugin.getInstance();

        ConfigManager configManager = plugin.getConfigManager();
        CooldownManager cooldownManager = plugin.getCooldownManager();
        TeleportManager teleportManager = plugin.getTeleportManager();

        // 1. Sprawdź cooldown
        if (cooldownManager.checkCooldown(player, "spawn")) {
            return true;
        }

        // 2. Pobierz lokalizację
        Location spawnLocation = configManager.getSpawnLocation();

        if (spawnLocation == null) {
            sendMessage(player, "&cGłówny Spawn nie został jeszcze ustawiony! Zgłoś to administracji.");
            return true;
        }

        // 3. Sprawdź czy świat jest załadowany (dodatkowe zabezpieczenie)
        if (spawnLocation.getWorld() == null) {
            sendMessage(player, "&cŚwiat spawnu nie jest załadowany! Zgłoś to administracji.");
            return true;
        }

        // 4. Teleportuj z delay
        teleportManager.teleport(player, spawnLocation, "&aPrzeteleportowano na główny Spawn!");

        // 5. Ustaw cooldown
        cooldownManager.setCooldown(player, "spawn");

        return true;
    }
}
