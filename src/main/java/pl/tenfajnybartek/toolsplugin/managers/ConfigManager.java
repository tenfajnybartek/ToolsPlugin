package pl.tenfajnybartek.toolsplugin.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
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

    // ==================== GETTERY OGÓLNE I CHATU ====================

    /**
     * Pobiera prefix pluginu (dla wiadomości systemowych)
     */
    public String getPrefix() {
        return config.getString("settings.prefix", "&8[&6ToolsPlugin&8] &r");
    }

    public String getDefaultChatFormat() {
        // Zakładamy, że klucz to "chat.default-format"
        return config.getString("chat.default-format", "%prefix%%player_name%%suffix%&f: &7%message%");
    }

    /**
     * Pobiera szablon formatowania wiadomości chatowej.
     * Używane placeholdery: %prefix%, %player_name%, %suffix%, %message%
     */
    public String getChatFormat() {
        // Domyślny format z placeholderami dla LuckPerms/ChatManager
        return config.getString("chat.format", "&f[%prefix%%player_name%%suffix%&f]: &7%message%");
    }
    public Map<String, String> getCustomChatFormats() {
        Map<String, String> formats = new HashMap<>();

        // Zakładamy, że formaty są zdefiniowane w sekcji 'chat.custom-formats'
        if (config.getConfigurationSection("chat.custom-formats") != null) {
            for (String key : config.getConfigurationSection("chat.custom-formats").getKeys(false)) {
                // Key to np. "admin" lub "vip"
                String permission = config.getString("chat.custom-formats." + key + ".permission");
                String format = config.getString("chat.custom-formats." + key + ".format");

                if (permission != null && format != null) {
                    // Przechowujemy w mapie: "tfbhc.chat.format.admin" -> "&c[ADMIN] %player_name%: %message%"
                    formats.put(permission, format);
                }
            }
        }
        return formats;
    }


    // ==================== GETTERY COOLDOWNÓW ====================

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

    // ==================== ZAPIS/ODCZYT SPAWNU ====================

    public void setSpawnLocation(Location location) {
        config.set("spawn.world", location.getWorld().getName());
        config.set("spawn.x", location.getX());
        config.set("spawn.y", location.getY());
        config.set("spawn.z", location.getZ());
        config.set("spawn.yaw", location.getYaw());
        config.set("spawn.pitch", location.getPitch());

        saveConfig(); // Zapisz zmiany do pliku
        plugin.getLogger().info("Zapisano nową lokalizację Spawna.");
    }

    /**
     * Pobiera lokalizację Spawna z config.yml
     */
    public Location getSpawnLocation() {
        String worldName = config.getString("spawn.world");

        if (worldName == null || Bukkit.getWorld(worldName) == null) {
            return null; // Brak zapisanego świata lub świat nie jest załadowany
        }

        double x = config.getDouble("spawn.x");
        double y = config.getDouble("spawn.y");
        double z = config.getDouble("spawn.z");
        float yaw = (float) config.getDouble("spawn.yaw");
        float pitch = (float) config.getDouble("spawn.pitch");

        return new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
    }

    // ==================== GETTERY TELEPORTACJI ====================

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
