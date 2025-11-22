package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;
import pl.tenfajnybartek.toolsplugin.utils.ColorUtils;

import java.util.ArrayList;

public class KickAllCommand extends BaseCommand {

    public KickAllCommand() {
        super("kickall", "Wyrzuca wszystkich graczy z serwera", "/kickall [powód]", "tfbhc.cmd.kickall", new String[]{"ka"});
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {

        String reason = "Zostałeś wyrzucony z serwera. Spróbuj dołączyć ponownie!";
        if (args.length >= 1) {
            // Używamy Arrays.stream, aby obsłużyć wiele argumentów jako powód
            reason = String.join(" ", args);
        }

        // Dodanie informacji o adminie do powodu
        String adminName = (sender instanceof Player) ? sender.getName() : "Konsola";
        String finalReason = ColorUtils.colorize("&cWyrzucenie przez &4" + adminName + ":&r\n\n" + reason);

        int kickedCount = 0;

        // Zapisujemy listę graczy, aby uniknąć błędów modyfikacji kolekcji
        ArrayList<Player> playersToKick = new ArrayList<>(Bukkit.getOnlinePlayers());

        for (Player target : playersToKick) {
            // 1. Warunek: Nie wyrzucamy admina wykonującego komendę
            if (target.equals(sender)) {
                sendMessage(sender, "&7Administrator &e" + sender.getName() + " &7został pominięty (wykonawca komendy).");
                continue;
            }

            // 2. Warunek: Nie wyrzucamy graczy z uprawnieniem bypass (tfbhc.cmd.kickall.bypass)
            if (target.hasPermission(perm("bypass"))) {
                sendMessage(sender, "&7Gracz &e" + target.getName() + " &7został pominięty (bypass).");
                continue;
            }

            // Wyrzucenie gracza
            target.kickPlayer(finalReason);
            kickedCount++;
        }

        sendMessage(sender, "&aWyrzucono &e" + kickedCount + " &agraczy z serwera z powodu: &f" + reason);

        return true;
    }

}
