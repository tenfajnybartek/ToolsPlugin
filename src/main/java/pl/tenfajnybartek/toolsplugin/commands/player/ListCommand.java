package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ListCommand extends BaseCommand {

    public ListCommand() {
        super("list", "Wyświetla listę graczy online", "/list", "tfbhc.cmd.list", new String[]{"online"});
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // ... (sprawdzenia i onlineCount/maxPlayers bez zmian) ...

        // Mapa do grupowania: Ranga -> Lista Nicków
        Map<String, List<String>> groups = new LinkedHashMap<>();
        groups.put("&cADMINI", new ArrayList<>()); // Używamy LinkedHashMap, by zachować kolejność
        groups.put("&6VIP", new ArrayList<>());
        groups.put("&7GRACZE", new ArrayList<>());

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("tfbhc.list.admin")) {
                groups.get("&cADMINI").add(p.getName());
            } else if (p.hasPermission("tfbhc.list.vip")) {
                groups.get("&6VIP").add(p.getName());
            } else {
                groups.get("&7GRACZE").add(p.getName());
            }
        }
        int onlineCount = Bukkit.getOnlinePlayers().size();
        int maxPlayers = Bukkit.getMaxPlayers();
        sendMessage(sender, "&8--- &eGracze Online &8---");
        sendMessage(sender, String.format("&aLiczba graczy: &f%d&7/&f%d", onlineCount, maxPlayers));

        // Wypisywanie grup
        for (Map.Entry<String, List<String>> entry : groups.entrySet()) {
            String groupName = entry.getKey();
            List<String> names = entry.getValue();

            if (!names.isEmpty()) {
                String playerList = names.stream()
                        .collect(Collectors.joining("&7, &f"));
                // Wysyłamy: [Ranga (Liczba)]: Nick1, Nick2
                sendMessage(sender, String.format("%s &8(&f%d&8): &f%s", groupName, names.size(), playerList));
            }
        }
        sendMessage(sender, "&8-----------------------");

        return true;
    }
}