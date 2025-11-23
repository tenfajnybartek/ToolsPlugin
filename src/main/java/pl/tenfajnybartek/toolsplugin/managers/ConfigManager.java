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

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
        plugin.getLogger().info("Config załadowany pomyślnie!");
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        plugin.getLogger().info("Config przeładowany!");
    }

    public void saveConfig() {
        plugin.saveConfig();
    }

    public boolean isPrefixEnabled() {
        return config.getBoolean("settings.prefix-enabled", true);
    }

    public String getPrefix() {
        if (!isPrefixEnabled()) {
            return ""; // prefix wyłączony
        }
        return config.getString("settings.prefix", "&8[&6ToolsPlugin&8] &r");
    }

    public String getDefaultChatFormat() {
        return config.getString("chat.default-format", "%prefix%%player_name%%suffix%&f: &7%message%");
    }

    public String getChatFormat() {
        return config.getString("chat.format", "&f[%prefix%%player_name%%suffix%&f]: &7%message%");
    }

    public Map<String, String> getCustomChatFormats() {
        Map<String, String> formats = new HashMap<>();
        if (config.getConfigurationSection("chat.custom-formats") != null) {
            for (String key : config.getConfigurationSection("chat.custom-formats").getKeys(false)) {
                String permission = config.getString("chat.custom-formats." + key + ".permission");
                String format = config.getString("chat.custom-formats." + key + ".format");
                if (permission != null && format != null) {
                    formats.put(permission, format);
                }
            }
        }
        return formats;
    }

    public boolean isCooldownsEnabled() {
        return config.getBoolean("cooldowns.enabled", true);
    }

    public int getCooldown(String command) {
        return config.getInt("cooldowns.commands." + command, 0);
    }

    public Map<String, Integer> getAllCooldowns() {
        Map<String, Integer> cooldowns = new HashMap<>();
        if (config.getConfigurationSection("cooldowns.commands") != null) {
            for (String key : config.getConfigurationSection("cooldowns.commands").getKeys(false)) {
                cooldowns.put(key, config.getInt("cooldowns.commands." + key));
            }
        }
        return cooldowns;
    }

    public String getCooldownBypassPermission() {
        return config.getString("permissions.bypass-cooldowns", "tfbhc.bypass.cooldown");
    }

    public void setSpawnLocation(Location location) {
        config.set("spawn.world", location.getWorld().getName());
        config.set("spawn.x", location.getX());
        config.set("spawn.y", location.getY());
        config.set("spawn.z", location.getZ());
        config.set("spawn.yaw", location.getYaw());
        config.set("spawn.pitch", location.getPitch());
        saveConfig();
        plugin.getLogger().info("Zapisano nową lokalizację Spawna.");
    }

    public Location getSpawnLocation() {
        String worldName = config.getString("spawn.world");
        if (worldName == null || Bukkit.getWorld(worldName) == null) return null;
        double x = config.getDouble("spawn.x");
        double y = config.getDouble("spawn.y");
        double z = config.getDouble("spawn.z");
        float yaw = (float) config.getDouble("spawn.yaw");
        float pitch = (float) config.getDouble("spawn.pitch");
        return new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
    }

    public boolean isSafeTeleportEnabled() {
        return config.getBoolean("teleport.safe-teleport", true);
    }

    public int getTeleportDelay() {
        return config.getInt("teleport.delay", 3);
    }

    public boolean cancelTeleportOnMove() {
        return config.getBoolean("teleport.cancel-on-move", true);
    }

    public boolean cancelTeleportOnDamage() {
        return config.getBoolean("teleport.cancel-on-damage", true);
    }

    public boolean isDebugEnabled() {
        return config.getBoolean("settings.debug", false);
    }

    public String getDatabaseType() {
        return config.getString("database.type", "mysql");
    }

    public String getDatabaseHost() { return config.getString("database.host", "localhost"); }
    public int getDatabasePort() { return config.getInt("database.port", 3306); }
    public String getDatabaseName() { return config.getString("database.database", "toolsplugin"); }
    public String getDatabaseUser() { return config.getString("database.username", "root"); }
    public String getDatabasePassword() { return config.getString("database.password", ""); }

    public FileConfiguration getConfig() {
        return config;
    }
}
