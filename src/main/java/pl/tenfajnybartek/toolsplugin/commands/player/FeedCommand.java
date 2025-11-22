package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
import pl.tenfajnybartek.toolsplugin.managers.CooldownManager;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

import java.util.List;
import java.util.stream.Collectors;

public class FeedCommand extends BaseCommand {

    public FeedCommand() {
        super("feed", "Karmi gracza do pełna", "/feed [gracz]", "tfbhc.cmd.feed", new String[]{"eat"});
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        CooldownManager cooldownManager = ToolsPlugin.getInstance().getCooldownManager();

        // /feed - karmi siebie
        if (args.length == 0) {
            if (!isPlayer(sender)) {
                sendMessage(sender, "&cTa komenda może być użyta tylko przez gracza!");
                sendMessage(sender, "&eUżycie: " + getUsage());
                return true;
            }

            Player player = getPlayer(sender);

            // Sprawdź cooldown
            if (cooldownManager.checkCooldown(player, "feed")) {
                return true; // Cooldown aktywny - blokuj wykonanie
            }

            feedPlayer(player);
            sendMessage(sender, "&aZostałeś nakarmiony!");

            // Ustaw cooldown
            cooldownManager.setCooldown(player, "feed");
            return true;
        }

        // /feed <gracz> - karmi innego gracza
        if (args.length == 1) {
            if (!sender.hasPermission(perm("others"))) {
                sendMessage(sender, "&cNie masz uprawnień do karmienia innych graczy!");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sendMessage(sender, "&cGracz &e" + args[0] + " &cnie jest online!");
                return true;
            }

            feedPlayer(target);
            sendMessage(sender, "&aNakarmiono gracza &e" + target.getName());

            if (!target.equals(sender)) {
                sendMessage(target, "&aZostałeś nakarmiony przez administratora!");
            }

            // Cooldown tylko dla gracza używającego komendy
            if (isPlayer(sender)) {
                cooldownManager.setCooldown(getPlayer(sender), "feed");
            }

            return true;
        }

        sendMessage(sender, "&cUżycie: " + getUsage());
        return true;
    }

    private void feedPlayer(Player player) {
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setExhaustion(0f);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender.hasPermission(perm("others"))) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return super.tabComplete(sender, args);
    }
}