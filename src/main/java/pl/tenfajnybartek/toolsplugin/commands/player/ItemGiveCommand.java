package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ItemGiveCommand extends BaseCommand {

    public ItemGiveCommand() {
        super("itemgive", "Daje graczowi przedmioty (tylko dla siebie)", "/itemgive <przedmiot> <ilość>", "tools.cmd.itemgive", new String[]{"igive"});
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {

        if (!isPlayer(sender)) {
            sendOnlyPlayer(sender);
            return true;
        }

        if (args.length != 2) {
            sendUsage(sender);
            return true;
        }

        Player player = getPlayer(sender);
        String materialName = args[0].toUpperCase();

        Material material;
        try {
            material = Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            sendMessage(sender, "&cNieprawidłowa nazwa przedmiotu: &e" + args[0] + ".");
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sendMessage(sender, "&cNieprawidłowa ilość. Oczekiwano liczby całkowitej.");
            return true;
        }

        if (amount <= 0 || amount > 64) {
            sendMessage(sender, "&cIlość musi być w zakresie od 1 do 64.");
            return true;
        }

        ItemStack item = new ItemStack(material, amount);

        player.getInventory().addItem(item).forEach((index, excessItem) -> {
            player.getWorld().dropItemNaturally(player.getLocation(), excessItem);
        });

        sendMessage(sender, String.format("&aOtrzymałeś &e%d x %s&a.", amount, material.name()));

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.stream(Material.values())
                    .map(Enum::name)
                    .filter(name -> name.startsWith(args[0].toUpperCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            return Arrays.asList("1", "16", "32", "64").stream()
                    .filter(s -> s.startsWith(args[1]))
                    .collect(Collectors.toList());
        }
        return super.tabComplete(sender, args);
    }
}
