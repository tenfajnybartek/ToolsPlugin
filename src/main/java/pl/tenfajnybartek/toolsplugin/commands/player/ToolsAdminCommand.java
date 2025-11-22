package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
import pl.tenfajnybartek.toolsplugin.managers.ConfigManager;
import pl.tenfajnybartek.toolsplugin.managers.CooldownManager;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ToolsAdminCommand extends BaseCommand {

    public ToolsAdminCommand() {
        super("toolsadmin", "Zarządza pluginem ToolsPlugin", "/toolsadmin <reload/cooldown>", "tfbhc.cmd.admin", new String[]{"tpa", "toolsa"});
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                return handleReload(sender);

            case "cooldown":
                return handleCooldown(sender, args);

            case "info":
                return handleInfo(sender);

            default:
                sendMessage(sender, "&cNieznana podkomenda! Użyj &e/toolsadmin &cdla pomocy.");
                return true;
        }
    }

    /**
     * Przeładowuje config pluginu
     */
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission(perm("reload"))) {
            sendMessage(sender, "&cNie masz uprawnień do przeładowania configu!");
            return true;
        }

        try {
            ConfigManager configManager = ToolsPlugin.getInstance().getConfigManager();
            configManager.reloadConfig();

            sendMessage(sender, "&aConfig pluginu został pomyślnie przeładowany!");
            return true;
        } catch (Exception e) {
            sendMessage(sender, "&cBłąd podczas przeładowywania configu!");
            e.printStackTrace();
            return true;
        }
    }

    /**
     * Zarządza cooldownami graczy
     */
    private boolean handleCooldown(CommandSender sender, String[] args) {
        if (!sender.hasPermission(perm("cooldown"))) {
            sendMessage(sender, "&cNie masz uprawnień do zarządzania cooldownami!");
            return true;
        }

        if (args.length < 3) {
            sendMessage(sender, "&cUżycie: /toolsadmin cooldown <clear/check> <gracz> [komenda]");
            return true;
        }

        String action = args[1].toLowerCase();
        Player target = Bukkit.getPlayer(args[2]);

        if (target == null) {
            sendMessage(sender, "&cGracz &e" + args[2] + " &cnie jest online!");
            return true;
        }

        CooldownManager cooldownManager = ToolsPlugin.getInstance().getCooldownManager();

        if (action.equals("clear")) {
            if (args.length == 3) {
                // Wyczyść wszystkie cooldowny gracza
                cooldownManager.clearPlayerCooldowns(target);
                sendMessage(sender, "&aWyczyszczono wszystkie cooldowny gracza &e" + target.getName());
            } else {
                // Wyczyść konkretny cooldown
                String command = args[3];
                cooldownManager.removeCooldown(target, command);
                sendMessage(sender, "&aWyczyszczono cooldown komendy &e" + command + " &adla gracza &e" + target.getName());
            }
            return true;
        }

        if (action.equals("check")) {
            if (args.length < 4) {
                sendMessage(sender, "&cUżycie: /toolsadmin cooldown check <gracz> <komenda>");
                return true;
            }

            String command = args[3];

            if (cooldownManager.hasCooldown(target, command)) {
                int remaining = cooldownManager.getRemainingCooldown(target, command);
                sendMessage(sender, "&eGracz &6" + target.getName() + " &ema cooldown na komendzie &6" + command + "&e: &c" + remaining + "s");
            } else {
                sendMessage(sender, "&eGracz &6" + target.getName() + " &enie ma cooldownu na komendzie &6" + command);
            }
            return true;
        }

        sendMessage(sender, "&cUżycie: /toolsadmin cooldown <clear/check> <gracz> [komenda]");
        return true;
    }

    /**
     * Wyświetla informacje o pluginie
     */
    private boolean handleInfo(CommandSender sender) {
        ConfigManager config = ToolsPlugin.getInstance().getConfigManager();

        sendMessage(sender, "&8--- &6&lToolsPlugin Info &8---");
        sendMessage(sender, "&eWersja: &f1.0.0");
        sendMessage(sender, "&eAutor: &fTenFajnyBartek");
        sendMessage(sender, "&eCooldowny: " + (config.isCooldownsEnabled() ? "&aWłączone" : "&cWyłączone"));
        sendMessage(sender, "&eZarejestrowanych komend: &f" + ToolsPlugin.getInstance().getCommandManager().getCommands().size());
        sendMessage(sender, "&8--------------------------");

        return true;
    }

    /**
     * Wyświetla pomoc
     */
    private void sendHelp(CommandSender sender) {
        sendMessage(sender, "&8--- &6&lToolsPlugin Admin &8---");
        sendMessage(sender, "&e/toolsadmin reload &7- Przeładowuje config");
        sendMessage(sender, "&e/toolsadmin cooldown clear <gracz> &7- Czyści cooldowny");
        sendMessage(sender, "&e/toolsadmin cooldown check <gracz> <komenda> &7- Sprawdza cooldown");
        sendMessage(sender, "&e/toolsadmin info &7- Informacje o pluginie");
        sendMessage(sender, "&8--------------------------");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("reload", "cooldown", "info").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("cooldown")) {
            return Arrays.asList("clear", "check").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("cooldown")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("cooldown") && args[1].equalsIgnoreCase("check")) {
            return Arrays.asList("heal", "feed", "tp", "repair", "enchant").stream()
                    .filter(s -> s.startsWith(args[3].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return super.tabComplete(sender, args);
    }
}
