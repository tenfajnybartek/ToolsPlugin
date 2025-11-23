package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
import pl.tenfajnybartek.toolsplugin.managers.MuteManager;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;
import pl.tenfajnybartek.toolsplugin.utils.ColorUtils;

import java.util.List;
import java.util.stream.Collectors;

public class UnMuteCommand extends BaseCommand {

    public UnMuteCommand() {
        super("unmute", "Odcisza gracza", "/unmute <nick>", "tools.unmute", null);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sendMessage(sender, getUsage());
            return true;
        }

        final OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        final MuteManager mm = ToolsPlugin.getInstance().getMuteManager();

        mm.unmutePlayer(target.getUniqueId())
                .thenAccept(wasMuted -> {
                    // Przełączenie na Główny Wątek do operacji Bukkit API
                    ToolsPlugin.getInstance().getServer().getScheduler().runTask(ToolsPlugin.getInstance(), () -> {
                        if (wasMuted) {
                            sendMessage(sender, "&aPomyślnie odciszono gracza &e" + target.getName() + "&a.");

                            // Powiadomienie gracza, jeśli jest online
                            if (target.isOnline() && target.getPlayer() != null) {
                                target.getPlayer().sendMessage(ColorUtils.toComponent("&aZostałeś odciszony!"));
                            }
                        } else {
                            // Ten komunikat obsłuży teraz także przypadek, gdy gracz nigdy nie grał
                            sendMessage(sender, "&cGracz &e" + target.getName() + " &cnie miał żadnego aktywnego wyciszenia.");
                        }
                    });
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