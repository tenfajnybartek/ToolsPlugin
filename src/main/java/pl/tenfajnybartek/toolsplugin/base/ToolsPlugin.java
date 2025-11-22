package pl.tenfajnybartek.toolsplugin.base;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import pl.tenfajnybartek.toolsplugin.commands.player.*;
import pl.tenfajnybartek.toolsplugin.listeners.ChatListener;
import pl.tenfajnybartek.toolsplugin.managers.*;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

public class ToolsPlugin extends JavaPlugin {

    private static ToolsPlugin instance;
    private CommandManager commandManager;
    private ChatManager chatManager;
    private ConfigManager configManager;
    private CooldownManager cooldownManager;
    private TeleportManager teleportManager;
    private HomeManager homeManager;
    private WarpManager warpManager;

    @Override
    public void onEnable() {
        instance = this;

        configManager = new ConfigManager(this);
        cooldownManager = new CooldownManager(configManager);
        teleportManager = new TeleportManager(this, configManager);
        homeManager = new HomeManager(this, configManager);
        warpManager = new WarpManager(this);
        commandManager = new CommandManager(this);
        chatManager = new ChatManager(this);

        registerCommands();
        registerListeners();
        startCooldownCleanupTask();
        getLogger().info("ToolsPlugin został włączony!");
    }
    private void startCooldownCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                cooldownManager.cleanupExpiredCooldowns();
            }
        }.runTaskTimer(this, 6000L, 6000L); // Co 5 minut (6000 ticków)
    }

    private void registerCommands() {
        // Podstawowe komendy
        registerCommand(new GameModeCommand());
        registerCommand(new TeleportCommand());
        registerCommand(new HealCommand());
        registerCommand(new FeedCommand());
        registerCommand(new FlyCommand());
        registerCommand(new GodCommand());
        registerCommand(new SpeedCommand());
        registerCommand(new BackCommand());
        registerCommand(new SetWarpCommand());
        registerCommand(new WarpCommand());
        registerCommand(new DelWarpCommand());
        registerCommand(new SetHomeCommand());
        registerCommand(new HomeCommand());
        registerCommand(new DelHomeCommand());
        registerCommand(new ClearInventoryCommand());
        registerCommand(new EnderChestCommand());
        registerCommand(new ClearEnderChestCommand());
        registerCommand(new OpenInventoryCommand());

        registerCommand(new TimeCommand());
        registerCommand(new WeatherCommand());
        registerCommand(new DayCommand());
        registerCommand(new NightCommand());

        registerCommand(new GiveCommand());
        registerCommand(new ItemGiveCommand());
        registerCommand(new RepairCommand());
        registerCommand(new EnchantCommand());
        registerCommand(new HatCommand());

        registerCommand(new KillCommand());
        registerCommand(new SuicideCommand());
        registerCommand(new KickCommand());
        registerCommand(new KickAllCommand());
        registerCommand(new BurnCommand());
        registerCommand(new SmiteCommand());
        registerCommand(new ToolsAdminCommand());

        registerCommand(new BroadCastCommand());
        registerCommand(new WorkbenchCommand());
        registerCommand(new AnvilCommand());
        registerCommand(new TrashCommand());
        registerCommand(new TopCommand());
        registerCommand(new ListCommand());
        registerCommand(new WhoisCommand());
        registerCommand(new TPSCommand());
        registerCommand(new ChatCommand(chatManager));
    }


    private void registerListeners() {
        registerListener(new ChatListener(chatManager));
    }

    public static void registerListener(Listener listener) {
        if (instance != null) {
            Bukkit.getPluginManager().registerEvents(listener, instance);
            instance.getLogger().info("Zarejestrowano Listenera: " + listener.getClass().getSimpleName());
        } else {
            Bukkit.getLogger().severe("Nie można zarejestrować listenera! ToolsPlugin nie jest zainicjalizowany.");
        }
    }


    public static void registerCommand(BaseCommand command) {
        CommandManager.register(command);
    }

    public static ToolsPlugin getInstance() {
        return instance;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public ChatManager getChatManager() {
        return chatManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public HomeManager getHomeManager() {
        return homeManager;
    }

    public WarpManager getWarpManager() {
        return warpManager;
    }

    public TeleportManager getTeleportManager() {
        return teleportManager;
    }

    @Override
    public void onDisable() {
        getLogger().info("ToolsPlugin został wyłączony!");
    }
}
