package pl.tenfajnybartek.toolsplugin.managers;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.utils.ColorUtils;

public class PermissionManager {

    private final LuckPerms luckPermsApi;

    public PermissionManager(LuckPerms luckPermsApi) {
        this.luckPermsApi = luckPermsApi;
    }

    public String getPlayerPrefix(Player player) {
        if (luckPermsApi == null) {
            return "";
        }

        CachedMetaData meta = luckPermsApi.getPlayerAdapter(Player.class).getMetaData(player);
        String prefix = meta.getPrefix();
        return (prefix != null) ? ColorUtils.colorize(prefix) : "";
    }

    public String getPlayerSuffix(Player player) {
        if (luckPermsApi == null) {
            return "";
        }

        CachedMetaData meta = luckPermsApi.getPlayerAdapter(Player.class).getMetaData(player);
        String suffix = meta.getSuffix();

        return (suffix != null) ? ColorUtils.colorize(suffix) : "";
    }

    public boolean isLuckPermsAvailable() {
        return luckPermsApi != null;
    }

    public LuckPerms getLuckPermsApi() {
        return luckPermsApi;
    }
}