package pl.tenfajnybartek.toolsplugin.managers;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import pl.tenfajnybartek.toolsplugin.utils.ColorUtils;

import java.util.Map;

public class ChatManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;      // 🚨 DODANA ZALEŻNOŚĆ
    private final PermissionManager permissionManager; // 🚨 DODANA ZALEŻNOŚĆ

    private boolean chatEnabled = true;
    private boolean chatVipOnly = false;
    private final String vipPermission = "tfbhc.chat.vip";

    // 🚨 KOREKTA: Konstruktor musi przyjmować zależności
    public ChatManager(JavaPlugin plugin, ConfigManager configManager, PermissionManager permissionManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.permissionManager = permissionManager;
    }

    // ==================== ZARZĄDZANIE STANEM ====================

    public boolean isChatEnabled() { return chatEnabled; }
    public void setChatEnabled(boolean chatEnabled) { this.chatEnabled = chatEnabled; }

    public boolean isChatVipOnly() { return chatVipOnly; }
    public void setChatVipOnly(boolean chatVipOnly) { this.chatVipOnly = chatVipOnly; }

    public String getVipPermission() { return vipPermission; }

    // ==================== LOGIKA FORMATOWANIA (NOWA) ====================

    /**
     * Główna metoda formatująca wiadomość gracza z prefixami, suffixami i kolorami.
     * Zwraca sformatowany String gotowy do rozesłania przez Bukkit.broadcastMessage.
     */
    public String formatAndSend(Player player, String message) {
        // 1. Logika kolorowania wiadomości (pozostaje bez zmian)
        // ...

        // 2. Wyszukanie odpowiedniego formatu
        String chatFormat = configManager.getDefaultChatFormat(); // Zaczynamy od domyślnego

        Map<String, String> customFormats = configManager.getCustomChatFormats();

        // Iterujemy po wszystkich niestandardowych formatach (możesz chcieć sortować, jeśli priorytety są ważne)
        // Na razie przyjmujemy, że pierwszy znaleziony format z permisją wygrywa (lub po prostu zostawiamy Bukkit/LuckPerms,
        // aby obsłużył priorytety uprawnień)
        for (Map.Entry<String, String> entry : customFormats.entrySet()) {
            String requiredPermission = entry.getKey();
            String specificFormat = entry.getValue();

            if (player.hasPermission(requiredPermission)) {
                // Znaleziono pasujący format. Używamy go.
                chatFormat = specificFormat;
                break; // Przerywamy po znalezieniu pierwszego (lub z najwyższym priorytetem)
            }
        }

        // 3. Pobierz prefix i suffix (z LuckPerms/PermissionManager) - pozostaje bez zmian
        String prefix = permissionManager.getPlayerPrefix(player);
        String suffix = permissionManager.getPlayerSuffix(player);

        // 4. Zastępowanie placeholderów - używamy wybranego chatFormat
        String formattedMessage = chatFormat
                .replace("%player_name%", player.getName())
                .replace("%prefix%", prefix)
                .replace("%suffix%", suffix)
                .replace("%message%", message);

        // 5. Finalne kolorowanie (pozostaje bez zmian)
        String finalMessage = ColorUtils.colorize(formattedMessage);

        return finalMessage;
    }

    public void sendMessage(CommandSender sender, String message) {
        // Używamy ConfigManager, aby uzyskać prefix pluginu (np. [&bTools&f])
        String fullMessage = configManager.getPrefix() + message;
        // Używamy ColorUtils do kolorowania całego komunikatu
        sender.sendMessage(ColorUtils.colorize(fullMessage));
    }
}