package pl.tenfajnybartek.toolsplugin.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private final JavaPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    /**
     * Ładuje config i tworzy domyślny jeśli nie istnieje
     */
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();

        plugin.getLogger().info("Config załadowany pomyślnie!");
    }

    /**
     * Przeładowuje config z pliku
     */
    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        plugin.getLogger().info("Config przeładowany!");
    }

    /**
     * Zapisuje config do pliku
     */
    public void saveConfig() {
        plugin.saveConfig();
    }

    // ==================== GETTERY ====================

    /**
     * Pobiera prefix pluginu
     */
    public String getPrefix() {
        return config.getString("settings.prefix", "&8[&6ToolsPlugin&8] &r");
    }

    /**
     * Pobiera czy cooldowny są włączone
     */
    public boolean isCooldownsEnabled() {
        return config.getBoolean("cooldowns.enabled", true);
    }

    /**
     * Pobiera cooldown dla konkretnej komendy (w sekundach)
     */
    public int getCooldown(String command) {
        return config.getInt("cooldowns.commands." + command, 0);
    }

    /**
     * Pobiera wszystkie cooldowny jako mapę
     */
    public Map<String, Integer> getAllCooldowns() {
        Map<String, Integer> cooldowns = new HashMap<>();

        if (config.getConfigurationSection("cooldowns.commands") != null) {
            for (String key : config.getConfigurationSection("cooldowns.commands").getKeys(false)) {
                cooldowns.put(key, config.getInt("cooldowns.commands." + key));
            }
        }

        return cooldowns;
    }

    /**
     * Pobiera permisję bypass dla cooldownów
     */
    public String getCooldownBypassPermission() {
        return config.getString("permissions.bypass-cooldowns", "tfbhc.bypass.cooldown");
    }

    /**
     * Pobiera czy teleportacje są bezpieczne (sprawdzanie bloku)
     */
    public boolean isSafeTeleportEnabled() {
        return config.getBoolean("teleport.safe-teleport", true);
    }

    /**
     * Pobiera delay teleportacji (w sekundach)
     */
    public int getTeleportDelay() {
        return config.getInt("teleport.delay", 3);
    }

    /**
     * Pobiera czy anulować teleportację przy ruchu
     */
    public boolean cancelTeleportOnMove() {
        return config.getBoolean("teleport.cancel-on-move", true);
    }

    /**
     * Pobiera czy anulować teleportację przy obrażeniach
     */
    public boolean cancelTeleportOnDamage() {
        return config.getBoolean("teleport.cancel-on-damage", true);
    }

    /**
     * Pobiera raw FileConfiguration (dla zaawansowanych)
     */
    public FileConfiguration getConfig() {
        return config;
    }
}
