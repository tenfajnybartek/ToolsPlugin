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
        super("itemgive", "Daje graczowi przedmioty (tylko dla siebie)", "/itemgive <przedmiot> <ilość>", "tfbhc.cmd.itemgive", new String[]{"igive"});
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {

        if (!isPlayer(sender)) {
            sendMessage(sender, "&cTa komenda może być użyta tylko przez gracza!");
            return true;
        }

        if (args.length != 2) {
            sendMessage(sender, "&cUżycie: " + getUsage());
            return true;
        }

        Player player = getPlayer(sender);
        String materialName = args[0].toUpperCase();

        // 1. Walidacja nazwy przedmiotu
        Material material;
        try {
            material = Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            sendMessage(sender, "&cNieprawidłowa nazwa przedmiotu: &e" + args[0] + ".");
            return true;
        }

        // 2. Walidacja ilości
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

        // 3. Dodanie przedmiotu
        ItemStack item = new ItemStack(material, amount);

        // Dodanie do ekwipunku z obsługą przepełnienia (drop na ziemię)
        player.getInventory().addItem(item).forEach((index, excessItem) -> {
            player.getWorld().dropItemNaturally(player.getLocation(), excessItem);
        });

        // 4. Wysyłanie wiadomości zwrotnych
        sendMessage(sender, String.format("&aOtrzymałeś &e%d x %s&a.", amount, material.name()));

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // Podpowiadanie nazw przedmiotów (filtracja)
            return Arrays.stream(Material.values())
                    .map(Enum::name)
                    .filter(name -> name.startsWith(args[0].toUpperCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            // Podpowiadanie ilości
            return Arrays.asList("1", "32", "64").stream()
                    .filter(s -> s.startsWith(args[1]))
                    .collect(Collectors.toList());
        }
        return super.tabComplete(sender, args);
    }
}
