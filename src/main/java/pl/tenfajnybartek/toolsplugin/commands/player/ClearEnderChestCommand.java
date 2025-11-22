package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ClearEnderChestCommand extends BaseCommand {

    public ClearEnderChestCommand() {
        super("clearenderchest", "Czyści ender chest gracza", "/clearenderchest <gracz>", "tfbhc.cmd.clearec", new String[]{"clearec", "cechest"});
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 1) {

            if (!sender.hasPermission(perm("others"))) {
                sendMessage(sender, "&cNie masz uprawnień do czyszczenia ender chestów innym graczom!");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sendMessage(sender, "&cGracz &e" + args[0] + " &cnie jest online!");
                return true;
            }

            // Wywołanie zmienionej metody czyszczącej
            clearEnderChest(target);

            sendMessage(sender, "&aWyczyszczono ender chest gracza &e" + target.getName() + "&a!");

            if (!target.equals(sender)) {
                sendMessage(target, "&cTwój ender chest został wyczyszczony przez administratora!");
            }
            return true;
        }

        // Komenda wymaga argumentu
        sendMessage(sender, "&cUżycie: " + getUsage());
        return true;
    }

    private void clearEnderChest(Player player) {
        player.getEnderChest().clear();
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