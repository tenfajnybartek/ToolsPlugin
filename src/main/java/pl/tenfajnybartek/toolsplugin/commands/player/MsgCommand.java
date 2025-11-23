package pl.tenfajnybartek.toolsplugin.commands.player;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
import pl.tenfajnybartek.toolsplugin.managers.MessageManager;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;
import pl.tenfajnybartek.toolsplugin.utils.ColorUtils;

import java.util.List;
import java.util.stream.Collectors;

public class MsgCommand extends BaseCommand {

    public MsgCommand() {
        // Użycie: /msg <gracz> <wiadomość>
        super("msg", "Wysyła prywatną wiadomość", "/msg <gracz> <wiadomość>", "tools.msg", null);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!isPlayer(sender)) {
            sendMessage(sender, "&cTylko gracze mogą używać tej komendy.");
            return true;
        }

        if (args.length < 2) {
            // Wyświetlenie prawidłowego użycia komendy
            sendMessage(sender, "&cUżycie: " + getUsage());
            return true;
        }

        Player player = getPlayer(sender);

        // 1. Pobranie gracza-odbiorcy
        Player target = Bukkit.getPlayer(args[0]);

        if (target == null || !target.isOnline()) {
            sendMessage(player, "&cGracz " + args[0] + " jest offline lub nie istnieje.");
            return true;
        }

        if (target.equals(player)) {
            sendMessage(player, "&cNie możesz wysłać wiadomości sam do siebie.");
            return true;
        }

        // 2. Budowanie wiadomości
        StringBuilder messageBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            messageBuilder.append(args[i]).append(" ");
        }
        String message = messageBuilder.toString().trim();

        // 3. Wysyłanie przez MessageManager
        MessageManager messageManager = ToolsPlugin.getInstance().getMessageManager();
        messageManager.sendPrivateMessage(player, target, message);

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
