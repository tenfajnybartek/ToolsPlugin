package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.command.CommandSender;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
import pl.tenfajnybartek.toolsplugin.managers.ConfigManager;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ToolsAdminCommand extends BaseCommand {

    public ToolsAdminCommand() {
        super("toolsadmin", "Zarządza pluginem ToolsPlugin", "/toolsadmin <reload|info>", "tools.cmd.admin", new String[]{"tpa", "toolsa"});
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

            case "info":
                return handleInfo(sender);

            default:
                sendUsage(sender);
                return true;
        }
    }

    /**
     * Przeładowuje config pluginu
     */
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission(perm("reload"))) {
            sendNoPermission(sender);
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

    private boolean handleInfo(CommandSender sender) {
        ConfigManager config = ToolsPlugin.getInstance().getConfigManager();

        sendMessage(sender, "&8--- &6&lToolsPlugin Info &8---");
        sendMessage(sender, "&eWersja: &f1.0.0");
        sendMessage(sender, "&eAutor: &ftenfajnybartek");
        // Linia o cooldownach została usunięta
        sendMessage(sender, "&eZarejestrowanych komend: &f" + ToolsPlugin.getInstance().getCommandManager().getCommands().size());
        sendMessage(sender, "&8--------------------------");

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sendMessage(sender, "&8--- &6&lToolsPlugin Admin &8---");
        sendMessage(sender, "&e/toolsadmin reload &7- Przeładowuje config");
        sendMessage(sender, "&e/toolsadmin info &7- Informacje o pluginie");
        sendMessage(sender, "&8--------------------------");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("reload", "info").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return super.tabComplete(sender, args);
    }
}