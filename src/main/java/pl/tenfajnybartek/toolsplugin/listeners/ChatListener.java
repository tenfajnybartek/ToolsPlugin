package pl.tenfajnybartek.toolsplugin.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import pl.tenfajnybartek.toolsplugin.managers.ChatManager;

public class ChatListener implements Listener {

    private final ChatManager chatManager;

    // POPRAWKA: Konstruktor musi przyjmować ChatManager jako argument
    public ChatListener(ChatManager chatManager) {
        this.chatManager = chatManager; // Poprawne przypisanie
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        // Zawsze pozwalamy administracji pisać (z permisją bypass)
        if (player.hasPermission("tfbhc.chat.bypass")) {
            return;
        }

        // 1. Sprawdzenie, czy chat jest w ogóle włączony
        if (!chatManager.isChatEnabled()) {
            event.setCancelled(true);
            chatManager.sendMessage(player, "&cGlobalny chat jest aktualnie &cwyłączony&c.");
            return;
        }

        // 2. Sprawdzenie, czy jest włączony tryb VIP
        if (chatManager.isChatVipOnly()) {
            if (!player.hasPermission(chatManager.getVipPermission())) {
                event.setCancelled(true);
                chatManager.sendMessage(player, "&cObecnie jest włączony tryb &eVIP CHAT&c. Musisz mieć rangę &eVIP&c, aby pisać.");
            }
        }
    }
}