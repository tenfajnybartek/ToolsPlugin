package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.CommandSender;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
import pl.tenfajnybartek.toolsplugin.managers.CooldownManager;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

import java.util.List;
import java.util.stream.Collectors;

public class HealCommand extends BaseCommand {

    public HealCommand() {
        super("heal", "Leczy gracza do pełnego HP i głodu", "/heal [gracz]", "tools.cmd.heal", new String[]{"h"});
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        CooldownManager cooldownManager = ToolsPlugin.getInstance().getCooldownManager();

        if (args.length == 0) {
            if (!isPlayer(sender)) {
                sendOnlyPlayer(sender);
                sendUsage(sender);
                return true;
            }

            Player player = getPlayer(sender);

            healPlayer(player);
            sendMessage(sender, "&aZostałeś uleczony!");
            return true;
        }

        if (args.length == 1) {
            if (!sender.hasPermission(perm("others"))) {
                sendNoPermission(sender);
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sendPlayerOffline(sender, args[0]);
                return true;
            }

            healPlayer(target);
            sendMessage(sender, "&aUleczono gracza &e" + target.getName());

            if (!target.equals(sender)) {
                sendMessage(target, "&aZostałeś uleczony przez administratora!");
            }

            return true;
        }

        sendUsage(sender);
        return true;
    }

    private void healPlayer(Player player) {
        AttributeInstance maxHealthAttribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);

        if (maxHealthAttribute != null) {
            player.setHealth(maxHealthAttribute.getValue());
        } else {
            player.setHealth(20.0);
        }
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setFireTicks(0);

        for (PotionEffect effect : player.getActivePotionEffects()) {
            String key = effect.getType().getKey().asString();

            if (effect.getType().isInstant() ||
                    key.equalsIgnoreCase("minecraft:poison") ||
                    key.equalsIgnoreCase("minecraft:wither")) {

                player.removePotionEffect(effect.getType());
            }
        }
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
