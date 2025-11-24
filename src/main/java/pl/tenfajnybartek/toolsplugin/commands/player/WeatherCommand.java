package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class WeatherCommand extends BaseCommand {

    private static final List<String> WEATHER_TYPES = Arrays.asList("clear", "rain", "thunder");

    public WeatherCommand() {
        super("weather", "Zmienia pogodę w świecie", "/weather <clear/rain/thunder>", "tools.cmd.weather", new String[]{"pogoda"});
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sendUsage(sender);
            return true;
        }

        String type = args[0].toLowerCase();

        if (!WEATHER_TYPES.contains(type)) {
            sendMessage(sender, "&cNieprawidłowy typ pogody. Użyj: &e&lclear, rain &cibądź &e&lthunder.");
            return true;
        }

        World world;
        if (isPlayer(sender)) {
            world = getPlayer(sender).getWorld();
        } else {
            world = Bukkit.getWorlds().get(0);
        }

        String status = "";

        switch (type) {
            case "clear":
                world.setStorm(false);
                world.setThundering(false);
                status = "&awyczyszczono";
                break;
            case "rain":
                world.setStorm(true);
                world.setThundering(false);
                status = "&awłączono deszcz";
                break;
            case "thunder":
                world.setStorm(true);
                world.setThundering(true);
                status = "&awłączono burzę";
                break;
            default:
                sendMessage(sender, "&cNieprawidłowy typ pogody.");
                return true;
        }

        sendMessage(sender, "&aPogoda w świecie &e" + world.getName() + " &azostała " + status + "!");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return WEATHER_TYPES.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return super.tabComplete(sender, args);
    }
}
