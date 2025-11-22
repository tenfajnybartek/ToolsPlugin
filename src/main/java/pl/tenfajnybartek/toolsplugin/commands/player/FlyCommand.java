package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

import java.util.List;
import java.util.stream.Collectors;

public class FlyCommand extends BaseCommand {

    public FlyCommand() {
        super("fly", "Włącza/wyłącza latanie", "/fly [gracz]", "tfbhc.cmd.fly", new String[]{"flight"});
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // /fly - przełącza latanie dla siebie
        if (args.length == 0) {
            if (!isPlayer(sender)) {
                sendMessage(sender, "&cTa komenda może być użyta tylko przez gracza!");
                sendMessage(sender, "&eUżycie: " + getUsage());
                return true;
            }

            Player player = getPlayer(sender);
            toggleFly(player);

            if (player.getAllowFlight()) {
                sendMessage(sender, "&aLatanie zostało &ewłączone");
            } else {
                sendMessage(sender, "&aLatanie zostało &ewyłączone");
            }
            return true;
        }

        // /fly <gracz> - przełącza latanie dla innego gracza
        if (args.length == 1) {
            if (!sender.hasPermission(perm("others"))) {
                sendMessage(sender, "&cNie masz uprawnień do zmiany latania innym graczom!");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sendMessage(sender, "&cGracz &e" + args[0] + " &cnie jest online!");
                return true;
            }

            toggleFly(target);

            if (target.getAllowFlight()) {
                sendMessage(sender, "&aLatanie dla gracza &e" + target.getName() + " &azostało &ewłączone");
                if (!target.equals(sender)) {
                    sendMessage(target, "&aLatanie zostało &ewłączone &aprzez administratora!");
                }
            } else {
                sendMessage(sender, "&aLatanie dla gracza &e" + target.getName() + " &azostało &ewyłączone");
                if (!target.equals(sender)) {
                    sendMessage(target, "&aLatanie zostało &ewyłączone &aprzez administratora!");
                }
            }
            return true;
        }

        sendMessage(sender, "&cUżycie: " + getUsage());
        return true;
    }

    private void toggleFly(Player player) {
        boolean newState = !player.getAllowFlight();
        player.setAllowFlight(newState);

        // Jeśli wyłączamy latanie i gracz leci, delikatnie go opuść
        if (!newState && player.isFlying()) {
            player.setFlying(false);
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender.hasPermission(perm("others"))) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return super.tabComplete(sender, args);
    }
}
