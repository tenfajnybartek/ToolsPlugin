package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

public class WorkbenchCommand extends BaseCommand {

    public WorkbenchCommand() {
        super("workbench", "Otwiera wirtualny stół rzemieślniczy", "/workbench", "tfbhc.cmd.workbench", new String[]{"craft", "crafting"});
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
        player.openInventory(Bukkit.createInventory(null, InventoryType.WORKBENCH));

        sendMessage(sender, "&aOtwarto wirtualny stół rzemieślniczy!");

        return true;
    }
}
