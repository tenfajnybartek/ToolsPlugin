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
        super("repair", "Naprawia trzymany przedmiot lub cały ekwipunek", "/repair [all]", "tfbhc.cmd.repair", new String[]{"napraw"});
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!isPlayer(sender)) {
            sendMessage(sender, "&cTa komenda może być użyta tylko przez gracza!");
            return true;
        }

        Player player = getPlayer(sender);
        CooldownManager cooldownManager = ToolsPlugin.getInstance().getCooldownManager();

        // 1. /repair all
        if (args.length == 1 && args[0].equalsIgnoreCase("all")) {

            if (!sender.hasPermission(perm("all"))) {
                sendMessage(sender, "&cNie masz uprawnień do naprawiania całego ekwipunku (&etfbhc.cmd.repair.all&c)!");
                return true;
            }

            // Sprawdź cooldown (używamy tej samej nazwy "repair" dla obu wariantów)
            if (cooldownManager.checkCooldown(player, "repair")) {
                return true; // Cooldown aktywny - blokuj wykonanie
            }

            int repairedCount = repairInventory(player.getInventory());

            if (repairedCount > 0) {
                sendMessage(sender, "&aPomyślnie naprawiono &e" + repairedCount + " &aprzedmiotów w Twoim ekwipunku!");
            } else {
                sendMessage(sender, "&cNie znaleziono przedmiotów do naprawy.");
            }

            // Ustaw cooldown
            cooldownManager.setCooldown(player, "repair");
            return true;

            // 2. /repair - trzymany przedmiot
        } else if (args.length == 0) {

            // Sprawdź cooldown
            if (cooldownManager.checkCooldown(player, "repair")) {
                return true; // Cooldown aktywny - blokuj wykonanie
            }

            ItemStack item = player.getInventory().getItemInMainHand();

            if (item == null || item.getType().isAir()) {
                sendMessage(sender, "&cMusisz trzymać przedmiot do naprawy w ręce.");
                return true;
            }

            if (repairItem(item)) {
                sendMessage(sender, "&aPomyślnie naprawiono &e" + item.getType().name() + "&a!");
            } else {
                sendMessage(sender, "&cTen przedmiot nie wymaga naprawy lub nie można go naprawić.");
                // Nie ustawiamy cooldownu jeśli naprawa się nie powiodła
                return true;
            }

            // Ustaw cooldown tylko jeśli naprawa się powiodła
            cooldownManager.setCooldown(player, "repair");
            return true;

        } else {
            sendMessage(sender, "&cUżycie: " + getUsage());
            return true;
        }
    }

    // --- Metody pomocnicze ---

    /**
     * Próbuje naprawić pojedynczy przedmiot.
     * @return true jeśli przedmiot został naprawiony, false w przeciwnym razie.
     */
    private boolean repairItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;

        // Zapewnienie, że item ma metadane
        if (!item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();

        if (meta instanceof Damageable) {
            Damageable damageable = (Damageable) meta;

            // Sprawdzenie, czy przedmiot jest uszkodzony (Damage > 0)
            if (damageable.getDamage() > 0) {

                // Naprawienie przedmiotu (ustawienie Damage na 0)
                damageable.setDamage(0);

                // Przypisanie zmodyfikowanych metadanych z powrotem do przedmiotu
                item.setItemMeta(meta);
                return true;
            }
        }

        return false;
    }

    /**
     * Naprawia wszystkie przedmioty w ekwipunku (w tym zbroję).
     * @return Liczba naprawionych przedmiotów.
     */
    private int repairInventory(PlayerInventory inventory) {
        int repairedCount = 0;

        // Tablica ze wszystkimi slotami do sprawdzenia (ekwipunek + zbroja)
        ItemStack[] allItems = inventory.getContents();

        for (ItemStack item : allItems) {
            if (item != null && !item.getType().isAir()) {
                if (repairItem(item)) {
                    repairedCount++;
                }
            }
        }

        // Zbroja
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
