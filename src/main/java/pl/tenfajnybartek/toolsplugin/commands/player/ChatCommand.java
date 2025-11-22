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
            sendMessage(sender, "&cUżycie: " + getUsage());
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "on":
                chatManager.setChatEnabled(true);
                chatManager.setChatVipOnly(false);
                sendMessage(sender, "&aGlobalny chat został &ewłączony&a.");
                break;

            case "off":
                chatManager.setChatEnabled(false);
                chatManager.setChatVipOnly(false);
                sendMessage(sender, "&cGlobalny chat został &cwyłączony&c.");
                break;

            case "clear":
                if (!(sender instanceof Player)) {
                    sendMessage(sender, "&aChat został wyczyszczony (dla graczy).");
                }

                // Wysłanie 100 pustych linii
                for (int i = 0; i < 100; i++) {
                    Bukkit.broadcastMessage(" ");
                }
                // Użycie ColorUtils.colorize dla BroadcastMessage
                Bukkit.broadcastMessage(ColorUtils.colorize("&6&lCHAT ZRESETOWANY: &aChat został wyczyszczony przez " + sender.getName() + "."));
                sendMessage(sender, "&aWyczyszczono chat dla wszystkich graczy.");
                break;

            case "vip":
                if (chatManager.isChatVipOnly()) {
                    chatManager.setChatVipOnly(false);
                    chatManager.setChatEnabled(true);
                    sendMessage(sender, "&aTryb &eVIP CHAT&a został &cwyłączony&a. Chat jest &ewłączony&a dla wszystkich.");
                } else {
                    chatManager.setChatVipOnly(true);
                    chatManager.setChatEnabled(true);
                    sendMessage(sender, "&aTryb &eVIP CHAT&a został &ewłączony&a. Tylko gracze z permisją &e" + chatManager.getVipPermission() + " &amogą pisać.");
                }
                break;

            default:
                sendMessage(sender, "&cNieprawidłowa opcja. Dostępne: on, off, clear, vip.");
                return true;
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("on", "off", "clear", "vip").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return super.tabComplete(sender, args);
    }
}
