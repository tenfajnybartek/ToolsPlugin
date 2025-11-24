package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
import pl.tenfajnybartek.toolsplugin.managers.BanManager;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

import java.util.List;
import java.util.stream.Collectors;

import static pl.tenfajnybartek.toolsplugin.utils.ColorUtils.toComponent;

public class UnBanCommand extends BaseCommand {

    public UnBanCommand() {
        super("unban", "Odbanowuje gracza", "/unban <nick>", "tools.cmd.unban", null);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sendUsage(sender);
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        BanManager bm = ToolsPlugin.getInstance().getBanManager();

        bm.unbanPlayer(target.getUniqueId())
                .thenAccept(success -> {
                    if (success) {
                        String broadcastRaw = "&2[UNBAN]&a Gracz &e" + target.getName() + " &azostał odbanowany przez &e" + sender.getName() + "&a.";

                        Bukkit.getServer().sendMessage(toComponent(broadcastRaw));
                    } else {
                        sendMessage(sender, "&cGracz &e" + target.getName() + " &cnie ma aktywnego bana.");
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
