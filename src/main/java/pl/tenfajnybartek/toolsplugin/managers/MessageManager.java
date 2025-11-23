package pl.tenfajnybartek.toolsplugin.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
import pl.tenfajnybartek.toolsplugin.objects.User;
import pl.tenfajnybartek.toolsplugin.utils.ColorUtils;

import java.util.UUID;

public class MessageManager {
    private final ToolsPlugin plugin;
    private final UserManager userManager;

    // Prefiksy i formatowanie wiadomości (UŻYWAJĄC COMPONENT)
    private static final Component MESSAGE_PREFIX = Component.text("[", NamedTextColor.GRAY)
            .append(Component.text("Wiadomość", NamedTextColor.AQUA))
            .append(Component.text("] ", NamedTextColor.GRAY));

    private static final Component SPY_PREFIX = Component.text("[", NamedTextColor.DARK_GRAY)
            .append(Component.text("SPY", NamedTextColor.RED))
            .append(Component.text("] ", NamedTextColor.DARK_GRAY));

    private static final NamedTextColor MSG_COLOR = NamedTextColor.YELLOW;

    public MessageManager(ToolsPlugin plugin, UserManager userManager) {
        this.plugin = plugin;
        this.userManager = userManager;
    }

    /**
     * Główna metoda do wysyłania prywatnej wiadomości.
     */
    public boolean sendPrivateMessage(Player sender, Player target, String message) {
        // Zabezpieczenie na wypadek braku załadowania User
        User targetUser = userManager.getUser(target);
        User senderUser = userManager.getUser(sender);

        if (targetUser == null || senderUser == null) {
            sender.sendMessage(ColorUtils.toComponent("&cWystąpił wewnętrzny błąd. Spróbuj się przelogować."));
            return false;
        }

        // Zabezpieczenie przed pisaniem do siebie
        if (sender.equals(target)) {
            sender.sendMessage(ColorUtils.toComponent("&cNie możesz wysłać wiadomości do samego siebie."));
            return false;
        }

        // 1. Sprawdzenie, czy odbiorca chce otrzymywać wiadomości
        if (!targetUser.isMsgToggle()) {
            sender.sendMessage(ColorUtils.toComponent("&cTen gracz wyłączył prywatne wiadomości (/msgtoggle)."));
            return false;
        }

        // Konwersja tekstu wiadomości na Component (z obsługą & i HEX)
        Component messageComponent = ColorUtils.toComponent(message).color(MSG_COLOR);

        // 2. Formatowanie wiadomości
        // Wiadomość dla nadawcy (Do [target]: [message])
        Component senderMsg = MESSAGE_PREFIX
                .append(Component.text("Do ", NamedTextColor.WHITE))
                .append(Component.text(target.getName(), NamedTextColor.WHITE))
                .append(Component.text(": "))
                .append(messageComponent);

        // Wiadomość dla odbiorcy (Od [sender]: [message])
        Component targetMsg = MESSAGE_PREFIX
                .append(Component.text("Od ", NamedTextColor.WHITE))
                .append(Component.text(sender.getName(), NamedTextColor.WHITE))
                .append(Component.text(": "))
                .append(messageComponent);


        // 3. Wysyłanie wiadomości
        sender.sendMessage(senderMsg);
        target.sendMessage(targetMsg);

        // 4. Aktualizacja pól dla /reply
        targetUser.setLastMessageFrom(sender.getUniqueId());
        senderUser.setLastMessageFrom(target.getUniqueId());

        // 5. Zapis do bazy (Asynchroniczny zapis dzięki logice w UserManager)
        userManager.saveUser(targetUser, false);
        userManager.saveUser(senderUser, false);


        // 6. Powiadomienie Social Spy
        String spyLog = ColorUtils.stripColors(sender.getName()) + " -> " + ColorUtils.stripColors(target.getName()) + ": " + message;
        Component spyComponent = SPY_PREFIX.append(ColorUtils.toComponent(spyLog).color(MSG_COLOR));
        notifySocialSpy(sender, spyComponent);

        return true;
    }

    /**
     * Wyszukuje graczy z włączonym Social Spy i wysyła im log rozmowy.
     */
    private void notifySocialSpy(Player sender, Component spyMessage) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            // Pomiń nadawcę wiadomości (nie musi widzieć logu własnej wiadomości)
            if (onlinePlayer.equals(sender)) continue;

            // 1. Sprawdzenie uprawnień
            if (onlinePlayer.hasPermission("tools.socialspy")) {
                User user = userManager.getUser(onlinePlayer);

                // 2. Sprawdzenie statusu w User (czy Social Spy jest aktywnie włączony w ustawieniach gracza)
                if (user != null && user.isSocialSpy()) {
                    onlinePlayer.sendMessage(spyMessage);
                }
            }
        }
    }

    // Getter do sprawdzenia statusu Social Spy (używany przez komendę SocialSpyToggle)
    public Component getSpyPrefix() {
        return SPY_PREFIX;
    }
}