package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;
import pl.tenfajnybartek.toolsplugin.utils.ColorUtils;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.text.DecimalFormat;

public class TPSCommand extends BaseCommand {

    private static final DecimalFormat FORMATTER = new DecimalFormat("0.00");

    public TPSCommand() {
        super("tps", "Wyświetla TPS serwera oraz obciążenie", "/tps", "tfbhc.cmd.tps", new String[]{"diag"});
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {

        if (args.length != 0) {
            sendMessage(sender, "&cUżycie: " + getUsage());
            return true;
        }

        // ------------------ 1. TPS ------------------

        // Próba użycia refleksji na klasie CraftServer (standardowe rozwiązanie dla Bukkit/Spigot/Paper)
        double[] tps;
        try {
            Object server = Bukkit.getServer().getClass().getMethod("getServer").invoke(Bukkit.getServer());
            tps = (double[]) server.getClass().getField("recentTps").get(server);
        } catch (Exception e) {
            // Fallback w przypadku błędu (np. inna implementacja serwera)
            tps = new double[]{20.0, 20.0, 20.0};
        }

        String tps1min = formatTps(tps[0]);
        String tps5min = formatTps(tps[1]);
        String tps15min = formatTps(tps[2]);

        // ------------------ 2. PAMIĘĆ (RAM) ------------------

        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        // Konwersja na megabajty (MB)
        long usedMemory = memoryBean.getHeapMemoryUsage().getUsed() / 1048576;
        long maxMemory = memoryBean.getHeapMemoryUsage().getMax() / 1048576;

        // ------------------ 3. GRACZE ------------------

        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        int maxPlayers = Bukkit.getMaxPlayers();

        // ------------------ WYSYŁANIE RAPORTU ------------------

        sendMessage(sender, "&8--- &b&lDIAGNOSTYKA SERWERA &8---");

        // TPS
        sendMessage(sender, "&e&lTPS (Tick Per Second):");
        sendMessage(sender, String.format("&b 1m: &f%s &b| 5m: &f%s &b| 15m: &f%s", tps1min, tps5min, tps15min));

        // Pamięć
        sendMessage(sender, "&e&lPamięć (RAM):");
        sendMessage(sender, String.format("&b Użycie: &f%dMB &7/ &f%dMB", usedMemory, maxMemory));

        // Gracze
        sendMessage(sender, "&e&lGracze:");
        sendMessage(sender, String.format("&b Online: &f%d&7/&f%d", onlinePlayers, maxPlayers));

        sendMessage(sender, "&8---------------------------------");

        return true;
    }

    /**
     * Formatuje wartość TPS i koloruje ją w zależności od kondycji.
     */
    private String formatTps(double tps) {
        String color;
        if (tps >= 19.0) { // Zmienione z 18.0 na 19.0 dla surowszej oceny 'doskonale'
            color = "&a"; // Zielony (doskonale)
        } else if (tps >= 16.0) { // Zmienione z 15.0 na 16.0
            color = "&e"; // Żółty (akceptowalny)
        } else {
            color = "&c"; // Czerwony (lagi)
        }

        if (tps > 20.0) tps = 20.0;

        return ColorUtils.colorize(color + FORMATTER.format(tps));
    }
}
