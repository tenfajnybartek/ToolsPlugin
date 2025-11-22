package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

import java.util.stream.Collectors;

public class ListCommand extends BaseCommand {

    public ListCommand() {
        super("list", "Wyświetla listę graczy online", "/list", "tfbhc.cmd.list", new String[]{"online"});
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {

        if (args.length != 0) {
            sendMessage(sender, "&cUżycie: " + getUsage());
            return true;
        }

        int onlineCount = Bukkit.getOnlinePlayers().size();
        int maxPlayers = Bukkit.getMaxPlayers();

        // Pobranie nazw graczy i sformatowanie ich
        String playerList = Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.joining("&7, &f"));

        // Wysyłanie sformatowanej wiadomości
        sendMessage(sender, "&8--- &eGracze Online &8---");
        sendMessage(sender, String.format("&aLiczba graczy: &f%d&7/&f%d", onlineCount, maxPlayers));

        if (onlineCount > 0) {
            // Używamy &f dla nicków, aby były białe
            sendMessage(sender, "&f" + playerList);
        } else {
            sendMessage(sender, "&7Brak graczy online.");
        }
        sendMessage(sender, "&8-----------------------");

        return true;
    }
}
