package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

public class NightCommand extends BaseCommand {

    // 13000 ticków to początek nocy
    private static final long TIME_NIGHT = 13000L;

    public NightCommand() {
        super("night", "Ustawia czas na noc", "/night", "tfbhc.cmd.night", null);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // Bierzemy świat, w którym znajduje się gracz, lub główny świat
        World world;
        if (isPlayer(sender)) {
            world = getPlayer(sender).getWorld();
        } else {
            world = Bukkit.getWorlds().get(0);
        }

        world.setTime(TIME_NIGHT);
        sendMessage(sender, "&aUstawiono czas w świecie &e" + world.getName() + " &ana noc!");

        return true;
    }
}
