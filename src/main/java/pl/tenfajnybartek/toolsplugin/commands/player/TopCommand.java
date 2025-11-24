package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

public class TopCommand extends BaseCommand {

    public TopCommand() {
        super("top", "Teleportuje na najwyższy blok na danej pozycji", "/top", "tools.cmd.top", new String[]{"góra"});
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
        Location startLocation = player.getLocation();

        Location safeLocation = startLocation.getWorld().getHighestBlockAt(startLocation.getBlockX(), startLocation.getBlockZ()).getLocation();

        double x = safeLocation.getBlockX() + 0.5;
        double z = safeLocation.getBlockZ() + 0.5;
        double y = safeLocation.getBlockY() + 1;

        Location finalLocation = new Location(
                player.getWorld(),
                x,
                y,
                z,
                startLocation.getYaw(),
                startLocation.getPitch()
        );

        if (finalLocation.getBlock().getType() == Material.LAVA || finalLocation.getBlock().getType() == Material.FIRE) {
            sendMessage(sender, "&cBezpieczny blok na powierzchni jest niebezpieczny. Teleportacja anulowana.");
            return true;
        }

        player.teleport(finalLocation);
        sendMessage(sender, "&aTeleportowano na &eGórę&a!");

        return true;
    }
}
