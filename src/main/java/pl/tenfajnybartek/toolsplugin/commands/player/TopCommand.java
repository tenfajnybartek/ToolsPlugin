package pl.tenfajnybartek.toolsplugin.commands.player;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.tenfajnybartek.toolsplugin.utils.BaseCommand;

public class TopCommand extends BaseCommand {

    public TopCommand() {
        super("top", "Teleportuje na najwyższy blok na danej pozycji", "/top", "tfbhc.cmd.top", new String[]{"góra"});
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
        Location startLocation = player.getLocation();

        // 1. Znalezienie najwyższego bloku

        // getHighestBlockAt() zwraca najwyższy niepowietrzny blok.
        // Używamy startLocation.getWorld().getHighestBlockAt() aby było bezpieczniej.
        Location safeLocation = startLocation.getWorld().getHighestBlockAt(startLocation.getBlockX(), startLocation.getBlockZ()).getLocation();

        // 2. Korekty lokalizacji do teleportacji

        // Zapewnienie, że teleportacja odbędzie się na blok POWYŻEJ najwyższego bloku,
        // oraz ustawienie X i Z na środek bloku.
        double x = safeLocation.getBlockX() + 0.5;
        double z = safeLocation.getBlockZ() + 0.5;
        double y = safeLocation.getBlockY() + 1; // +1, aby stanąć na bloku, a nie w nim

        // Utworzenie finalnej lokalizacji z zachowaniem kierunku patrzenia gracza (pitch i yaw)
        Location finalLocation = new Location(
                player.getWorld(),
                x,
                y,
                z,
                startLocation.getYaw(),
                startLocation.getPitch()
        );

        // 3. Teleportacja
        // Sprawdzamy, czy blok, na który chcemy teleportować, jest bezpieczny (np. czy to nie lawa)
        if (finalLocation.getBlock().getType() == Material.LAVA || finalLocation.getBlock().getType() == Material.FIRE) {
            sendMessage(sender, "&cBezpieczny blok na powierzchni jest niebezpieczny. Teleportacja anulowana.");
            return true;
        }

        player.teleport(finalLocation);
        sendMessage(sender, "&aTeleportowano na &eGórę&a!");

        return true;
    }
}
