package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TimeCommand extends BaseCommand {

    private static final List<String> SUB_COMMANDS = Arrays.asList("set", "add");
    private static final List<String> TIME_ALIASES = Arrays.asList("day", "night", "noon", "midnight", "6000", "18000");

    public TimeCommand() {
        super("time", "Zmienia czas w świecie", "/time <set/add> <czas>", "tools.cmd.time", new String[]{"czas"});
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        World world;
        if (isPlayer(sender)) {
            world = getPlayer(sender).getWorld();
        } else {
            world = Bukkit.getWorlds().get(0);
        }

        if (args.length < 2) {
            sendUsage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        try {
            long amount = parseTimeArgument(args[1]);

            if (subCommand.equals("set")) {
                world.setTime(amount);
                sendMessage(sender, "&aUstawiono czas w świecie &e" + world.getName() + " &ana &6" + amount + " ticków.");
            } else if (subCommand.equals("add")) {
                world.setTime(world.getTime() + amount);
                sendMessage(sender, "&aDodano &6" + amount + " ticków &ado czasu w świecie &e" + world.getName() + ".");
            } else {
                sendMessage(sender, "&cNieznana podkomenda: &e" + args[0] + ". &cUżyj: set lub add.");
                return true;
            }

        } catch (NumberFormatException e) {
            sendMessage(sender, "&cNieprawidłowy format czasu! Oczekiwano liczby ticków lub aliasu (np. day, night).");
        }

        return true;
    }

    private long parseTimeArgument(String timeArg) throws NumberFormatException {
        timeArg = timeArg.toLowerCase();

        switch (timeArg) {
            case "day":
                return 1000L;
            case "noon":
                return 6000L;
            case "night":
                return 13000L;
            case "midnight":
                return 18000L;
            default:
                return Long.parseLong(timeArg);
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return SUB_COMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            return TIME_ALIASES.stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return super.tabComplete(sender, args);
    }
}
