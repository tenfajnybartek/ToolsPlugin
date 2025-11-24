package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

import java.util.List;
import java.util.stream.Collectors;

public class ClearInventoryCommand extends BaseCommand {

    public ClearInventoryCommand() {
        super("clearinventory", "Czyści ekwipunek gracza", "/clearinventory [gracz]", "tools.cmd.clearinv", new String[]{"clear", "ci", "clearinv"});
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!isPlayer(sender)) {
                sendOnlyPlayer(sender);
                sendUsage(sender);
                return true;
            }

            Player player = getPlayer(sender);
            clearInventory(player);
            sendMessage(sender, "&aTwój ekwipunek został wyczyszczony!");
            return true;
        }

        if (args.length == 1) {
            if (!sender.hasPermission(perm("others"))) {
                sendNoPermission(sender);
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sendPlayerOffline(sender, args[0]);
                return true;
            }

            clearInventory(target);
            sendMessage(sender, "&aWyczyszczono ekwipunek gracza &e" + target.getName());

            if (!target.equals(sender)) {
                sendMessage(target, "&aTwój ekwipunek został wyczyszczony przez administratora!");
            }
            return true;
        }

        sendUsage(sender);
        return true;
    }

    private void clearInventory(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItemInOffHand(null);
        player.updateInventory();
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
