package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GiveCommand extends BaseCommand {

    public GiveCommand() {
        super("give", "Daje graczowi przedmiot", "/give <gracz> <item> [ilość]", "tools.cmd.give", null);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {

        // Komenda wymaga minimum 2 argumentów: gracz i przedmiot
        if (args.length < 2 || args.length > 3) {
            sendUsage(sender);
            return true;
        }

        // 1. Walidacja gracza docelowego
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sendPlayerOffline(sender, args[0]);
            return true;
        }

        // 2. Walidacja przedmiotu
        Material material;
        try {
            // Próba odczytania Material (ignorowanie wielkości liter i spacji)
            material = Material.valueOf(args[1].toUpperCase().replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            sendMessage(sender, "&cNieprawidłowa nazwa przedmiotu: &e" + args[1] + ".");
            return true;
        }

        // 3. Walidacja ilości
        int amount = 1;
        if (args.length == 3) {
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sendMessage(sender, "&cNieprawidłowa ilość. Oczekiwano liczby.");
                return true;
            }
            // Zabezpieczenie przed zbyt dużą lub ujemną ilością
            if (amount <= 0 || amount > material.getMaxStackSize() * 64) {
                sendMessage(sender, "&cNieprawidłowa ilość. Podaj liczbę z zakresu 1-" + material.getMaxStackSize() * 64 + ".");
                return true;
            }
        }

        // 4. Utworzenie i dodanie przedmiotu
        ItemStack item = new ItemStack(material, amount);

        // Dodaj przedmioty, a te, które się nie zmieściły, odrzuć na ziemię
        target.getInventory().addItem(item).forEach((index, excessItem) -> {
            target.getWorld().dropItemNaturally(target.getLocation(), excessItem);
        });

        // 5. Wysyłanie wiadomości zwrotnych
        String itemDisplayName = material.name().toLowerCase().replace('_', ' ');

        sendMessage(sender, String.format("&aDałeś &e%d &ax &e%s &agraczowi &e%s&a.", amount, itemDisplayName, target.getName()));

        if (!target.equals(sender)) {
            sendMessage(target, String.format("&aOtrzymałeś &e%d &ax &e%s &aod administratora.", amount, itemDisplayName));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // Podpowiadanie graczy
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            // Podpowiadanie przedmiotów (filtrowanie po Material)
            return Arrays.stream(Material.values())
                    .map(Enum::name)
                    .filter(name -> name.startsWith(args[1].toUpperCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 3) {
            // Podpowiadanie ilości
            return Arrays.asList("1", "16", "32", "64").stream()
                    .filter(s -> s.startsWith(args[2]))
                    .collect(Collectors.toList());
        }
        return super.tabComplete(sender, args);
    }
}
