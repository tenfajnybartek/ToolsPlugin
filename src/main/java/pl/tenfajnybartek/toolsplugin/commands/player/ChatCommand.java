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
        super("chat", "Zarządza globalnym stanem chatu", "/chat <on/off/clear/vip>", "tools.cmd.chat", new String[]{"czat"});
        this.chatManager = chatManager;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {

        if (args.length != 1) {
            sendUsage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String senderName = sender.getName();
        String broadcastMessage;

        switch (subCommand) {
            case "on":
                if (chatManager.isChatEnabled() && !chatManager.isChatVipOnly()) {
                    chatManager.sendMessage(sender, "&aGlobalny chat jest już &ewłączony&a dla wszystkich.");
                    return true;
                }

                chatManager.setChatEnabled(true);
                chatManager.setChatVipOnly(false);

                broadcastMessage = "&6Chat serwera został &awłączony &6przez " + senderName + ".";
                chatManager.sendMessage(sender, "&aGlobalny chat został &ewłączony&a.");
                break;

            case "off":
                if (!chatManager.isChatEnabled()) {
                    chatManager.sendMessage(sender, "&cGlobalny chat jest już &cwyłączony&c.");
                    return true;
                }

                chatManager.setChatEnabled(false);
                chatManager.setChatVipOnly(false);

                broadcastMessage = "&6Chat serwera został &cwyłączony &6przez " + senderName + ".";
                chatManager.sendMessage(sender, "&cGlobalny chat został &cwyłączony&c.");
                break;

            case "clear":
                for (Player player : Bukkit.getOnlinePlayers()) {
                    for (int i = 0; i < 100; i++) {
                        player.sendMessage(" ");
                    }
                }

                chatManager.sendMessage(sender, "&aWyczyszczono chat dla wszystkich graczy.");

                broadcastMessage = "&6&lCHAT ZRESETOWANY: &aChat został wyczyszczony przez " + senderName + ".";
                break;

            case "vip":
                if (chatManager.isChatVipOnly()) {
                    chatManager.setChatVipOnly(false);
                    chatManager.setChatEnabled(true);

                    broadcastMessage = "&6Tryb &eVIP CHAT&6 został &cwyłączony&6 przez " + senderName + ".";
                    chatManager.sendMessage(sender, "&aTryb &eVIP CHAT&a został &cwyłączony&a. Chat jest &ewłączony&a dla wszystkich.");
                } else {
                    chatManager.setChatVipOnly(true);
                    chatManager.setChatEnabled(true);

                    broadcastMessage = "&6Tryb &eVIP CHAT&6 został &awłączony &6przez " + senderName + ".";
                    chatManager.sendMessage(sender, "&aTryb &eVIP CHAT&a został &ewłączony&a. Tylko gracze z permisją &e" + chatManager.getVipPermission() + " &amogą pisać."); // Użycie ChatManager
                }
                break;

            default:
                chatManager.sendMessage(sender, "&cNieprawidłowa opcja. Dostępne: on, off, clear, vip.");
                return true;
        }

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