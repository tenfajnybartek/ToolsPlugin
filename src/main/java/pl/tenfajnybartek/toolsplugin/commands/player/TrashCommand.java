package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;
import pl.tenfajnybartek.toolsplugin.utils.ColorUtils;

public class TrashCommand extends BaseCommand {

    public TrashCommand() {
        super("trash", "Otwiera kosz na śmieci", "/trash", "tools.cmd.trash", new String[]{"kosz", "bin"});
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {

        if (!isPlayer(sender)) {
            sendOnlyPlayer(sender);
            return true;
        }

        if (args.length != 0) {
            sendUsage(sender);
            return true;
        }

        Player player = getPlayer(sender);

        String title = ColorUtils.colorize("&4&lKOSZ NA ŚMIECI");
        Inventory trashInventory = Bukkit.createInventory(null, 54, ColorUtils.toComponent(title));

        player.openInventory(trashInventory);

        sendMessage(sender, "&aOtwarto kosz na śmieci. Wszystko, co w nim zostawisz, ZNIKNIE!");

        return true;
    }
}
