package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

public class DayCommand extends BaseCommand {

    // 1000 ticków to początek dnia (wschód słońca)
    private static final long TIME_DAY = 1000L;

    public DayCommand() {
        super("day", "Ustawia czas na dzień", "/day", "tfbhc.cmd.day", null);
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

        world.setTime(TIME_DAY);
        sendMessage(sender, "&aUstawiono czas w świecie &e" + world.getName() + " &ana dzień!");

        return true;
    }
}
