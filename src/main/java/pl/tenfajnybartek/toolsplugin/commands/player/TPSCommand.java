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

        double[] tps = Bukkit.getServer().getTPS();

        String tps1min = formatTps(tps[0]);
        String tps5min = formatTps(tps[1]);
        String tps15min = formatTps(tps[2]);

        sendMessage(sender, "&e&lTPS:");
        sendMessage(sender, "&b 1 min: &f" + tps1min + " &7/ &b5 min: &f" + tps5min + " &7/ &b15 min: &f" + tps15min);


        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long usedMemory = memoryBean.getHeapMemoryUsage().getUsed() / 1048576;
        long maxMemory = memoryBean.getHeapMemoryUsage().getMax() / 1048576;
        sendMessage(sender, "&e&lPamięć (RAM):");
        sendMessage(sender, "&b Zużycie: &f" + usedMemory + "MB &7/ &bMaks: &f" + maxMemory + "MB");

        // ------------------ 3. GRACZE ------------------
        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        int maxPlayers = Bukkit.getMaxPlayers();
        sendMessage(sender, "&e&lGracze:");
        sendMessage(sender, String.format("&b Online: &f%d&7/&f%d", onlinePlayers, maxPlayers));

        sendMessage(sender, "&8---------------------------------");

        return true;
    }

    /**
     * Formatuje wartość TPS i koloruje ją w zależności od kondycji.
     */
    private String formatTps(double tps) {
        String formatted = FORMATTER.format(tps);
        if (tps >= 19.5) return "&a" + formatted;
        if (tps >= 15.0) return "&e" + formatted;
        return "&c" + formatted;
    }
}