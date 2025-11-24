package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

public class HatCommand extends BaseCommand {

    public HatCommand() {
        super("hat", "Zakłada trzymany przedmiot na głowę", "/hat", "tools.cmd.hat", new String[]{"czapka"});
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
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        ItemStack helmet = player.getInventory().getHelmet();

        // 1. Sprawdzenie, czy gracz trzyma cokolwiek w ręce
        if (itemInHand == null || itemInHand.getType() == Material.AIR) {
            sendMessage(sender, "&cMusisz trzymać w ręce przedmiot, który chcesz założyć na głowę.");
            return true;
        }

        // 2. Wymiana przedmiotów

        // Ustawienie przedmiotu z ręki na głowie
        player.getInventory().setHelmet(itemInHand);

        // Ustawienie poprzedniego hełmu (lub powietrza) w ręce
        player.getInventory().setItemInMainHand(helmet);

        // 3. Wysyłanie wiadomości zwrotnej
        if (helmet == null || helmet.getType() == Material.AIR) {
            sendMessage(sender, "&aPomyślnie założyłeś &e" + itemInHand.getType().name() + " &ana głowę!");
        } else {
            sendMessage(sender, "&aPomyślnie zamieniłeś przedmiot w ręce z tym na głowie!");
        }

        player.updateInventory();

        return true;
    }
}
