package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
import pl.tenfajnybartek.toolsplugin.managers.HelpopManager;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;
import pl.tenfajnybartek.toolsplugin.utils.ColorUtils;

import java.util.Collections;
import java.util.List;

public class HelpopCommand extends BaseCommand {

    private final HelpopManager helpopManager;

    public HelpopCommand() {
        super(
                "helpop",
                "Wyślij zgłoszenie do administracji lub przełącz HelpOp",
                "/helpop <wiadomość>",
                HelpopManager.PERM_SEND,
                null
        );
        this.helpopManager = ToolsPlugin.getInstance().getHelpopManager();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (helpopManager == null) {
            sender.sendMessage(ColorUtils.colorize("&cHelpOp nie jest gotowy."));
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("on") || sub.equals("off") || sub.equals("reload")) {
            if (!(sender instanceof Player) || ((Player) sender).hasPermission(HelpopManager.PERM_TOGGLE)) {
                if (sub.equals("reload")) {
                    helpopManager.reload();
                    sendMessage(sender, "&aPrzeładowano HelpOp (enabled / cooldown).");
                    return true;
                }
                boolean state = sub.equals("on");
                if (helpopManager.setEnabled(state)) {
                    sendMessage(sender, "&7HelpOp: " + (state ? "&aWŁĄCZONY" : "&cWYŁĄCZONY"));
                } else {
                    sendMessage(sender, "&7HelpOp już był: " + (state ? "&aWŁĄCZONY" : "&cWYŁĄCZONY"));
                }
                return true;
            } else {
                sendNoPermission(sender);
                return true;
            }
        }

        if (!(sender instanceof Player)) {
            sendMessage(sender, "&cKonsola nie może wysyłać HelpOp.");
            return true;
        }

        Player player = (Player) sender;

        if (!helpopManager.isEnabled()) {
            sendMessage(player, "&cHelpOp jest obecnie wyłączony.");
            return true;
        }
        if (!player.hasPermission(HelpopManager.PERM_SEND)) {
            sendNoPermission(sender);
            return true;
        }

        int remaining = helpopManager.getRemainingCooldown(player);
        if (remaining > 0) {
            sendMessage(player, "&cMusisz odczekać &e" + remaining + "s &cprzed kolejną wiadomością.");
            return true;
        }

        String message = String.join(" ", args);
        if (message.length() < 3) {
            sendMessage(player, "&cWiadomość jest zbyt krótka.");
            return true;
        }
        if (message.length() > 256) {
            sendMessage(player, "&cWiadomość jest zbyt długa (max 256 znaków).");
            return true;
        }

        helpopManager.sendHelpop(player, message);
        helpopManager.recordSend(player);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (helpopManager == null) return Collections.emptyList();
        if (args.length == 1 && sender.hasPermission(HelpopManager.PERM_TOGGLE)) {
            return filter(args[0], "on", "off", "reload");
        }
        return Collections.emptyList();
    }

    private List<String> filter(String prefix, String... options) {
        if (prefix == null || prefix.isEmpty()) {
            return java.util.Arrays.asList(options);
        }
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String o : options) {
            if (o.toLowerCase().startsWith(prefix.toLowerCase())) {
                out.add(o);
            }
        }
        return out;
    }
}
