package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

public class AnvilCommand extends BaseCommand {

    public AnvilCommand() {
        super("anvil", "Otwiera wirtualne kowadło", "/anvil", "tfbhc.cmd.anvil", new String[]{"kowadlo"});
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

        Inventory anvilInventory = Bukkit.createInventory(null, InventoryType.ANVIL);
        player.openInventory(anvilInventory);
        sendMessage(sender, "&aOtwarto wirtualne kowadło!");

        return true;
    }
}
