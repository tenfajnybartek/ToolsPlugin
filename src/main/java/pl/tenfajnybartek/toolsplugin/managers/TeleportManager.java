package pl.tenfajnybartek.toolsplugin.managers;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
import pl.tenfajnybartek.toolsplugin.objects.User;
import pl.tenfajnybartek.toolsplugin.utils.ColorUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeleportManager {

    private final ToolsPlugin plugin;
    private final ConfigManager configManager;
    private final UserManager userManager;
    private final ActionBarManager actionBarManager; // NOWE

    private final Map<UUID, PendingTeleport> pendingTeleports;
    private final Map<UUID, Location> lastLocations;
    private final Map<UUID, TpaRequest> tpaRequests;

    public TeleportManager(ToolsPlugin plugin,
                           ConfigManager configManager,
                           UserManager userManager,
                           ActionBarManager actionBarManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.userManager = userManager;
        this.actionBarManager = actionBarManager;
        this.pendingTeleports = new HashMap<>();
        this.lastLocations = new HashMap<>();
        this.tpaRequests = new HashMap<>();
    }

    public void teleport(Player player, Location destination, String successMessage) {
        int delay = configManager.getTeleportDelay();

        saveLastLocation(player);

        if (delay <= 0) {
            teleportNow(player, destination, successMessage);
            return;
        }

        Location startLocation = player.getLocation().clone();
        PendingTeleport pending = new PendingTeleport(player, destination, startLocation, successMessage);
        pendingTeleports.put(player.getUniqueId(), pending);

        if (actionBarManager != null) {
            actionBarManager.setPersistent(player, "tp",
                    ColorUtils.toComponent("&aTeleport za &e" + delay + "s &7(Nie ruszaj się)"));
        } else {
            player.sendMessage(ColorUtils.colorize("&aTeleportacja za &e" + delay + "s&a. Nie ruszaj się!"));
        }

        new BukkitRunnable() {
            int countdown = delay;

            @Override
            public void run() {
                UUID uuid = player.getUniqueId();
                if (!pendingTeleports.containsKey(uuid)) {
                    if (actionBarManager != null) actionBarManager.removePersistent(player, "tp");
                    this.cancel();
                    return;
                }

                countdown--;

                if (countdown > 0) {
                    if (actionBarManager != null) {
                        String timeColored = countdown <= 3 ? "&c" + countdown + "s" : "&e" + countdown + "s";
                        actionBarManager.updatePersistent(player, "tp",
                                ColorUtils.toComponent("&aTeleport za " + timeColored + " &7(Nie ruszaj się)"));
                    } else if (countdown <= 3) {
                        player.sendMessage(ColorUtils.colorize("&e" + countdown + "..."));
                    }
                } else {
                    PendingTeleport tp = pendingTeleports.remove(uuid);
                    if (tp != null) {
                        teleportNow(player, tp.getDestination(), tp.getSuccessMessage());
                    }
                    if (actionBarManager != null) {
                        actionBarManager.removePersistent(player, "tp");
                        actionBarManager.pushEphemeral(player,
                                ColorUtils.toComponent("&aTeleport zakończony."),
                                40, ActionBarManager.ActionPriority.MEDIUM);
                    }
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void teleportNow(Player player, Location destination, String successMessage) {
        if (configManager.isSafeTeleportEnabled()) {
            destination = findSafeLocation(destination);
        }
        player.teleport(destination);
        if (successMessage != null && !successMessage.isEmpty()) {
            player.sendMessage(ColorUtils.colorize(successMessage));
        }
    }

    public void cancelTeleport(Player player, String reason) {
        UUID uuid = player.getUniqueId();
        if (pendingTeleports.containsKey(uuid)) {
            pendingTeleports.remove(uuid);
            if (actionBarManager != null) {
                actionBarManager.removePersistent(player, "tp");
                actionBarManager.pushEphemeral(player,
                        ColorUtils.toComponent("&cAnulowano: &7" + reason),
                        60, ActionBarManager.ActionPriority.HIGH);
            }
            player.sendMessage(ColorUtils.colorize("&cTeleportacja anulowana! &7(" + reason + ")"));
        }
    }

    public boolean hasPendingTeleport(Player player) {
        return pendingTeleports.containsKey(player.getUniqueId());
    }

    public Location getStartLocation(Player player) {
        PendingTeleport pending = pendingTeleports.get(player.getUniqueId());
        return pending != null ? pending.getStartLocation() : null;
    }

    public void saveLastLocation(Player player) {
        User user = userManager.getUser(player);
        if (user != null && !user.isTeleportToggle()) {
            return;
        }
        lastLocations.put(player.getUniqueId(), player.getLocation().clone());
    }

    public Location getLastLocation(Player player) {
        return lastLocations.get(player.getUniqueId());
    }

    private Location findSafeLocation(Location location) {
        Location safe = location.clone();
        if (!safe.getBlock().getType().isSolid() &&
                !safe.clone().add(0, 1, 0).getBlock().getType().isSolid() &&
                safe.clone().add(0, -1, 0).getBlock().getType().isSolid()) {
            return safe;
        }
        for (int i = 1; i <= 5; i++) {
            Location test = safe.clone().add(0, i, 0);
            if (!test.getBlock().getType().isSolid() &&
                    !test.clone().add(0, 1, 0).getBlock().getType().isSolid() &&
                    test.clone().add(0, -1, 0).getBlock().getType().isSolid()) {
                return test;
            }
        }
        return safe;
    }

    public void addTpaRequest(Player sender, Player target) {
        long expirationTime = 60 * 20L;
        tpaRequests.put(target.getUniqueId(), new TpaRequest(sender.getUniqueId()));

        new BukkitRunnable() {
            @Override
            public void run() {
                TpaRequest request = tpaRequests.get(target.getUniqueId());
                if (request != null && request.getSenderId().equals(sender.getUniqueId())) {
                    tpaRequests.remove(target.getUniqueId());
                    if (sender.isOnline()) {
                        sender.sendMessage(ColorUtils.colorize("&cTwoja prośba do " + target.getName() + " wygasła."));
                        if (actionBarManager != null) {
                            actionBarManager.pushEphemeral(sender,
                                    ColorUtils.toComponent("&cTPA do " + target.getName() + " wygasła."),
                                    60, ActionBarManager.ActionPriority.MEDIUM);
                        }
                    }
                }
            }
        }.runTaskLater(plugin, expirationTime);

        if (actionBarManager != null) {
            actionBarManager.setPersistent(target, "tpa",
                    ColorUtils.toComponent("&eTPA od &6" + sender.getName() + " &7(/tpaccept /tpadeny)"));
        }
    }

    public TpaRequest getTpaRequest(Player target) {
        return tpaRequests.get(target.getUniqueId());
    }

    public void removeTpaRequest(Player target) {
        tpaRequests.remove(target.getUniqueId());
        if (actionBarManager != null) {
            actionBarManager.removePersistent(target, "tpa");
        }
    }

    private static class PendingTeleport {
        private final Player player;
        private final Location destination;
        private final Location startLocation;
        private final String successMessage;
        public PendingTeleport(Player player, Location destination, Location startLocation, String successMessage) {
            this.player = player;
            this.destination = destination;
            this.startLocation = startLocation;
            this.successMessage = successMessage;
        }
        public Location getDestination() { return destination; }
        public Location getStartLocation() { return startLocation; }
        public String getSuccessMessage() { return successMessage; }
    }

    public static class TpaRequest {
        private final UUID senderId;
        private final long timestamp;
        public TpaRequest(UUID senderId) {
            this.senderId = senderId;
            this.timestamp = System.currentTimeMillis();
        }
        public UUID getSenderId() { return senderId; }
        public long getTimestamp() { return timestamp; }
    }
}