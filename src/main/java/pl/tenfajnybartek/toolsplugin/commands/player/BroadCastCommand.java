package pl.tenfajnybartek.toolsplugin.commands.player;

import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;
import pl.tenfajnybartek.toolsplugin.utils.ColorUtils;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BroadCastCommand extends BaseCommand {

    private static final List<String> BROADCAST_TYPES = Arrays.asList("chat", "actionbar", "title");
    private static final String DEFAULT_PREFIX = "&8[&4&lUWAGA&8] &r";
    private static final int ACTIONBAR_DURATION_SECONDS = 3; // Nowy czas trwania paska

    public BroadCastCommand() {
        super("broadcast", "Wysyła ogłoszenie do wszystkich", "/broadcast <chat/actionbar/title> <wiadomość>", "tfbhc.cmd.broadcast", new String[]{"ogłoszenie", "bc"});
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {

        if (args.length < 2) {
            sendMessage(sender, "&cUżycie: " + getUsage());
            return true;
        }

        String type = args[0].toLowerCase();

        if (!BROADCAST_TYPES.contains(type)) {
            sendMessage(sender, "&cNieprawidłowy typ ogłoszenia. Użyj: &e&lchat, actionbar &cibądź &e&ltitle.");
            return true;
        }

        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        // Używamy prefixu, ale w Title będzie on tytułem, nie częścią wiadomości
        String prefixComponent = DEFAULT_PREFIX;
        String fullChatMessage = prefixComponent + message;

        // 1. CHAT
        if (type.equals("chat")) {
            Bukkit.getServer().sendMessage(ColorUtils.toComponent(fullChatMessage));

            // 2. ACTIONBAR
        } else if (type.equals("actionbar")) {
            final int repeats = ACTIONBAR_DURATION_SECONDS * 20 / 4; // Wysłanie co 4 ticki przez X sekund
            final net.kyori.adventure.text.Component component = ColorUtils.toComponent(fullChatMessage);

            // Logika wielokrotnego wysyłania (BukkitRunnable)
            new BukkitRunnable() {
                int count = 0;

                @Override
                public void run() {
                    if (count++ >= repeats) {
                        this.cancel();
                        return;
                    }
                    // Actionbar jest wysyłany indywidualnie do każdego gracza
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendActionBar(component);
                    }
                }
            }.runTaskTimer(ToolsPlugin.getInstance(), 0L, 4L); // Start natychmiast, powtarzaj co 4 ticki

            sendMessage(sender, prefixComponent + "&7Wysłano ogłoszenie na Actionbar (przez " + ACTIONBAR_DURATION_SECONDS + "s).");

            // 3. TITLE
        } else if (type.equals("title")) {

            // NOWE CZASY: 4 sekundy 'stay'
            Duration fadeIn = Duration.ofMillis(500);  // 0.5s
            Duration stay = Duration.ofSeconds(4);     // 4.0s
            Duration fadeOut = Duration.ofSeconds(1);    // 1.0s

            // NOWA LOGIKA: Prefix jako Tytuł, Wiadomość jako Podtytuł
            Title title = Title.title(
                    ColorUtils.toComponent(prefixComponent),    // Główny tytuł: [OGŁOSZENIE]
                    ColorUtils.toComponent(message),            // Podtytuł: Wiadomość
                    Title.Times.times(fadeIn, stay, fadeOut)
            );

            // Wysyłamy tytuł do każdego gracza
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.showTitle(title);
            }
        }

        sendMessage(sender, "&aOgłoszenie typu &e" + type.toUpperCase() + " &azostało wysłane!");

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return BROADCAST_TYPES.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            return Arrays.asList("Witajcie na serwerze!", "Restart za 5 minut...", "Zapraszamy do gry!").stream()
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return super.tabComplete(sender, args);
    }
}
