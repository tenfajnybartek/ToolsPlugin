package pl.tenfajnybartek.toolsplugin.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import pl.tenfajnybartek.toolsplugin.utils.ColorUtils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HelpopManager {

    // Hardcoded permisje
    public static final String PERM_SEND    = "tools.cmd.helpop";
    public static final String PERM_RECEIVE = "tools.cmd.helpop.receive";
    public static final String PERM_BYPASS  = "tools.cmd.helpop.bypass";
    public static final String PERM_TOGGLE  = "tools.cmd.helpop.toggle";

    // Hardcoded format (placeholdery: %player%, %message%)
    private static final String MESSAGE_FORMAT = "&c[HelpOp] &f%player% &7: &7%message%";

    private final JavaPlugin plugin;
    private final ConfigManager configManager;

    private volatile boolean enabled;
    private int cooldownSeconds;

    private final Map<UUID, Long> lastSent = new ConcurrentHashMap<>();

    public HelpopManager(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        load();
    }

    public void load() {
        this.enabled = configManager.getConfig().getBoolean("helpop.enabled", true);
        this.cooldownSeconds = configManager.getConfig().getInt("helpop.cooldown-seconds", 30);
    }

    public void reload() {
        load();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean setEnabled(boolean state) {
        if (this.enabled != state) {
            this.enabled = state;
            configManager.getConfig().set("helpop.enabled", state);
            configManager.saveConfig();
            return true;
        }
        return false;
    }

    public int getRemainingCooldown(Player player) {
        if (player.hasPermission(PERM_BYPASS)) return 0;
        Long last = lastSent.get(player.getUniqueId());
        if (last == null) return 0;

        int elapsed = (int) ((System.currentTimeMillis() - last) / 1000L);
        int remaining = cooldownSeconds - elapsed;
        return Math.max(0, remaining);
    }

    public boolean canSend(Player player) {
        if (!enabled) return false;
        if (!player.hasPermission(PERM_SEND)) return false;
        return getRemainingCooldown(player) == 0;
    }

    public void recordSend(Player player) {
        lastSent.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public void sendHelpop(Player sender, String rawMessage) {
        String colored = MESSAGE_FORMAT
                .replace("%player%", sender.getName())
                .replace("%message%", rawMessage);

        Component component = ColorUtils.toComponent(colored)
                .hoverEvent(HoverEvent.showText(ColorUtils.toComponent("&aKliknij aby się teleportować do &f" + sender.getName())))
                .clickEvent(ClickEvent.runCommand("/tp " + sender.getName()));

        int delivered = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission(PERM_RECEIVE)) {
                online.sendMessage(component);
                delivered++;
            }
        }

        if (delivered == 0) {
            sender.sendMessage(ColorUtils.toComponent("&cBrak dostępnych administratorów. Spróbuj później."));
        } else {
            sender.sendMessage(ColorUtils.toComponent("&aWysłano wiadomość do &e" + delivered + " &aadministratorów."));
        }
    }
}
