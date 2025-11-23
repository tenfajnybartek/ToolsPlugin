package pl.tenfajnybartek.toolsplugin.managers;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.utils.ColorUtils;

public class PermissionManager {

    // Pole przechowujące tylko instancję LuckPerms
    private final LuckPerms luckPermsApi;

    // 🚨 KOREKTA: Konstruktor przyjmuje tylko LuckPerms
    public PermissionManager(LuckPerms luckPermsApi) {
        this.luckPermsApi = luckPermsApi;
    }

    // --- LOGIKA POBIERANIA DANYCH ---

    /**
     * Pobiera prefix gracza, używając LuckPerms API.
     * @param player Gracz, dla którego pobierany jest prefix.
     * @return Sformatowany prefix lub pusty String.
     */
    public String getPlayerPrefix(Player player) {
        if (luckPermsApi == null) {
            return "";
        }

        // Pobieranie metadanych (bezpieczne dla wątków i wydajne)
        CachedMetaData meta = luckPermsApi.getPlayerAdapter(Player.class).getMetaData(player);
        String prefix = meta.getPrefix();

        // Zawsze kolorujemy, aby obsłużyć kody & (jeśli LP ich używa) lub kody Adventure
        return (prefix != null) ? ColorUtils.colorize(prefix) : "";
    }

    /**
     * Pobiera suffix gracza, używając LuckPerms API.
     * @param player Gracz, dla którego pobierany jest suffix.
     * @return Sformatowany suffix lub pusty String.
     */
    public String getPlayerSuffix(Player player) {
        if (luckPermsApi == null) {
            return "";
        }

        CachedMetaData meta = luckPermsApi.getPlayerAdapter(Player.class).getMetaData(player);
        String suffix = meta.getSuffix();

        return (suffix != null) ? ColorUtils.colorize(suffix) : "";
    }

    /**
     * Sprawdza, czy API LuckPerms zostało pomyślnie załadowane.
     */
    public boolean isLuckPermsAvailable() {
        return luckPermsApi != null;
    }

    /**
     * Zwraca instancję LuckPerms API.
     */
    public LuckPerms getLuckPermsApi() {
        return luckPermsApi;
    }
}