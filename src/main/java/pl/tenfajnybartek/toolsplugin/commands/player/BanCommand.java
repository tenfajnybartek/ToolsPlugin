package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
import pl.tenfajnybartek.toolsplugin.managers.BanManager;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;
import pl.tenfajnybartek.toolsplugin.utils.ColorUtils;
import pl.tenfajnybartek.toolsplugin.utils.TimeUtils;

import java.util.List;
import java.util.stream.Collectors;

import static pl.tenfajnybartek.toolsplugin.utils.ColorUtils.toComponent;

public class BanCommand extends BaseCommand {

    public BanCommand() {
        super("ban", "Banuje gracza", "/ban <nick> [czas] [powód]", "tools.cmd.ban", null);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!isPlayer(sender)) {
            sendOnlyPlayer(sender);
            return true;
        }

        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }

        final Player banner = getPlayer(sender);
        final OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        final BanManager bm = ToolsPlugin.getInstance().getBanManager();

        String timeString;
        String reason;

        if (args.length >= 2) {
            if (bm.parseTime(args[1]) != null || args[1].equalsIgnoreCase("perm") || args[1].equalsIgnoreCase("permanent")) {
                timeString = args[1];
                reason = args.length >= 3
                        ? String.join(" ", args).substring(args[0].length() + args[1].length() + 2)
                        : "Brak podanego powodu";
            } else {
                timeString = "perm";
                reason = String.join(" ", args).substring(args[0].length() + 1);
            }
        } else {
            timeString = "perm";
            reason = "Brak podanego powodu";
        }

        final String finalTimeString = timeString;
        final String finalReason = reason;

        bm.banPlayer(target, banner, finalTimeString, finalReason)
                .thenAccept(record -> {
                    if (record != null) {

                        ToolsPlugin.getInstance().getServer().getScheduler().runTask(ToolsPlugin.getInstance(), () -> {

                            if (target.isOnline() && target.getPlayer() != null) {

                                target.getPlayer().kick(TimeUtils.getBanMessage(record));
                            }

                            String timeInfo = record.isPermanent() ? "&cNA ZAWSZE" : "&e" + TimeUtils.formatDuration(record.getExpireTime());

                            String broadcastRaw = "&4[BAN]&c Gracz &e" + target.getName() + " &czostał zbanowany przez &e" + banner.getName() + " &cna: " + timeInfo + "&c. Powód: &7" + record.getReason();

                            Bukkit.getServer().sendMessage(toComponent(broadcastRaw));
                        });

                    } else {
                        sendMessage(sender, "&cBłąd podczas banowania gracza. Sprawdź logi serwera.");
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