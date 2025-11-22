package pl.tenfajnybartek.toolsplugin.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class HomeManager {
    private final ToolsPlugin plugin;
    private final ConfigManager configManager;
    private final File homesFile;
    private FileConfiguration homesConfig;

    // Mapa home'ów: UUID gracza -> (nazwa home -> lokalizacja)
    private final Map<UUID, Map<String, Location>> homes;

    public HomeManager(ToolsPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.homesFile = new File(plugin.getDataFolder(), "homes.yml");
        this.homes = new HashMap<>();

        loadHomes();
    }

    /**
     * Ładuje home'y z pliku
     */
    public void loadHomes() {
        if (!homesFile.exists()) {
            try {
                homesFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Nie można utworzyć pliku homes.yml!");
                e.printStackTrace();
            }
        }

        homesConfig = YamlConfiguration.loadConfiguration(homesFile);
        homes.clear();

        if (homesConfig.getConfigurationSection("homes") == null) {
            return;
        }

        for (String uuidString : homesConfig.getConfigurationSection("homes").getKeys(false)) {
            UUID uuid = UUID.fromString(uuidString);
            Map<String, Location> playerHomes = new HashMap<>();

            String basePath = "homes." + uuidString;

            if (homesConfig.getConfigurationSection(basePath) == null) {
                continue;
            }

            for (String homeName : homesConfig.getConfigurationSection(basePath).getKeys(false)) {
                String path = basePath + "." + homeName;

                String worldName = homesConfig.getString(path + ".world");
                double x = homesConfig.getDouble(path + ".x");
                double y = homesConfig.getDouble(path + ".y");
                double z = homesConfig.getDouble(path + ".z");
                float yaw = (float) homesConfig.getDouble(path + ".yaw");
                float pitch = (float) homesConfig.getDouble(path + ".pitch");

                if (worldName == null || Bukkit.getWorld(worldName) == null) {
                    plugin.getLogger().warning("Nie można załadować home '" + homeName + "' dla gracza " + uuidString + " - świat nie istnieje!");
                    continue;
                }

                Location location = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
                playerHomes.put(homeName.toLowerCase(), location);
            }

            homes.put(uuid, playerHomes);
        }

        plugin.getLogger().info("Załadowano home'y dla " + homes.size() + " graczy!");
    }

    /**
     * Zapisuje home'y do pliku
     */
    public void saveHomes() {
        try {
            homesConfig.save(homesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Nie można zapisać home'ów!");
            e.printStackTrace();
        }
    }

    /**
     * Tworzy nowy home dla gracza
     */
    public boolean createHome(Player player, String name, Location location) {
        UUID uuid = player.getUniqueId();
        String lowerName = name.toLowerCase();

        // Sprawdź limit home'ów
        int maxHomes = getMaxHomes(player);
        int currentHomes = getHomeCount(player);

        if (currentHomes >= maxHomes && !hasHome(player, name)) {
            return false; // Limit osiągnięty
        }

        // Pobierz lub utwórz mapę home'ów gracza
        Map<String, Location> playerHomes = homes.getOrDefault(uuid, new HashMap<>());

        String path = "homes." + uuid.toString() + "." + lowerName;
        homesConfig.set(path + ".world", location.getWorld().getName());
        homesConfig.set(path + ".x", location.getX());
        homesConfig.set(path + ".y", location.getY());
        homesConfig.set(path + ".z", location.getZ());
        homesConfig.set(path + ".yaw", location.getYaw());
        homesConfig.set(path + ".pitch", location.getPitch());

        playerHomes.put(lowerName, location);
        homes.put(uuid, playerHomes);
        saveHomes();

        return true;
    }

    /**
     * Usuwa home gracza
     */
    public boolean deleteHome(Player player, String name) {
        UUID uuid = player.getUniqueId();
        String lowerName = name.toLowerCase();

        if (!homes.containsKey(uuid) || !homes.get(uuid).containsKey(lowerName)) {
            return false; // Home nie istnieje
        }

        homesConfig.set("homes." + uuid.toString() + "." + lowerName, null);
        homes.get(uuid).remove(lowerName);

        // Usuń całą mapę gracza jeśli pusta
        if (homes.get(uuid).isEmpty()) {
            homes.remove(uuid);
            homesConfig.set("homes." + uuid.toString(), null);
        }

        saveHomes();
        return true;
    }

    /**
     * Pobiera lokalizację home gracza
     */
    public Location getHome(Player player, String name) {
        UUID uuid = player.getUniqueId();
        String lowerName = name.toLowerCase();

        if (!homes.containsKey(uuid)) {
            return null;
        }

        return homes.get(uuid).get(lowerName);
    }

    /**
     * Sprawdza czy gracz ma home o danej nazwie
     */
    public boolean hasHome(Player player, String name) {
        UUID uuid = player.getUniqueId();
        String lowerName = name.toLowerCase();

        return homes.containsKey(uuid) && homes.get(uuid).containsKey(lowerName);
    }

    /**
     * Pobiera listę home'ów gracza
     */
    public Set<String> getHomeNames(Player player) {
        UUID uuid = player.getUniqueId();

        if (!homes.containsKey(uuid)) {
            return new HashSet<>();
        }

        return new HashSet<>(homes.get(uuid).keySet());
    }

    /**
     * Pobiera liczbę home'ów gracza
     */
    public int getHomeCount(Player player) {
        UUID uuid = player.getUniqueId();

        if (!homes.containsKey(uuid)) {
            return 0;
        }

        return homes.get(uuid).size();
    }

    /**
     * Pobiera maksymalną liczbę home'ów dla gracza
     */
    public int getMaxHomes(Player player) {
        // Sprawdź uprawnienia dla rang
        if (player.hasPermission("tfbhc.homes.admin")) {
            return configManager.getConfig().getInt("homes.rank-limits.admin", 20);
        }
        if (player.hasPermission("tfbhc.homes.mvp")) {
            return configManager.getConfig().getInt("homes.rank-limits.mvp", 10);
        }
        if (player.hasPermission("tfbhc.homes.vip")) {
            return configManager.getConfig().getInt("homes.rank-limits.vip", 5);
        }

        // Domyślny limit
        return configManager.getConfig().getInt("homes.max-per-player", 3);
    }
}
