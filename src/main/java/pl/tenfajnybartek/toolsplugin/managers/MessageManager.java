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

    public boolean sendPrivateMessage(Player sender, Player target, String message) {
        User targetUser = userManager.getUser(target);
        User senderUser = userManager.getUser(sender);

        if (targetUser == null || senderUser == null) {
            sender.sendMessage(ColorUtils.toComponent("&cWystąpił wewnętrzny błąd. Spróbuj się przelogować."));
            return false;
        }

        if (sender.equals(target)) {
            sender.sendMessage(ColorUtils.toComponent("&cNie możesz wysłać wiadomości do samego siebie."));
            return false;
        }

        if (!targetUser.isMsgToggle()) {
            sender.sendMessage(ColorUtils.toComponent("&cTen gracz wyłączył prywatne wiadomości (/msgtoggle)."));
            return false;
        }

        Component messageComponent = ColorUtils.toComponent(message).color(MSG_COLOR);

        Component senderMsg = MESSAGE_PREFIX
                .append(Component.text("Do ", NamedTextColor.WHITE))
                .append(Component.text(target.getName(), NamedTextColor.WHITE))
                .append(Component.text(": "))
                .append(messageComponent);

        Component targetMsg = MESSAGE_PREFIX
                .append(Component.text("Od ", NamedTextColor.WHITE))
                .append(Component.text(sender.getName(), NamedTextColor.WHITE))
                .append(Component.text(": "))
                .append(messageComponent);


        sender.sendMessage(senderMsg);
        target.sendMessage(targetMsg);

        targetUser.setLastMessageFrom(sender.getUniqueId());
        senderUser.setLastMessageFrom(target.getUniqueId());

        userManager.saveUserAsync(targetUser);
        userManager.saveUserAsync(senderUser);


        String spyLog = ColorUtils.stripColors(sender.getName()) + " -> " + ColorUtils.stripColors(target.getName()) + ": " + message;
        Component spyComponent = SPY_PREFIX.append(ColorUtils.toComponent(spyLog).color(MSG_COLOR));
        notifySocialSpy(sender, spyComponent);

        return true;
    }

    private void notifySocialSpy(Player sender, Component spyMessage) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.equals(sender)) continue;

            if (onlinePlayer.hasPermission("tools.socialspy")) {
                User user = userManager.getUser(onlinePlayer);

                if (user != null && user.isSocialSpy()) {
                    onlinePlayer.sendMessage(spyMessage);
                }
            }
        }
    }

    public Component getSpyPrefix() {
        return SPY_PREFIX;
    }
}