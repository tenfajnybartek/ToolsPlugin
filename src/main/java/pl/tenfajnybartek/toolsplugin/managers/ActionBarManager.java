package pl.tenfajnybartek.toolsplugin.managers;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import pl.tenfajnybartek.toolsplugin.utils.ColorUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ActionBarManager z obsługą pinned persistent:
 *  - pinnedMessages: klucze, które są wysyłane KAŻDYM cyklem (np. vanish)
 *  - zwykłe persistent + ephemeral z priorytetami
 *  - heartbeat wciąż działa dla zwykłych persistent, ale pinned ignoruje warunek
 */
public class ActionBarManager {

    private final Plugin plugin;
    private final Map<UUID, PlayerActionBarState> states = new ConcurrentHashMap<>();

    private final int updateIntervalTicks;
    private final int maxEphemeralShown;
    private final boolean showMultipleEphemeral;

    private final boolean resendAlways;
    private final int persistentHeartbeatTicks;
    private final int noEphemeralHeartbeatTicks;
    private final Component separatorComponent;

    // Zestaw kluczy persistent, które mają być “przypięte”
    private final Set<String> pinnedKeys = new HashSet<>();

    public ActionBarManager(Plugin plugin,
                            int updateIntervalTicks,
                            boolean showMultipleEphemeral,
                            int maxEphemeralShown,
                            boolean resendAlways,
                            int persistentHeartbeatTicks,
                            int noEphemeralHeartbeatTicks,
                            String separator) {
        this.plugin = plugin;
        this.updateIntervalTicks = updateIntervalTicks;
        this.showMultipleEphemeral = showMultipleEphemeral;
        this.maxEphemeralShown = maxEphemeralShown;
        this.resendAlways = resendAlways;
        this.persistentHeartbeatTicks = Math.max(1, persistentHeartbeatTicks);
        this.noEphemeralHeartbeatTicks = Math.max(1, noEphemeralHeartbeatTicks);
        this.separatorComponent = ColorUtils.toComponent(separator);
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, updateIntervalTicks, updateIntervalTicks);
    }

    // API persistent
    public void setPersistent(Player player, String key, Component message) {
        PlayerActionBarState st = states.computeIfAbsent(player.getUniqueId(), u -> new PlayerActionBarState());
        st.persistentMessages.put(key, message);
        st.force = true;
    }

    public void updatePersistent(Player player, String key, Component message) {
        setPersistent(player, key, message);
    }

    public void removePersistent(Player player, String key) {
        PlayerActionBarState st = states.get(player.getUniqueId());
        if (st != null) {
            st.persistentMessages.remove(key);
            pinnedKeys.remove(key);
            st.force = true;
        }
    }

    public void clearPersistent(Player player) {
        PlayerActionBarState st = states.get(player.getUniqueId());
        if (st != null) {
            for (String k : st.persistentMessages.keySet()) {
                pinnedKeys.remove(k);
            }
            st.persistentMessages.clear();
            st.force = true;
        }
    }

    // Pin / Unpin
    public void pinPersistent(String key) {
        pinnedKeys.add(key);
    }

    public void unpinPersistent(String key) {
        pinnedKeys.remove(key);
    }

    // Ephemeral
    public void pushEphemeral(Player player, Component message, int durationTicks, ActionPriority priority) {
        PlayerActionBarState st = states.computeIfAbsent(player.getUniqueId(), u -> new PlayerActionBarState());
        long expireAt = System.currentTimeMillis() + (durationTicks * 50L);
        st.ephemeralMessages.add(new EphemeralMessage(message, expireAt, priority));
        st.force = true;
    }

    public void clearEphemeral(Player player) {
        PlayerActionBarState st = states.get(player.getUniqueId());
        if (st != null) {
            st.ephemeralMessages.clear();
            st.force = true;
        }
    }

    public void forceRefresh(Player player) {
        PlayerActionBarState st = states.get(player.getUniqueId());
        if (st != null) st.force = true;
    }

    public Component colored(String s) {
        return ColorUtils.toComponent(s);
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerActionBarState st = states.computeIfAbsent(player.getUniqueId(), u -> new PlayerActionBarState());
            st.cycles++;

            boolean expired = st.ephemeralMessages.removeIf(e -> e.expireAt <= now);
            if (expired) st.force = true;

            Component combined = buildCombined(st);

            boolean hasPersistent = !st.persistentMessages.isEmpty();
            boolean hasEphemeral = !st.ephemeralMessages.isEmpty();
            boolean anyPinnedPresent = st.persistentMessages.keySet().stream().anyMatch(pinnedKeys::contains);

            boolean shouldSend;
            if (resendAlways) {
                shouldSend = true;
            } else if (anyPinnedPresent) {
                // Jeśli mamy pinned – wysyłamy ZAWSZE (agresywny tryb tylko dla pinned)
                shouldSend = true;
            } else if (st.force) {
                shouldSend = true;
            } else if (!Objects.equals(st.lastSent, combined)) {
                shouldSend = true;
            } else {
                if (hasPersistent) {
                    int hb = hasEphemeral ? persistentHeartbeatTicks : noEphemeralHeartbeatTicks;
                    shouldSend = (st.cycles % hb == 0);
                } else {
                    shouldSend = false;
                }
            }

            if (shouldSend) {
                st.lastSent = combined;
                st.force = false;
                player.sendActionBar(combined);
            }
        }
    }

    private Component buildCombined(PlayerActionBarState st) {
        List<Component> parts = new ArrayList<>();

        st.persistentMessages.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .forEach(parts::add);

        if (!st.ephemeralMessages.isEmpty()) {
            if (showMultipleEphemeral) {
                st.ephemeralMessages.stream()
                        .sorted(Comparator.comparingInt((EphemeralMessage e) -> e.priority.weight()).reversed())
                        .limit(maxEphemeralShown)
                        .map(e -> e.message)
                        .forEach(parts::add);
            } else {
                st.ephemeralMessages.stream()
                        .max(Comparator.comparingInt(e -> e.priority.weight()))
                        .map(e -> e.message)
                        .ifPresent(parts::add);
            }
        }

        if (parts.isEmpty()) return Component.empty();

        Component result = Component.empty();
        boolean first = true;
        for (Component c : parts) {
            if (!first) result = result.append(separatorComponent);
            result = result.append(c);
            first = false;
        }
        return result;
    }

    // Struktury
    private static class PlayerActionBarState {
        Map<String, Component> persistentMessages = new HashMap<>();
        List<EphemeralMessage> ephemeralMessages = new ArrayList<>();
        Component lastSent;
        long cycles = 0;
        boolean force = false;
    }

    public record EphemeralMessage(Component message, long expireAt, ActionPriority priority) {}

    public enum ActionPriority {
        HIGH(3), MEDIUM(2), LOW(1);
        private final int w;
        ActionPriority(int w){ this.w = w; }
        public int weight(){ return w; }
    }
}