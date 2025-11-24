package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

public class DayCommand extends BaseCommand {

    private static final long TIME_DAY = 1000L;

    public DayCommand() {
        super("day", "Ustawia czas na dzień", "/day", "tools.cmd.day", null);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
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
