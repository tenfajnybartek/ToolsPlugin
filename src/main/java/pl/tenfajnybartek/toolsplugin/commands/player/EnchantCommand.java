package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public class EnchantCommand extends BaseCommand {

    public EnchantCommand() {
        super("enchant", "Nadaje enchant na trzymany przedmiot", "/enchant <enchant> <level>", "tfbhc.cmd.enchant", new String[]{"zaklinaj"});
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
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType().isAir()) {
            sendMessage(sender, "&cMusisz trzymać przedmiot do enchantowania w ręce.");
            return true;
        }

        // 1. Walidacja enchantu (Używamy NamespacedKey)
        Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(args[0].toLowerCase()));

        if (enchantment == null) {
            sendMessage(sender, "&cNieprawidłowa nazwa enchantu: &e" + args[0] + ".");
            return true;
        }

        // 2. Walidacja poziomu
        int level;
        try {
            level = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sendMessage(sender, "&cNieprawidłowy poziom. Oczekiwano liczby całkowitej.");
            return true;
        }

        if (level <= 0) {
            sendMessage(sender, "&cPoziom musi być większy od zera.");
            return true;
        }

        // 3. Aplikacja enchantu
        try {
            // Dodajemy enchant, ignorując limity poziomu
            item.addUnsafeEnchantment(enchantment, level);
        } catch (IllegalArgumentException e) {
            sendMessage(sender, "&cNie można nałożyć tego enchantu na ten przedmiot.");
            return true;
        }

        // 4. Wysyłanie wiadomości zwrotnych (Używamy getKey().asString())
        String enchantKey = enchantment.getKey().asString();
        // Formatowanie nazwy klucza dla lepszej czytelności w wiadomości
        String displayEnchantName = enchantKey.replace("minecraft:", "").replace('_', ' ');

        sendMessage(sender, String.format("&aPomyślnie nadano enchant &e%s %d &ana &e%s&a.", displayEnchantName, level, item.getType().name()));

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // Podpowiadanie nazw enchantów (używając NamespacedKey)
            return Arrays.stream(Enchantment.values())
                    .map(e -> e.getKey().getKey()) // Bierzemy tylko część po 'minecraft:'
                    .filter(name -> name.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            // Podpowiadanie poziomów
            return Arrays.asList("1", "3", "5", "10").stream()
                    .filter(s -> s.startsWith(args[1]))
                    .collect(Collectors.toList());
        }
        return super.tabComplete(sender, args);
    }
}
