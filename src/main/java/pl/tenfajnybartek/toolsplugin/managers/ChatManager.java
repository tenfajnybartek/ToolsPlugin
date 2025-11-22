package pl.tenfajnybartek.toolsplugin.managers;

import org.bukkit.plugin.java.JavaPlugin;
import pl.tenfajnybartek.toolsplugin.utils.ColorUtils;

public class ChatManager {

    private final JavaPlugin plugin;
    private boolean chatEnabled = true;
    private boolean chatVipOnly = false;
    private final String vipPermission = "tfbhc.chat.vip"; // Permisja dla trybu VIP

    public ChatManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // --- Stan Chatu ---

    public boolean isChatEnabled() {
        return chatEnabled;
    }

    public void setChatEnabled(boolean chatEnabled) {
        this.chatEnabled = chatEnabled;
    }

    public boolean isChatVipOnly() {
        return chatVipOnly;
    }

    public void setChatVipOnly(boolean chatVipOnly) {
        this.chatVipOnly = chatVipOnly;
    }

    public String getVipPermission() {
        return vipPermission;
    }

    // Opcjonalna metoda pomocnicza do wysyłania wiadomości (zakładając ColorUtils w pluginie)
    public void sendMessage(org.bukkit.command.CommandSender sender, String message) {
        // Użyj istniejącej metody sendMessage z BaseCommand lub Głównego Pluginu
        // Na potrzeby przykładu użyję prostej logiki:
        sender.sendMessage(ColorUtils.colorize(message));
    }
}
