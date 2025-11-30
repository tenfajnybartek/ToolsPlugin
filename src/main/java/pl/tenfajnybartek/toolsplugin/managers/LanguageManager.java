package pl.tenfajnybartek.toolsplugin.managers;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import pl.tenfajnybartek.toolsplugin.utils.ColorUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager {

    private final JavaPlugin plugin;
    private final File langFile;
    private YamlConfiguration yaml;
    private final Map<String,String> cache = new HashMap<>();

    public LanguageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.langFile = new File(plugin.getDataFolder(), "lang.yml");
        load();
    }

    private void load() {
        if (!langFile.exists()) {
            plugin.saveResource("lang.yml", false);
        }
        yaml = YamlConfiguration.loadConfiguration(langFile);
        buildCache();
    }

    public void reload() {
        yaml = YamlConfiguration.loadConfiguration(langFile);
        buildCache();
    }

    private void buildCache() {
        cache.clear();
        if (yaml.getConfigurationSection("core") != null) {
            for (String k : yaml.getConfigurationSection("core").getKeys(false)) {
                cache.put("core." + k, yaml.getString("core." + k, ""));
            }
        }
    }

    private void saveSilent() {
        try { yaml.save(langFile); } catch (IOException ignored) {}
    }

    public String raw(String key) {
        String val = cache.get(key);
        if (val == null) {
            // Automatyczne dopisanie brakującego klucza (fallback)
            yaml.set(key, "[" + key + "] # AUTO");
            cache.put(key, "[" + key + "]");
            saveSilent();
            return "[" + key + "]";
        }
        return val;
    }

    public String get(String key, Map<String,String> placeholders) {
        String base = raw(key);
        if (placeholders != null) {
            for (Map.Entry<String,String> e : placeholders.entrySet()) {
                base = base.replace("{" + e.getKey() + "}", e.getValue());
            }
        }
        return ColorUtils.colorize(base);
    }

    public String get(String key) {
        return get(key, null);
    }

    public String getPrefix() {
        boolean enabled = plugin.getConfig().getBoolean("settings.prefix-enabled", true);
        if (!enabled) return "";
        return ColorUtils.colorize(raw("core.prefix"));
    }

    public String formatUsage(String usage) {
        String msg = raw("core.correct-usage").replace("{USAGE}", usage);
        return ColorUtils.colorize(getPrefix() + msg);
    }
    public String getNoPermission(String permission) {
        String base = raw("core.no-permission").replace("{PERMISSION}", permission);
        return ColorUtils.colorize(getPrefix() + base);
    }

    public String getOnlyPlayer() {
        String base = raw("core.only-player");
        return ColorUtils.colorize(getPrefix() + base);
    }

    public String getPlayerOffline(String playerName) {
        String base = raw("core.player-offline").replace("{PLAYER}", playerName);
        return ColorUtils.colorize(getPrefix() + base);
    }
    public String formatNoPermission(String permission) {
        String base = raw("core.no-permission").replace("{PERMISSION}", permission);
        return ColorUtils.colorize(getPrefix() + base);
    }
}