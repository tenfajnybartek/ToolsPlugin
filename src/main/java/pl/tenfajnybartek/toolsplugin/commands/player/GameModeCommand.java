package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GameModeCommand extends BaseCommand {

    public GameModeCommand() {
        super("gamemode", "Zmiana trybu gry graczy", "/gamemode <tryb> [gracz]", "tfbhc.cmd.gamemode", new String[]{"gm", "gmode"});
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sendMessage(sender, "&cUżycie: " + getUsage());
            return true;
        }

        GameMode gameMode = parseGameMode(args[0]);
        if (gameMode == null) {
            sendMessage(sender, "&cNieprawidłowy tryb gry! &7Użyj: survival, creative, adventure, spectator");
            return true;
        }

        Player target;

        if (args.length >= 2) {
            if (!sender.hasPermission(perm("others"))) {
                sendMessage(sender, "&cNie masz uprawnień do zmiany trybu gry innym graczom!");
                return true;
            }

            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sendMessage(sender, "&cGracz nie jest online!");
                return true;
            }
        } else {
            if (!isPlayer(sender)) {
                sendMessage(sender, "&cMusisz określić gracza z konsoli!");
                return true;
            }
            target = getPlayer(sender);
        }

        target.setGameMode(gameMode);
        sendMessage(sender, "&aTryb gry gracza &e" + target.getName() + " &azostał zmieniony na &e" + gameMode.name());

        if (!target.equals(sender)) {
            sendMessage(target, "&aTwój tryb gry został zmieniony na &e" + gameMode.name());
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("survival", "creative", "adventure", "spectator", "0", "1", "2", "3"));
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && sender.hasPermission(perm("others"))) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return completions;
    }

    private GameMode parseGameMode(String input) {
        switch (input.toLowerCase()) {
            case "0":
            case "survival":
            case "s":
                return GameMode.SURVIVAL;
            case "1":
            case "creative":
            case "c":
                return GameMode.CREATIVE;
            case "2":
            case "adventure":
            case "a":
                return GameMode.ADVENTURE;
            case "3":
            case "spectator":
            case "sp":
                return GameMode.SPECTATOR;
            default:
                return null;
        }
    }
}