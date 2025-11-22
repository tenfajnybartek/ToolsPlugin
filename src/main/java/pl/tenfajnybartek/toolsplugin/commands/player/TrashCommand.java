package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;
import pl.tenfajnybartek.toolsplugin.utils.ColorUtils;

public class TrashCommand extends BaseCommand {

    public TrashCommand() {
        super("trash", "Otwiera kosz na śmieci", "/trash", "tfbhc.cmd.trash", new String[]{"kosz", "bin"});
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {

        if (!isPlayer(sender)) {
            sendMessage(sender, "&cTa komenda może być użyta tylko przez gracza!");
            return true;
        }

        if (args.length != 0) {
            sendMessage(sender, "&cUżycie: " + getUsage());
            return true;
        }

        Player player = getPlayer(sender);

        // 1. Tworzenie wirtualnego ekwipunku
        // Tworzymy dużą skrzynię (54 sloty)
        String title = ColorUtils.colorize("&4&lKOSZ NA ŚMIECI");
        Inventory trashInventory = Bukkit.createInventory(null, 54, ColorUtils.toComponent(title));

        // 2. Otwarcie ekwipunku dla gracza
        player.openInventory(trashInventory);

        sendMessage(sender, "&aOtwarto kosz na śmieci. Wszystko, co w nim zostawisz, ZNIKNIE!");

        return true;
    }
}
