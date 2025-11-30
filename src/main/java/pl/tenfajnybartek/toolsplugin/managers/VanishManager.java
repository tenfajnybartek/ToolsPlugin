package pl.tenfajnybartek.toolsplugin.managers;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pl.tenfajnybartek.toolsplugin.utils.ColorUtils;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VanishManager {

    public static final String PERM_BASE      = "tools.cmd.vanish";
    public static final String PERM_ADMIN     = PERM_BASE + ".admin";
    public static final String PERM_SEE       = PERM_BASE + ".see";
    public static final String PERM_PICKUP    = PERM_BASE + ".pickup";
    public static final String PERM_MOBTARGET = PERM_BASE + ".mobtarget";
    public static final String PERM_FLIGHT    = PERM_BASE + ".flight";

    private final JavaPlugin plugin;
    private final ActionBarManager actionBarManager;
    private final Set<UUID> vanished = ConcurrentHashMap.newKeySet();

    private final boolean giveInvisibilityEffect = true;
    private final boolean giveFlightIfAllowed = true;
    private final boolean blockItemPickup = true;
    private final boolean hideJoinQuitMessages = true;
    private final boolean preventMobTarget = true;

    private final boolean vanishAbEnabled;
    private final String vanishText;

    private static final String VANISH_KEY = "vanish";

    public VanishManager(JavaPlugin plugin, ActionBarManager actionBarManager) {
        this.plugin = plugin;
        this.actionBarManager = actionBarManager;

        this.vanishAbEnabled = plugin.getConfig().getBoolean("actionbar.vanish.enabled", true);
        this.vanishText = plugin.getConfig().getString("actionbar.vanish.text", "&a&lJESTEŚ NIEWIDZIALNY!");

        if (actionBarManager != null && vanishAbEnabled) {
            actionBarManager.pinPersistent(VANISH_KEY);
        }
    }

    public boolean isVanished(Player player) {
        return vanished.contains(player.getUniqueId());
    }

    public Set<UUID> getVanishedUUIDs() {
        return Collections.unmodifiableSet(vanished);
    }

    public void toggleSelf(Player player) {
        setVanish(player, !isVanished(player));
    }

    public void setVanish(Player player, boolean vanish) {
        boolean already = isVanished(player);

        if (vanish && !already) {
            vanished.add(player.getUniqueId());
            applyEffects(player);
            hideFromOthers(player);
            player.sendMessage(ColorUtils.colorize("&aJesteś teraz &ew ukryciu&a."));

            if (actionBarManager != null && vanishAbEnabled) {
                actionBarManager.setPersistent(player, VANISH_KEY, ColorUtils.toComponent(vanishText));
                actionBarManager.pinPersistent(VANISH_KEY);
            }

        } else if (!vanish && already) {
            showToOthers(player);
            removeEffects(player);
            vanished.remove(player.getUniqueId());
            player.sendMessage(ColorUtils.colorize("&cNie jesteś już ukryty."));

            if (actionBarManager != null) {
                actionBarManager.removePersistent(player, VANISH_KEY);
                if (vanished.isEmpty()) {
                    actionBarManager.unpinPersistent(VANISH_KEY);
                }
            }

        } else {
            player.sendMessage(ColorUtils.colorize("&7Stan bez zmian (&e" + (vanish ? "ukryty" : "widoczny") + "&7)."));
        }
    }

    private void applyEffects(Player p) {
        if (giveInvisibilityEffect) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, false, false, false));
        }
        if (giveFlightIfAllowed && p.hasPermission(PERM_FLIGHT)
                && p.getGameMode() != GameMode.CREATIVE
                && p.getGameMode() != GameMode.SPECTATOR) {
            p.setAllowFlight(true);
        }
    }

    private void removeEffects(Player p) {
        if (giveInvisibilityEffect) {
            p.removePotionEffect(PotionEffectType.INVISIBILITY);
        }
        if (giveFlightIfAllowed && p.hasPermission(PERM_FLIGHT)
                && p.getGameMode() != GameMode.CREATIVE
                && p.getGameMode() != GameMode.SPECTATOR) {
            p.setAllowFlight(false);
            p.setFlying(false);
        }
    }

    private void hideFromOthers(Player vanishedPlayer) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!other.equals(vanishedPlayer) && !other.hasPermission(PERM_SEE)) {
                other.hidePlayer(plugin, vanishedPlayer);
            }
        }
    }

    private void showToOthers(Player vanishedPlayer) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!other.equals(vanishedPlayer)) {
                other.showPlayer(plugin, vanishedPlayer);
            }
        }
    }

    public void syncOnJoin(Player joining) {
        for (UUID uuid : vanished) {
            Player v = Bukkit.getPlayer(uuid);
            if (v != null && !joining.hasPermission(PERM_SEE)) {
                joining.hidePlayer(plugin, v);
            }
        }
        if (isVanished(joining) && actionBarManager != null && vanishAbEnabled) {
            actionBarManager.setPersistent(joining, VANISH_KEY, ColorUtils.toComponent(vanishText));
            actionBarManager.pinPersistent(VANISH_KEY);
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

    public void clearAll() {
        for (UUID uuid : vanished) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                removeEffects(p);
                showToOthers(p);
                if (actionBarManager != null) {
                    actionBarManager.removePersistent(p, VANISH_KEY);
                }
            }
        }
        vanished.clear();
        if (actionBarManager != null) {
            actionBarManager.unpinPersistent(VANISH_KEY);
        }
    }
}