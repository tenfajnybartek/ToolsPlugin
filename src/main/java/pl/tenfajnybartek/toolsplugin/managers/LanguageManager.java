package pl.tenfajnybartek.toolsplugin.managers;

import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import pl.tenfajnybartek.toolsplugin.utils.ColorUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class LanguageManager {

    private final JavaPlugin plugin;
    private final Logger logger;
    private final File langFile;
    private YamlConfiguration yaml;
    private final Map<String, String> cache = new HashMap<>();

    private static final int CURRENT_VERSION = 1;

    public LanguageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.langFile = new File(plugin.getDataFolder(), "lang.yml");
        loadOrCreate();
    }

    private void loadOrCreate() {
        if (!langFile.exists()) {
            plugin.saveResource("lang.yml", false); // kopiuj z resources
            logger.info("Utworzono domyślny plik lang.yml.");
        }
        yaml = YamlConfiguration.loadConfiguration(langFile);
        ensureVersionAndDefaults();
        buildCache();
    }

    private void ensureVersionAndDefaults() {
        int version = yaml.getInt("lang-version", -1);
        if (version == -1) {
            yaml.set("lang-version", CURRENT_VERSION);
            logger.warning("Brak lang-version – ustawiono " + CURRENT_VERSION);
            saveSilent();
        } else if (version < CURRENT_VERSION) {
            // tu możesz w przyszłości dopisać migracje
            yaml.set("lang-version", CURRENT_VERSION);
            logger.info("Zaktualizowano lang-version do: " + CURRENT_VERSION);
            saveSilent();
        }
    }

    private void buildCache() {
        cache.clear();
        // Przechodzimy po wszystkich ścieżkach
        for (String key : getAllKeys("")) {
            cache.put(key, yaml.getString(key, ""));
        }
        logger.info("Załadowano " + cache.size() + " wpisów językowych.");
    }

    private List<String> getAllKeys(String path) {
        List<String> result = new ArrayList<>();
        if (path.isEmpty()) {
            for (String top : yaml.getKeys(false)) {
                if ("lang-version".equals(top)) continue;
                result.addAll(getAllKeys(top));
            }
        } else {
            if (yaml.isConfigurationSection(path)) {
                for (String child : yaml.getConfigurationSection(path).getKeys(false)) {
                    result.addAll(getAllKeys(path + "." + child));
                }
            } else {
                result.add(path);
            }
        }
        return result;
    }

    public void reload() {
        try {
            yaml = YamlConfiguration.loadConfiguration(langFile);
            buildCache();
            logger.info("Przeładowano lang.yml.");
        } catch (Exception e) {
            logger.severe("Błąd przy przeładowaniu lang.yml: " + e.getMessage());
        }
    }

    public String raw(String key) {
        String val = cache.get(key);
        if (val == null) {
            val = addMissingKey(key);
        }
        return val;
    }

    public String get(String key) {
        return colorize(raw(key));
    }

    public String get(String key, Map<String, String> placeholders) {
        String base = raw(key);
        if (base == null) {
            base = addMissingKey(key);
        }
        base = applyPlaceholders(base, placeholders);
        return colorize(base);
    }

    public Component component(String key, Map<String, String> placeholders) {
        return ColorUtils.toComponent(get(key, placeholders));
    }

    public Component component(String key) {
        return ColorUtils.toComponent(get(key));
    }

    private String applyPlaceholders(String text, Map<String, String> placeholders) {
        if (placeholders == null || placeholders.isEmpty()) return text;
        String out = text;
        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            out = out.replace("{" + e.getKey() + "}", e.getValue());
        }
        return out;
    }

    private String colorize(String text) {
        return ColorUtils.colorize(text);
    }

    private String addMissingKey(String key) {
        String placeholder = "[" + key + "]";
        yaml.set(key, placeholder + " # ADDED AUTO");
        cache.put(key, placeholder);
        saveSilent();
        logger.warning("Brak klucza lang: " + key + " – dodano automatycznie.");
        return placeholder;
    }

    private void saveSilent() {
        try {
            yaml.save(langFile);
        } catch (IOException e) {
            logger.severe("Nie można zapisać lang.yml: " + e.getMessage());
        }
    }
}