package pl.tenfajnybartek.toolsplugin.managers;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import pl.tenfajnybartek.toolsplugin.utils.ColorUtils;

import java.util.Map;

public class ChatManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final PermissionManager permissionManager;

    private boolean chatEnabled = true;
    private boolean chatVipOnly = false;
    private final String vipPermission = "tools.chat.vip";

    public ChatManager(JavaPlugin plugin, ConfigManager configManager, PermissionManager permissionManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.permissionManager = permissionManager;
    }


    public boolean isChatEnabled() { return chatEnabled; }
    public void setChatEnabled(boolean chatEnabled) { this.chatEnabled = chatEnabled; }

    public boolean isChatVipOnly() { return chatVipOnly; }
    public void setChatVipOnly(boolean chatVipOnly) { this.chatVipOnly = chatVipOnly; }

    public String getVipPermission() { return vipPermission; }

    public String formatAndSend(Player player, String message) {

        String chatFormat = configManager.getDefaultChatFormat();

        Map<String, String> customFormats = configManager.getCustomChatFormats();

        for (Map.Entry<String, String> entry : customFormats.entrySet()) {
            String requiredPermission = entry.getKey();
            String specificFormat = entry.getValue();

            if (player.hasPermission(requiredPermission)) {
                chatFormat = specificFormat;
                break;
            }
        }

        String prefix = permissionManager.getPlayerPrefix(player);
        String suffix = permissionManager.getPlayerSuffix(player);


        String formattedMessage = chatFormat
                .replace("%player_name%", player.getName())
                .replace("%prefix%", prefix)
                .replace("%suffix%", suffix)
                .replace("%message%", message);

        String finalMessage = ColorUtils.colorize(formattedMessage);

        return finalMessage;
    }

    public void sendMessage(CommandSender sender, String message) {
        String fullMessage = configManager.getPrefix() + message;
        sender.sendMessage(ColorUtils.colorize(fullMessage));
    }
}