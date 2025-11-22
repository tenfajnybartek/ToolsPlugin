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
        super("heal", "Leczy gracza do pełnego HP i głodu", "/heal [gracz]", "tfbhc.cmd.heal", new String[]{"h"});
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        CooldownManager cooldownManager = ToolsPlugin.getInstance().getCooldownManager();

        // /heal - leczy siebie
        if (args.length == 0) {
            if (!isPlayer(sender)) {
                sendMessage(sender, "&cTa komenda może być użyta tylko przez gracza!");
                sendMessage(sender, "&eUżycie: " + getUsage());
                return true;
            }

            Player player = getPlayer(sender);

            // Sprawdź cooldown
            if (cooldownManager.checkCooldown(player, "heal")) {
                return true; // Cooldown aktywny - blokuj wykonanie
            }

            healPlayer(player);
            sendMessage(sender, "&aZostałeś uleczony!");

            // Ustaw cooldown
            cooldownManager.setCooldown(player, "heal");
            return true;
        }

        // /heal <gracz> - leczy innego gracza
        if (args.length == 1) {
            if (!sender.hasPermission(perm("others"))) {
                sendMessage(sender, "&cNie masz uprawnień do leczenia innych graczy!");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sendMessage(sender, "&cGracz &e" + args[0] + " &cnie jest online!");
                return true;
            }

            healPlayer(target);
            sendMessage(sender, "&aUleczono gracza &e" + target.getName());

            if (!target.equals(sender)) {
                sendMessage(target, "&aZostałeś uleczony przez administratora!");
            }

            // Cooldown tylko dla gracza używającego komendy
            if (isPlayer(sender)) {
                cooldownManager.setCooldown(getPlayer(sender), "heal");
            }

            return true;
        }

        sendMessage(sender, "&cUżycie: " + getUsage());
        return true;
    }

    private void healPlayer(Player player) {
        AttributeInstance maxHealthAttribute = player.getAttribute(Attribute.MAX_HEALTH);

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
