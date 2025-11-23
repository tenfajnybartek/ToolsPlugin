package pl.tenfajnybartek.toolsplugin.base;

import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import pl.tenfajnybartek.toolsplugin.commands.player.*;
import pl.tenfajnybartek.toolsplugin.listeners.*;
import pl.tenfajnybartek.toolsplugin.managers.*;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ToolsPlugin extends JavaPlugin {

    private static ToolsPlugin instance;
    private LanguageManager languageManager;
    private PermissionManager permissionManager;
    private ActionBarManager actionBarManager;
    private CommandManager commandManager;
    private LuckPerms luckPermsApi = null;
    private ChatManager chatManager;
    private ConfigManager configManager;
    private CooldownManager cooldownManager;
    private TeleportManager teleportManager;
    private HomeManager homeManager;
    private WarpManager warpManager;
    private MuteManager muteManager;
    private ExecutorService asyncTaskExecutor;
    private BanManager banManager;
    private DatabaseManager databaseManager;
    private UserManager userManager;
    private HelpopManager helpopManager;
    private MessageManager messageManager;
    private VanishManager vanishManager;

    @Override
    public void onEnable() {
        instance = this;
        if (!setupPermissions()) {
            getLogger().severe("Nie udało się zainicjować API Vault lub LuckPerms. Wylaczam plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        configManager = new ConfigManager(this);
        languageManager = new LanguageManager(this);
        actionBarManager = new ActionBarManager(
                this,
                10,          // 10 ticków = 0.5 s
                true,        // show multiple ephemeral
                3,           // max ephemeral
                false,       // resendAlways (nie trzeba, bo pinned vanish wymusza send)
                40,          // persistent heartbeat (gdy ephemeral obecne)
                10,          // no ephemeral heartbeat (gdy brak ephemeral)
                " | "        // separator
        );
        actionBarManager.start();
        cooldownManager = new CooldownManager(configManager);
        databaseManager = new DatabaseManager(this);
        this.permissionManager = new PermissionManager(this.getLuckPermsApi());
        this.asyncTaskExecutor = Executors.newFixedThreadPool(4);
        userManager = new UserManager(this, databaseManager);
        teleportManager = new TeleportManager(this, configManager, userManager, actionBarManager);
        banManager = new BanManager(this, databaseManager);
        muteManager = new MuteManager(this, databaseManager);
        homeManager = new HomeManager(this, configManager, databaseManager);
        warpManager = new WarpManager(this, databaseManager);
        commandManager = new CommandManager(this);
        chatManager = new ChatManager(this, configManager, permissionManager);
        messageManager = new MessageManager(this, userManager);
        helpopManager = new HelpopManager(this, configManager);
        vanishManager = new VanishManager(this, actionBarManager);
        registerCommands();
        registerListeners();
        startCooldownCleanupTask();
        getLogger().info("ToolsPlugin został włączony!");
    }

    private boolean setupPermissions() {
        // --- Inicjalizacja LuckPerms (tylko to zostaje) ---

        RegisteredServiceProvider<LuckPerms> providerLP = getServer().getServicesManager().getRegistration(LuckPerms.class);
        if (providerLP != null) {
            this.luckPermsApi = providerLP.getProvider();
            getLogger().info("Pomyślnie zaladowano LuckPerms API.");
            return true;
        } else {
            getLogger().severe("LuckPerms API nie zostalo znalezione. Jest wymagane do chatu.");
            return false;
        }
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
        registerCommand(new MsgCommand());
        registerCommand(new ReplyCommand());
        registerCommand(new MsgToggleCommand());
        registerCommand(new SocialSpyCommand());
        registerCommand(new TpaCommand());
        registerCommand(new TpAcceptCommand());
        registerCommand(new TpDenyCommand());
        registerCommand(new TpaToggleCommand());
        registerCommand(new BanCommand());
        registerCommand(new UnBanCommand());
        registerCommand(new BanInfoCommand());
        registerCommand(new MuteCommand());
        registerCommand(new UnMuteCommand());
        registerCommand(new SetSpawnCommand());
        registerCommand(new SpawnCommand());
        registerCommand(new HelpopCommand());
        registerCommand(new VanishCommand());
    }


    private void registerListeners() {
        registerListener(new ChatListener(chatManager));
        registerListener(new TeleportListener(teleportManager, configManager));
        registerListener(new UserListener(userManager, homeManager));
        registerListener(new BanListener(banManager));
        registerListener(new MuteListener(muteManager, this));
        registerListener(new VanishListener(vanishManager));
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

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public UserManager getUserManager() {
        return userManager;
    }
    public MessageManager getMessageManager() { // NOWY GETTER
        return messageManager;
    }
    public BanManager getBanManager() {
        return banManager;
    }

    public LuckPerms getLuckPermsApi() {
        return luckPermsApi;
    }
    public static ExecutorService getExecutor() {
        // Musimy użyć statycznej instancji, aby uzyskać dostęp do pola asyncTaskExecutor
        if (instance == null || instance.asyncTaskExecutor == null) {
            throw new IllegalStateException("Executor Service not initialized!");
        }
        return instance.asyncTaskExecutor;
    }
    public MuteManager getMuteManager() { // NOWY GETTER
        return muteManager;
    }
    public PermissionManager getPermissionManager() {
        return permissionManager;
    }
    public HelpopManager getHelpopManager() {
        return helpopManager;
    }
    public LanguageManager getLanguageManager() { return languageManager; }
    public VanishManager getVanishManager() { return vanishManager; }
    public ActionBarManager getActionBarManager() { return actionBarManager; }
    @Override
    public void onDisable() {

        if (userManager != null) {
            userManager.saveAllSyncOnShutdown();
        }
        if (actionBarManager != null) {

        }
        if (vanishManager != null) {
            vanishManager.clearAll();
        }

        if (this.asyncTaskExecutor != null && !this.asyncTaskExecutor.isShutdown()) {
            this.asyncTaskExecutor.shutdownNow();
        }

        // 4. Zamknięcie połączeń z bazą
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
    }
}
