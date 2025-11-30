package pl.tenfajnybartek.toolsplugin.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import pl.tenfajnybartek.toolsplugin.managers.ChatManager;

public class ChatListener implements Listener {

    private final ChatManager chatManager;

    public ChatListener(ChatManager chatManager) {
        this.chatManager = chatManager;
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        String rawMessage = LegacyComponentSerializer.legacySection().serialize(event.message());

        if (!chatManager.isChatEnabled()) {
            if (!player.hasPermission("tfbhc.chat.bypass")) {
                event.setCancelled(true);
                chatManager.sendMessage(player, "&cGlobalny chat jest aktualnie &cwyłączony&c.");
                return;
            }
        }

        if (chatManager.isChatVipOnly()) {
            if (!player.hasPermission(chatManager.getVipPermission())) {
                if (!player.hasPermission("tfbhc.chat.bypass")) {
                    event.setCancelled(true);
                    chatManager.sendMessage(player, "&cObecnie jest włączony tryb &eVIP CHAT&c. Musisz mieć rangę &eVIP&c, aby pisać.");
                    return;
                }
            }
        }

        String customFormat = chatManager.formatAndSend(player, rawMessage);

        event.setCancelled(true);

        net.kyori.adventure.text.Component formattedComponent =
                LegacyComponentSerializer.legacySection().deserialize(customFormat);

        Bukkit.getServer().sendMessage(formattedComponent);

        Bukkit.getConsoleSender().sendMessage(customFormat);
    }
}