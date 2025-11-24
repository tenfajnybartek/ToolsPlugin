package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.command.CommandSender;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import pl.tenfajnybartek.toolsplugin.base.ToolsPlugin;
import pl.tenfajnybartek.toolsplugin.managers.CooldownManager;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

public class RepairCommand extends BaseCommand {

    public RepairCommand() {
        super("repair", "Naprawia trzymany przedmiot lub cały ekwipunek", "/repair [all]", "tools.cmd.repair", new String[]{"napraw"});
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!isPlayer(sender)) {
            sendOnlyPlayer(sender);
            return true;
        }

        Player player = getPlayer(sender);

        if (args.length == 1 && args[0].equalsIgnoreCase("all")) {

            if (!sender.hasPermission(perm("all"))) {
                sendNoPermission(sender);
                return true;
            }

            int repairedCount = repairInventory(player.getInventory());

            if (repairedCount > 0) {
                sendMessage(sender, "&aPomyślnie naprawiono &e" + repairedCount + " &aprzedmiotów w Twoim ekwipunku!");
            } else {
                sendMessage(sender, "&cNie znaleziono przedmiotów do naprawy.");
            }

            return true;

        } else if (args.length == 0) {


            ItemStack item = player.getInventory().getItemInMainHand();

            if (item == null || item.getType().isAir()) {
                sendMessage(sender, "&cMusisz trzymać przedmiot do naprawy w ręce.");
                return true;
            }

            if (repairItem(item)) {
                sendMessage(sender, "&aPomyślnie naprawiono &e" + item.getType().name() + "&a!");
            } else {
                sendMessage(sender, "&cTen przedmiot nie wymaga naprawy lub nie można go naprawić.");
                return true;
            }
            return true;

        } else {
            sendUsage(sender);
            return true;
        }
    }

    private boolean repairItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;

        // Zapewnienie, że item ma metadane
        if (!item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();

        if (meta instanceof Damageable) {
            Damageable damageable = (Damageable) meta;
            if (damageable.getDamage() > 0) {
                damageable.setDamage(0);
                item.setItemMeta(meta);
                return true;
            }
        }

        return false;
    }

    private int repairInventory(PlayerInventory inventory) {
        int repairedCount = 0;
        ItemStack[] allItems = inventory.getContents();
        for (ItemStack item : allItems) {
            if (item != null && !item.getType().isAir()) {
                if (repairItem(item)) {
                    repairedCount++;
                }
            }
        }

        for (ItemStack armor : inventory.getArmorContents()) {
            if (armor != null && !armor.getType().isAir()) {
                if (repairItem(armor)) {
                    repairedCount++;
                }
            }
        }

        return repairedCount;
    }
}
