package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class GodCommand extends BaseCommand {
    private static final Map<UUID, Boolean> godMode = new HashMap<>();

    public GodCommand() {
        super("god", "Włącza/wyłącza tryb nieśmiertelności", "/god [gracz]", "tfbhc.cmd.god", new String[]{"godmode"});
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // /god - przełącza god mode dla siebie
        if (args.length == 0) {
            if (!isPlayer(sender)) {
                sendMessage(sender, "&cTa komenda może być użyta tylko przez gracza!");
                sendMessage(sender, "&eUżycie: " + getUsage());
                return true;
            }

            Player player = getPlayer(sender);
            toggleGod(player);

            if (isGod(player)) {
                sendMessage(sender, "&aTryb nieśmiertelności został &ewłączony");
            } else {
                sendMessage(sender, "&aTryb nieśmiertelności został &ewyłączony");
            }
            return true;
        }

        // /god <gracz> - przełącza god mode dla innego gracza
        if (args.length == 1) {
            if (!sender.hasPermission(perm("others"))) {
                sendMessage(sender, "&cNie masz uprawnień do zmiany god mode innym graczom!");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sendMessage(sender, "&cGracz &e" + args[0] + " &cnie jest online!");
                return true;
            }

            toggleGod(target);

            if (isGod(target)) {
                sendMessage(sender, "&aTryb nieśmiertelności dla gracza &e" + target.getName() + " &azostał &ewłączony");
                if (!target.equals(sender)) {
                    sendMessage(target, "&aTryb nieśmiertelności został &ewłączony &aprzez administratora!");
                }
            } else {
                sendMessage(sender, "&aTryb nieśmiertelności dla gracza &e" + target.getName() + " &azostał &ewyłączony");
                if (!target.equals(sender)) {
                    sendMessage(target, "&aTryb nieśmiertelności został &ewyłączony &aprzez administratora!");
                }
            }
            return true;
        }

        sendMessage(sender, "&cUżycie: " + getUsage());
        return true;
    }

    private void toggleGod(Player player) {
        UUID uuid = player.getUniqueId();
        boolean current = godMode.getOrDefault(uuid, false);
        godMode.put(uuid, !current);

        // Ustaw invulnerable dla Minecraft API
        player.setInvulnerable(!current);
    }

    public static boolean isGod(Player player) {
        return godMode.getOrDefault(player.getUniqueId(), false);
    }

    public static void removeGod(Player player) {
        godMode.remove(player.getUniqueId());
        player.setInvulnerable(false);
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
