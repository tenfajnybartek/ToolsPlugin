package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TeleportCommand extends BaseCommand {

    public TeleportCommand() {
        super("teleport", "Teleportacja graczy", "/tp <gracz> | /tp <g1> <g2> | /tp <gracz> <x> <y> <z> [world]", "tools.cmd.tp", new String[]{"tp"});
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        if (args.length == 1) {
            if (!isPlayer(sender)) {
                sendOnlyPlayer(sender);
                return true;
            }

            Player player = getPlayer(sender);
            Player target = Bukkit.getPlayer(args[0]);

            if (target == null) {
                sendPlayerOffline(sender, args[0]);
                return true;
            }

            player.teleport(target.getLocation());
            sendMessage(sender, "&aTeleportowano do gracza &e" + target.getName());
            return true;
        }

        // /tp <g1> <g2> - teleportuje g1 do g2
        if (args.length == 2) {
            if (!sender.hasPermission(perm("others"))) {
                sendNoPermission(sender);
                return true;
            }

            Player player1 = Bukkit.getPlayer(args[0]);
            Player player2 = Bukkit.getPlayer(args[1]);

            if (player1 == null) {
                sendPlayerOffline(sender, args[0]);
                return true;
            }

            if (player2 == null) {
                sendPlayerOffline(sender, args[1]);
                return true;
            }

            player1.teleport(player2.getLocation());
            sendMessage(sender, "&aTeleportowano gracza &e" + player1.getName() + " &ado gracza &e" + player2.getName());
            sendMessage(player1, "&aZostałeś przeteleportowany do gracza &e" + player2.getName());
            return true;
        }

        if (args.length >= 4) {
            if (!sender.hasPermission(perm("coords"))) {
                sendNoPermission(sender);
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sendPlayerOffline(sender, args[0]);
                return true;
            }

            try {
                double x = parseCoordinate(args[1], target.getLocation().getX());
                double y = parseCoordinate(args[2], target.getLocation().getY());
                double z = parseCoordinate(args[3], target.getLocation().getZ());

                World world = target.getWorld();

                if (args.length >= 5) {
                    World specifiedWorld = Bukkit.getWorld(args[4]);
                    if (specifiedWorld != null) {
                        world = specifiedWorld;
                    } else {
                        sendMessage(sender, "&cŚwiat &e" + args[4] + " &cnie istnieje!");
                        return true;
                    }
                }

                Location location = new Location(world, x, y, z, target.getLocation().getYaw(), target.getLocation().getPitch());
                target.teleport(location);

                sendMessage(sender, "&aTeleportowano gracza &e" + target.getName() + " &ana koordynaty &e" +
                        String.format("%.1f, %.1f, %.1f", x, y, z) + (args.length >= 5 ? " &aw świecie &e" + world.getName() : ""));

                if (!target.equals(sender)) {
                    sendMessage(target, "&aZostałeś przeteleportowany na koordynaty &e" +
                            String.format("%.1f, %.1f, %.1f", x, y, z));
                }

            } catch (NumberFormatException e) {
                sendMessage(sender, "&cNieprawidłowe koordynaty! Użyj liczb lub relatywnych wartości (~)");
                return true;
            }

            return true;
        }

        sendUsage(sender);
        return true;
    }

    private double parseCoordinate(String input, double current) throws NumberFormatException {
        if (input.equals("~")) {
            return current;
        }

        if (input.startsWith("~")) {
            double offset = Double.parseDouble(input.substring(1));
            return current + offset;
        }

        return Double.parseDouble(input);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            Player firstPlayer = Bukkit.getPlayer(args[0]);
            if (firstPlayer != null) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args.length >= 2 && args.length <= 4) {
            if (isPlayer(sender)) {
                Player player = getPlayer(sender);
                Location loc = player.getLocation();

                if (args.length == 2) {
                    completions.add(String.valueOf((int) loc.getX()));
                    completions.add("~");
                } else if (args.length == 3) {
                    completions.add(String.valueOf((int) loc.getY()));
                    completions.add("~");
                } else if (args.length == 4) {
                    completions.add(String.valueOf((int) loc.getZ()));
                    completions.add("~");
                }
            }
        }

        if (args.length == 5) {
            return Bukkit.getWorlds().stream()
                    .map(World::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[4].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return completions;
    }
}
