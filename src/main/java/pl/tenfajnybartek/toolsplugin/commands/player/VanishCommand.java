package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
import pl.tenfajnybartek.toolsplugin.managers.VanishManager;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;
import pl.tenfajnybartek.toolsplugin.utils.ColorUtils;

import java.util.Collections;
import java.util.List;

public class VanishCommand extends BaseCommand {

    private final VanishManager vanishManager;

    public VanishCommand() {
        super(
                "vanish",
                "Przełącza tryb ukrycia (niewidoczny dla graczy bez permisji).",
                "/vanish [gracz]",
                VanishManager.PERM_BASE,
                null
        );
        this.vanishManager = ToolsPlugin.getInstance().getVanishManager();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (vanishManager == null) {
            sendMessage(sender, "&cSystem vanish nie jest gotowy.");
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sendMessage(sender, "&cTylko gracz może użyć /vanish bez argumentów.");
                return true;
            }
            Player player = (Player) sender;
            if (!player.hasPermission(VanishManager.PERM_BASE)) {
                sendMessage(player, "&cBrak uprawnień.");
                return true;
            }
            vanishManager.toggleSelf(player);
            return true;
        }

        if (!sender.hasPermission(VanishManager.PERM_ADMIN)) {
            sendMessage(sender, "&cBrak uprawnień (wymagane: " + VanishManager.PERM_ADMIN + ").");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sendMessage(sender, "&cGracz offline lub nie znaleziony.");
            return true;
        }

        vanishManager.setVanish(target, !vanishManager.isVanished(target));
        sendMessage(sender, "&aPrzełączono vanish gracza &e" + target.getName() + "&a. Teraz: " +
                (vanishManager.isVanished(target) ? "&eUKRYTY" : "&cWIDOCZNY"));
        if (!sender.equals(target)) {
            target.sendMessage(ColorUtils.colorize("&8[&cTools&8] &7Twój stan vanish został zmieniony przez &e" + sender.getName()));
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender.hasPermission(VanishManager.PERM_ADMIN)) {
            String prefix = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(prefix))
                    .sorted()
                    .toList();
        }
        return Collections.emptyList();
    }
}
