package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.managers.ChatManager;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;
import pl.tenfajnybartek.toolsplugin.utils.ColorUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ChatCommand extends BaseCommand {

    private final ChatManager chatManager;

    public ChatCommand(ChatManager chatManager) {
        super("chat", "Zarządza globalnym stanem chatu", "/chat <on/off/clear/vip>", "tfbhc.cmd.chat", new String[]{"czat"});
        this.chatManager = chatManager;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {

        if (args.length != 1) {
            chatManager.sendMessage(sender, "&cUżycie: " + getUsage()); // Użycie ChatManager
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String senderName = sender.getName();
        String broadcastMessage;

        switch (subCommand) {
            case "on":
                // Stan "VIP only" oznacza, że chat jest włączony, ale z restrykcjami
                if (chatManager.isChatEnabled() && !chatManager.isChatVipOnly()) {
                    chatManager.sendMessage(sender, "&aGlobalny chat jest już &ewłączony&a dla wszystkich.");
                    return true;
                }

                chatManager.setChatEnabled(true);
                chatManager.setChatVipOnly(false);

                broadcastMessage = "&6Chat serwera został &awłączony &6przez " + senderName + ".";
                chatManager.sendMessage(sender, "&aGlobalny chat został &ewłączony&a."); // Użycie ChatManager
                break;

            case "off":
                if (!chatManager.isChatEnabled()) {
                    chatManager.sendMessage(sender, "&cGlobalny chat jest już &cwyłączony&c.");
                    return true;
                }

                chatManager.setChatEnabled(false);
                chatManager.setChatVipOnly(false);

                broadcastMessage = "&6Chat serwera został &cwyłączony &6przez " + senderName + ".";
                chatManager.sendMessage(sender, "&cGlobalny chat został &cwyłączony&c."); // Użycie ChatManager
                break;

            case "clear":
                // Wysłanie 100 pustych linii (działa to synchronicznie)
                for (Player player : Bukkit.getOnlinePlayers()) {
                    for (int i = 0; i < 100; i++) {
                        player.sendMessage(" ");
                    }
                }

                chatManager.sendMessage(sender, "&aWyczyszczono chat dla wszystkich graczy."); // Użycie ChatManager

                // Wiadomość broadcast powinna być wysłana raz
                broadcastMessage = "&6&lCHAT ZRESETOWANY: &aChat został wyczyszczony przez " + senderName + ".";
                break;

            case "vip":
                if (chatManager.isChatVipOnly()) {
                    chatManager.setChatVipOnly(false);
                    // Resetowanie do stanu "chat włączony dla wszystkich"
                    chatManager.setChatEnabled(true);

                    broadcastMessage = "&6Tryb &eVIP CHAT&6 został &cwyłączony&6 przez " + senderName + ".";
                    chatManager.sendMessage(sender, "&aTryb &eVIP CHAT&a został &cwyłączony&a. Chat jest &ewłączony&a dla wszystkich."); // Użycie ChatManager
                } else {
                    chatManager.setChatVipOnly(true);
                    // Włączenie globalnego chatu, jeśli VIP jest włączany
                    chatManager.setChatEnabled(true);

                    broadcastMessage = "&6Tryb &eVIP CHAT&6 został &awłączony &6przez " + senderName + ".";
                    chatManager.sendMessage(sender, "&aTryb &eVIP CHAT&a został &ewłączony&a. Tylko gracze z permisją &e" + chatManager.getVipPermission() + " &amogą pisać."); // Użycie ChatManager
                }
                break;

            default:
                chatManager.sendMessage(sender, "&cNieprawidłowa opcja. Dostępne: on, off, clear, vip."); // Użycie ChatManager
                return true;
        }

        // 🚨 Krok 1: Wysłanie globalnej wiadomości z koloryzacją
        // Wysłanie komunikatu do wszystkich (clear i on/off/vip)
        Bukkit.broadcastMessage(ColorUtils.colorize(broadcastMessage));

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("on", "off", "clear", "vip").stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return super.tabComplete(sender, args);
    }
}