package pl.tenfajnybartek.toolsplugin.managers;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;
import pl.tenfajnybartek.toolsplugin.utils.ColorUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandManager {
    private static CommandManager instance;
    private final Plugin plugin;
    private final Map<String, BaseCommand> commands;
    private CommandMap commandMap;

    public CommandManager(Plugin plugin) {
        instance = this;
        this.plugin = plugin;
        this.commands = new HashMap<>();
        this.commandMap = getCommandMap();
    }



    /**
     * Rejestruje komendę (metoda instancyjna)
     */
    public void registerCommand(BaseCommand baseCommand) {
        if (commandMap == null) {
            plugin.getLogger().severe("Nie można zarejestrować komendy: CommandMap jest null!");
            return;
        }

        commands.put(baseCommand.getName().toLowerCase(), baseCommand);

        PluginCommand command = new PluginCommand(baseCommand, plugin);
        commandMap.register(plugin.getName().toLowerCase(), command);

        plugin.getLogger().info("Zarejestrowano komendę: /" + baseCommand.getName());
    }



    /**
     * Statyczna metoda do rejestracji komend
     */
    public static void register(BaseCommand baseCommand) {
        if (instance == null) {
            throw new IllegalStateException("CommandManager nie został zainicjalizowany! Upewnij się, że tworzysz instancję w onEnable().");
        }
        instance.registerCommand(baseCommand);
    }


    /**
     * Pobiera instancję CommandManagera
     */
    public static CommandManager getInstance() {
        return instance;
    }

    public BaseCommand getCommand(String name) {
        return commands.get(name.toLowerCase());
    }

    public Map<String, BaseCommand> getCommands() {
        return new HashMap<>(commands);
    }

    private CommandMap getCommandMap() {
        try {
            Field field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            field.setAccessible(true);
            return (CommandMap) field.get(Bukkit.getServer());
        } catch (Exception e) {
            plugin.getLogger().severe("Nie można uzyskać CommandMap!");
            e.printStackTrace();
            return null;
        }
    }

    private static class PluginCommand extends Command {
        private final BaseCommand baseCommand;
        private final Plugin plugin;

        public PluginCommand(BaseCommand baseCommand, Plugin plugin) {
            super(baseCommand.getName());
            this.baseCommand = baseCommand;
            this.plugin = plugin;

            setDescription(baseCommand.getDescription());
            setUsage(baseCommand.getUsage());
            setPermission(baseCommand.getPermission());

            if (baseCommand.getAliases().length > 0) {
                setAliases(List.of(baseCommand.getAliases()));
            }
        }

        @Override
        public boolean execute(CommandSender sender, String label, String[] args) {
            if (baseCommand.getPermission() != null && !baseCommand.getPermission().isEmpty()) {
                if (!sender.hasPermission(baseCommand.getPermission())) {
                    var lm = ToolsPlugin.getInstance().getLanguageManager();
                    sender.sendMessage(lm.formatNoPermission(baseCommand.getPermission()));
                    return true;
                }
            }
            try {
                return baseCommand.execute(sender, args);
            } catch (Exception e) {
                var lm = ToolsPlugin.getInstance().getLanguageManager();
                // Możesz później dodać klucz core.command-error
                sender.sendMessage(ColorUtils.colorize("&cWystąpił błąd podczas wykonywania komendy!"));
                plugin.getLogger().severe("Błąd w komendzie /" + baseCommand.getName() + ":");
                e.printStackTrace();
                return true;
            }
        }

        @Override
        public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
            try {
                return baseCommand.tabComplete(sender, args);
            } catch (Exception e) {
                plugin.getLogger().severe("Błąd w tab complete dla /" + baseCommand.getName() + ":");
                e.printStackTrace();
                return new ArrayList<>();
            }
        }
    }
}
