package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
import pl.tenfajnybartek.toolsplugin.managers.MuteManager;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;
import pl.tenfajnybartek.toolsplugin.utils.TimeUtils;

import java.util.List;
import java.util.stream.Collectors;

import static pl.tenfajnybartek.toolsplugin.utils.ColorUtils.toComponent;

public class MuteCommand extends BaseCommand {

    public MuteCommand() {
        super("mute", "Wycisza gracza", "/mute <nick> [czas] [powód]", "tools.mute", null);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!isPlayer(sender)) {
            sendMessage(sender, "&cTylko gracze mogą wyciszać.");
            return true;
        }

        if (args.length < 1) {
            sendMessage(sender, getUsage());
            return true;
        }

        final Player muter = getPlayer(sender);
        final OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        final MuteManager mm = ToolsPlugin.getInstance().getMuteManager();

        // Logika parsowania czasu i powodu (identyczna jak w BanCommand)
        String timeString;
        String reason;

        if (args.length >= 2) {
            if (TimeUtils.parseTime(args[1]) != null || args[1].equalsIgnoreCase("perm") || args[1].equalsIgnoreCase("permanent")) {
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

        // Wykonanie wyciszenia asynchronicznie
        mm.mutePlayer(target, muter, finalTimeString, finalReason)
                .thenAccept(record -> {
                    if (record != null) {

                        // Przełączenie na Główny Wątek do operacji Bukkit API
                        ToolsPlugin.getInstance().getServer().getScheduler().runTask(ToolsPlugin.getInstance(), () -> {

                            // Powiadomienie gracza (bez kicka - to jest mute)
                            if (target.isOnline() && target.getPlayer() != null) {
                                target.getPlayer().sendMessage(record.getMuteMessage());
                                sendMessage(sender, "&aWyciszyłeś gracza &e" + target.getName() + " &ana: &e" + (record.isPermanent() ? "NA ZAWSZE" : TimeUtils.formatDuration(record.getExpireTime())));
                            }

                            // Broadcast do serwera
                            String timeInfo = record.isPermanent() ? "&cNA ZAWSZE" : "&e" + TimeUtils.formatDuration(record.getExpireTime());
                            String broadcastRaw = "&4[MUTE]&c Gracz &e" + target.getName() + " &czostał wyciszony przez &e" + muter.getName() + " &cna: " + timeInfo + "&c. Powód: &7" + record.getReason();

                            Bukkit.getServer().sendMessage(toComponent(broadcastRaw));
                        });

                    } else {
                        sendMessage(sender, "&cBłąd podczas wyciszania gracza. Sprawdź logi serwera.");
                    }
                });

        return true;
    }
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // Podpowiadanie graczy
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return super.tabComplete(sender, args);
    }
}
