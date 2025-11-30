package pl.tenfajnybartek.toolsplugin.utils;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseCommand {
    private final String name;
    private final String description;
    private final String usage;
    private final String permission;
    private final String[] aliases;

    public BaseCommand(String name, String description, String usage, String permission, String[] aliases) {
        this.name = name;
        this.description = description;
        this.usage = usage;
        this.permission = permission;
        this.aliases = aliases != null ? aliases : new String[0];
    }

    protected String perm(String suffix) {
        return permission + "." + suffix;
    }

    public abstract boolean execute(CommandSender sender, String[] args);

    public List<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getUsage() {
        return usage;
    }

    public String getPermission() {
        return permission;
    }

    public String[] getAliases() {
        return aliases;
    }

    protected boolean isPlayer(CommandSender sender) {
        return sender instanceof Player;
    }

    protected Player getPlayer(CommandSender sender) {
        return (Player) sender;
    }

    protected void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(ColorUtils.colorize(message));
    }

    protected void sendMiniMessage(CommandSender sender, String message) {
        Component component = ColorUtils.miniMessage(message);
        sender.sendMessage(component);
    }

    protected void sendComponent(CommandSender sender, Component component) {
        sender.sendMessage(component);
    }
    protected void sendUsage(CommandSender sender) {
        var lm = ToolsPlugin.getInstance().getLanguageManager();
        sender.sendMessage(lm.formatUsage(getUsage()));
    }
    protected void sendNoPermission(CommandSender sender) {
        var lm = pl.tenfajnybartek.toolsplugin.base.ToolsPlugin.getInstance().getLanguageManager();
        sender.sendMessage(lm.getNoPermission(getPermission() != null ? getPermission() : "brak"));
    }

    protected void sendOnlyPlayer(CommandSender sender) {
        var lm = pl.tenfajnybartek.toolsplugin.base.ToolsPlugin.getInstance().getLanguageManager();
        sender.sendMessage(lm.getOnlyPlayer());
    }

    protected void sendPlayerOffline(CommandSender sender, String playerName) {
        var lm = pl.tenfajnybartek.toolsplugin.base.ToolsPlugin.getInstance().getLanguageManager();
        sender.sendMessage(lm.getPlayerOffline(playerName));
    }
}
