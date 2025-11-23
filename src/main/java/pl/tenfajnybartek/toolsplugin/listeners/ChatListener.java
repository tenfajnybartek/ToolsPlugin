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
    // 🚨 KOREKTA: Używamy nowego, nieprzestarzałego zdarzenia
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        // Konwersja wiadomości Adventure na stary String dla naszych managerów
        // (Wiadomość gracza, którą wpisał, jest w event.message())
        String rawMessage = LegacyComponentSerializer.legacySection().serialize(event.message());

        // 1. Weryfikacja (Bypass i stany chatu)

        // 1.1 Sprawdzenie, czy chat jest w ogóle włączony
        if (!chatManager.isChatEnabled()) {
            if (!player.hasPermission("tfbhc.chat.bypass")) {
                event.setCancelled(true);
                chatManager.sendMessage(player, "&cGlobalny chat jest aktualnie &cwyłączony&c.");
                return;
            }
        }

        // 1.2 Sprawdzenie, czy jest włączony tryb VIP
        if (chatManager.isChatVipOnly()) {
            if (!player.hasPermission(chatManager.getVipPermission())) {
                if (!player.hasPermission("tfbhc.chat.bypass")) {
                    event.setCancelled(true);
                    chatManager.sendMessage(player, "&cObecnie jest włączony tryb &eVIP CHAT&c. Musisz mieć rangę &eVIP&c, aby pisać.");
                    return;
                }
            }
        }

        // 🚨 Krok 2: PRZEJĘCIE KONTROLI I FORMATOWANIE

        // Generujemy w pełni sformatowany string (z prefixami, suffixami, kolorami)
        String customFormat = chatManager.formatAndSend(player, rawMessage);

        // Anulujemy zdarzenie, aby Bukkit nie wysłał surowej wiadomości.
        event.setCancelled(true);

        // 🚨 KROK 3: RĘCZNE ROZSYŁANIE DO GRACZY ONLINE

        // Konwersja sformatowanego Stringa z powrotem na Component dla Adventure API
        net.kyori.adventure.text.Component formattedComponent =
                LegacyComponentSerializer.legacySection().deserialize(customFormat);

        // Używamy Bukkit.sendMessage(Component) do rozesłania w grze
        Bukkit.getServer().sendMessage(formattedComponent);

        // 4. Zapis do konsoli (dla logów)
        // Konsola zazwyczaj akceptuje Adventure Component, ale dla bezpieczeństwa można użyć Bukkit.getConsoleSender()
        Bukkit.getConsoleSender().sendMessage(customFormat);
    }
}