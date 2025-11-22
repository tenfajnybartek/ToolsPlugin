package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class WhoisCommand extends BaseCommand {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public WhoisCommand() {
        super("whois", "Wyświetla szczegółowe informacje o graczu", "/whois <gracz>", "tfbhc.cmd.whois", new String[]{"info"});
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {

        if (args.length != 1) {
            sendMessage(sender, "&cUżycie: " + getUsage());
            return true;
        }

        String targetName = args[0];

        // Używamy getOfflinePlayer, aby pobrać dane nawet dla graczy offline
        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        // Sprawdzamy, czy gracz jest znany serwerowi/Mojangowi (ma UUID)
        if (target.getUniqueId() == null) {
            sendMessage(sender, String.format("&cNie znaleziono gracza &e%s&c.", targetName));
            return true;
        }

        // ------------------ ZBIERANIE DANYCH ------------------

        // Status
        String status = target.isOnline() ? "&aOnline" : "&cOffline";

        // Adres IP (tylko dla graczy online!)
        String ipAddress = "Niedostępny";
        if (target.isOnline()) {
            Player onlineTarget = target.getPlayer();
            // Sprawdzamy, czy połączenie jest dostępne
            if (onlineTarget != null && onlineTarget.getAddress() != null) {
                ipAddress = onlineTarget.getAddress().getHostString();
            }
        }

        // Data pierwszej wizyty
        String firstPlayed = target.getFirstPlayed() > 0 ?
                DATE_FORMAT.format(new Date(target.getFirstPlayed())) :
                "Brak danych";

        // Data ostatniej wizyty
        String lastPlayed = target.getLastPlayed() > 0 ?
                DATE_FORMAT.format(new Date(target.getLastPlayed())) :
                "Brak danych";

        // ------------------ WYSYŁANIE RAPORTU ------------------

        sendMessage(sender, "&8--- &6&lWHOIS: &e" + targetName + " &8---");
        sendMessage(sender, String.format("&aStatus: %s", status));
        sendMessage(sender, String.format("&aUUID: &f%s", target.getUniqueId().toString()));
        sendMessage(sender, String.format("&aAdres IP: &f%s", ipAddress));
        sendMessage(sender, String.format("&aPierwsza wizyta: &f%s", firstPlayed));
        sendMessage(sender, String.format("&aOstatnia wizyta: &f%s", lastPlayed));
        sendMessage(sender, String.format("&aOperator: &f%s", target.isOp() ? "&aTAK" : "&cNIE"));
        sendMessage(sender, "&8--------------------------------------");

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // Podpowiadanie graczy online
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return super.tabComplete(sender, args);
    }
}
