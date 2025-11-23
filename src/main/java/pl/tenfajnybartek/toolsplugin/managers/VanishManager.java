package pl.tenfajnybartek.toolsplugin.managers;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import pl.tenfajnybartek.toolsplugin.utils.ColorUtils;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VanishManager {

    public static final String PERM_BASE      = "tfbhc.cmd.vanish";
    public static final String PERM_ADMIN     = PERM_BASE + ".admin";
    public static final String PERM_SEE       = PERM_BASE + ".see";
    public static final String PERM_PICKUP    = PERM_BASE + ".pickup";
    public static final String PERM_MOBTARGET = PERM_BASE + ".mobtarget";
    public static final String PERM_FLIGHT    = PERM_BASE + ".flight";

    private final JavaPlugin plugin;
    private final Set<UUID> vanished = ConcurrentHashMap.newKeySet();

    // Ustawienia (na razie hardcoded)
    private final boolean giveInvisibilityEffect = true;
    private final boolean giveFlightIfAllowed = true;
    private final boolean blockItemPickup = true;
    private final boolean hideJoinQuitMessages = true;
    private final boolean preventMobTarget = true;

    // ActionBar task
    private BukkitTask actionBarTask;
    private static final String ACTION_BAR_TEXT = "&a&lJESTEŚ NIEWIDZIALNY!";

    public VanishManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isVanished(Player player) {
        return vanished.contains(player.getUniqueId());
    }

    public void toggleSelf(Player player) {
        setVanish(player, !isVanished(player));
    }

    public void setVanish(Player player, boolean vanish) {
        boolean already = isVanished(player);
        if (vanish && !already) {
            vanished.add(player.getUniqueId());
            applyVanishEffects(player);
            hideFromOthers(player);
            player.sendMessage(ColorUtils.colorize("&8[&cTools&8] &aJesteś teraz &ew ukryciu&a."));
            ensureActionBarTask();
        } else if (!vanish && already) {
            showToOthers(player);
            removeVanishEffects(player);
            vanished.remove(player.getUniqueId());
            player.sendMessage(ColorUtils.colorize("&8[&cTools&8] &cNie jesteś już ukryty."));
            checkStopActionBarTask();
        } else {
            player.sendMessage(ColorUtils.colorize("&8[&cTools&8] &7Stan bez zmian (&e" + (vanish ? "ukryty" : "widoczny") + "&7)."));
        }
    }

    private void applyVanishEffects(Player p) {
        if (giveInvisibilityEffect) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, false, false, false));
        }
        if (giveFlightIfAllowed && p.hasPermission(PERM_FLIGHT)) {
            if (p.getGameMode() != GameMode.CREATIVE && p.getGameMode() != GameMode.SPECTATOR) {
                p.setAllowFlight(true);
            }
        }
    }

    private void removeVanishEffects(Player p) {
        if (giveInvisibilityEffect) {
            p.removePotionEffect(PotionEffectType.INVISIBILITY);
        }
        if (giveFlightIfAllowed && p.hasPermission(PERM_FLIGHT)) {
            if (p.getGameMode() != GameMode.CREATIVE && p.getGameMode() != GameMode.SPECTATOR) {
                p.setAllowFlight(false);
                p.setFlying(false);
            }
        }
    }

    private void hideFromOthers(Player vanishedPlayer) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(vanishedPlayer)) continue;
            if (!other.hasPermission(PERM_SEE)) {
                other.hidePlayer(plugin, vanishedPlayer);
            }
        }
    }

    private void showToOthers(Player vanishedPlayer) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(vanishedPlayer)) continue;
            other.showPlayer(plugin, vanishedPlayer);
        }
    }

    public void syncOnJoin(Player joining) {
        // Ukryj join message jeśli gracz już był w vanish (powrót)
        if (isVanished(joining) && hideJoinQuitMessages) {
            // joinMessage null ustawiamy w listenerze
        }
        // Ukryj przed nim innych w vanish jeśli nie ma uprawnienia
        for (UUID uuid : vanished) {
            Player v = Bukkit.getPlayer(uuid);
            if (v != null && !joining.hasPermission(PERM_SEE)) {
                joining.hidePlayer(plugin, v);
            }
        }
    }

    public boolean shouldBlockPickup(Player p) {
        return blockItemPickup && isVanished(p) && !p.hasPermission(PERM_PICKUP);
    }

    public boolean shouldPreventMobTarget(Player p) {
        return preventMobTarget && isVanished(p) && !p.hasPermission(PERM_MOBTARGET);
    }

    public boolean shouldHideJoinQuitMessages() {
        return hideJoinQuitMessages;
    }

    private void ensureActionBarTask() {
        if (actionBarTask == null) {
            actionBarTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                for (UUID uuid : vanished) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null && p.isOnline()) {
                        p.sendActionBar(ColorUtils.toComponent(ACTION_BAR_TEXT));
                    }
                }
            }, 20L, 40L); // start po 1s, odśwież co 2s
        }
    }

    private void checkStopActionBarTask() {
        if (vanished.isEmpty() && actionBarTask != null) {
            actionBarTask.cancel();
            actionBarTask = null;
        }
    }

    public void clearAll() {
        for (UUID uuid : vanished) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                removeVanishEffects(p);
                showToOthers(p);
            }
        }
        vanished.clear();
        checkStopActionBarTask();
    }
}
