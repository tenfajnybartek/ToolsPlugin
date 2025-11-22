package pl.tenfajnybartek.toolsplugin.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class WarpManager {
    private final ToolsPlugin plugin;
    private final File warpsFile;
    private FileConfiguration warpsConfig;

    // Mapa warpów: nazwa -> lokalizacja
    private final Map<String, Location> warps;

    public WarpManager(ToolsPlugin plugin) {
        this.plugin = plugin;
        this.warpsFile = new File(plugin.getDataFolder(), "warps.yml");
        this.warps = new HashMap<>();

        loadWarps();
    }

    /**
     * Ładuje warpy z pliku
     */
    public void loadWarps() {
        if (!warpsFile.exists()) {
            plugin.saveResource("warps.yml", false);
        }

        warpsConfig = YamlConfiguration.loadConfiguration(warpsFile);
        warps.clear();

        if (warpsConfig.getConfigurationSection("warps") == null) {
            return;
        }

        for (String warpName : warpsConfig.getConfigurationSection("warps").getKeys(false)) {
            String path = "warps." + warpName;

            String worldName = warpsConfig.getString(path + ".world");
            double x = warpsConfig.getDouble(path + ".x");
            double y = warpsConfig.getDouble(path + ".y");
            double z = warpsConfig.getDouble(path + ".z");
            float yaw = (float) warpsConfig.getDouble(path + ".yaw");
            float pitch = (float) warpsConfig.getDouble(path + ".pitch");

            if (worldName == null || Bukkit.getWorld(worldName) == null) {
                plugin.getLogger().warning("Nie można załadować warpa '" + warpName + "' - świat nie istnieje!");
                continue;
            }

            Location location = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
            warps.put(warpName.toLowerCase(), location);
        }

        plugin.getLogger().info("Załadowano " + warps.size() + " warpów!");
    }

    /**
     * Zapisuje warpy do pliku
     */
    public void saveWarps() {
        try {
            warpsConfig.save(warpsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Nie można zapisać warpów!");
            e.printStackTrace();
        }
    }

    /**
     * Tworzy nowy warp
     */
    public boolean createWarp(String name, Location location) {
        String lowerName = name.toLowerCase();

        if (warps.containsKey(lowerName)) {
            return false; // Warp już istnieje
        }

        String path = "warps." + lowerName;
        warpsConfig.set(path + ".world", location.getWorld().getName());
        warpsConfig.set(path + ".x", location.getX());
        warpsConfig.set(path + ".y", location.getY());
        warpsConfig.set(path + ".z", location.getZ());
        warpsConfig.set(path + ".yaw", location.getYaw());
        warpsConfig.set(path + ".pitch", location.getPitch());

        warps.put(lowerName, location);
        saveWarps();

        return true;
    }

    /**
     * Usuwa warp
     */
    public boolean deleteWarp(String name) {
        String lowerName = name.toLowerCase();

        if (!warps.containsKey(lowerName)) {
            return false; // Warp nie istnieje
        }

        warpsConfig.set("warps." + lowerName, null);
        warps.remove(lowerName);
        saveWarps();

        return true;
    }

    /**
     * Pobiera lokalizację warpa
     */
    public Location getWarp(String name) {
        return warps.get(name.toLowerCase());
    }

    /**
     * Sprawdza czy warp istnieje
     */
    public boolean warpExists(String name) {
        return warps.containsKey(name.toLowerCase());
    }

    /**
     * Pobiera listę wszystkich warpów
     */
    public Set<String> getWarpNames() {
        return new HashSet<>(warps.keySet());
    }

    /**
     * Pobiera liczbę warpów
     */
    public int getWarpCount() {
        return warps.size();
    }
}
