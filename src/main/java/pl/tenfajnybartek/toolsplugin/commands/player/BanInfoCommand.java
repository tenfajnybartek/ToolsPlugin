package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
import pl.tenfajnybartek.toolsplugin.managers.BanManager;
import pl.tenfajnybartek.toolsplugin.objects.BanRecord;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;
import pl.tenfajnybartek.toolsplugin.utils.TimeUtils;

import java.util.List;
import java.util.stream.Collectors;


public class BanInfoCommand extends BaseCommand {

    public BanInfoCommand() {
        super("baninfo", "Wyświetla historię i aktualny ban gracza", "/baninfo <nick>", "tools.cmd.baninfo", null);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sendUsage(sender);
            return true;
        }

        final OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        final BanManager bm = ToolsPlugin.getInstance().getBanManager();

        bm.getAllBans(target.getUniqueId())
                .thenAccept(records -> {

                    if (records.isEmpty()) {
                        sendMessage(sender, "&aGracz &e" + target.getName() + " &anie ma historii banów.");
                        return;
                    }

                    sendMessage(sender, "&7--- &6Historia Banów dla &e" + target.getName() + " &7---");

                    for (int i = 0; i < records.size(); i++) {
                        BanRecord record = records.get(i);
                        String status = record.isActive() && !record.hasExpired() ? "&4AKTYWNY" : "&aNIEAKTYWNY";
                        String time = record.isPermanent() ? "&cPERM" : TimeUtils.formatDateTime(record.getExpireTime());

                        sendMessage(sender, "&7#&f" + (i + 1) + ". Status: " + status);
                        sendMessage(sender, "  &7| Data: &f" + TimeUtils.formatDateTime(record.getBanTime()));
                        sendMessage(sender, "  &7| Wygasa: " + time);
                        sendMessage(sender, "  &7| Banner: &f" + record.getBannerName());
                        sendMessage(sender, "  &7| Powód: &f" + record.getReason());
                    }
                });

        return true;
    }
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return super.tabComplete(sender, args);
    }
}
