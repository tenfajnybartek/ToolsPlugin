package pl.tenfajnybartek.toolsplugin.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import pl.tenfajnybartek.toolsplugin.objects.BanRecord;
import pl.tenfajnybartek.toolsplugin.objects.MuteRecord;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeUtils {

    private static final MiniMessage mm = MiniMessage.miniMessage();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // =========================================================================
    // Parsowanie ciągów czasu (1h, 30m, 15d, perm)
    // =========================================================================
    public static LocalDateTime parseTime(String timeString) {
        if (timeString == null || timeString.equalsIgnoreCase("perm") || timeString.equalsIgnoreCase("permanent")) {
            return null;
        }
        long durationSeconds = 0;
        Pattern pattern = Pattern.compile("(\\d+)([a-zA-Z])");
        Matcher matcher = pattern.matcher(timeString);
        while (matcher.find()) {
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2).toLowerCase();
            switch (unit) {
                case "s": durationSeconds += value; break;
                case "m": durationSeconds += value * 60; break;
                case "h": durationSeconds += value * 3600; break;
                case "d": durationSeconds += value * 86400; break;
                case "w": durationSeconds += value * 604800; break;
                case "y": durationSeconds += value * 31536000; break;
                default: return null; // nieznana jednostka => null
            }
        }
        if (durationSeconds <= 0) return null;
        return LocalDateTime.now().plusSeconds(durationSeconds);
    }

    // =========================================================================
    // Formatowanie daty i czasu
    // =========================================================================
    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "N/A" : dateTime.format(DATE_FORMATTER);
    }

    // =========================================================================
    // Formatowanie pozostałego czasu (Duration/LocalDateTime)
    // =========================================================================
    public static String formatDuration(Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) return "Wygasł";
        long seconds = duration.getSeconds();
        long years = seconds / 31536000; seconds %= 31536000;
        long weeks = seconds / 604800;   seconds %= 604800;
        long days = seconds / 86400;     seconds %= 86400;
        long hours = seconds / 3600;     seconds %= 3600;
        long minutes = seconds / 60;     seconds %= 60;
        StringBuilder sb = new StringBuilder();
        if (years > 0) sb.append(years).append(" lata, ");
        if (weeks > 0) sb.append(weeks).append(" tyg., ");
        if (days > 0) sb.append(days).append(" dni, ");
        if (hours > 0) sb.append(hours).append(" godz., ");
        if (minutes > 0) sb.append(minutes).append(" min., ");
        if (seconds > 0 && sb.length() == 0) sb.append(seconds).append(" sek.");
        String out = sb.toString().trim();
        if (out.endsWith(",")) out = out.substring(0, out.length() - 1);
        return out.isEmpty() ? "Wygasł" : out;
    }

    public static String formatDuration(LocalDateTime expireTime) {
        if (expireTime == null) return "NA ZAWSZE";
        return formatDuration(Duration.between(LocalDateTime.now(), expireTime));
    }

    // =========================================================================
    // Wiadomości dla Ban/Mute
    // =========================================================================
    public static Component getBanMessage(BanRecord record) {
        if (record == null) return mm.deserialize("<red>Błąd danych bana.</red>");
        String expires = record.isPermanent() ? "<red>permanentnie</red>" : "<yellow>" + formatDuration(record.getExpireTime()) + "</yellow>";
        String raw = "&cZostałeś zbanowany!\n" +
                "&7-----------------------------------\n" +
                "&cPowód: &f" + record.getReason() + "\n" +
                "&cWygasa: " + expires + "\n" +
                "&cBanner: &f" + record.getBannerName() + "\n" +
                "&cData bana: &f" + formatDateTime(record.getBanTime()) + "\n" +
                "&7-----------------------------------";
        return ColorUtils.toComponent(raw);
    }

    public static Component getMuteMessage(MuteRecord record) {
        if (record == null) return mm.deserialize("<red>Błąd danych wyciszenia.</red>");
        String expires = record.isPermanent() ? "<red>permanentnie</red>" : "<yellow>" + formatDuration(record.getExpireTime()) + "</yellow>";
        String raw = "&cZostałeś wyciszony!\n" +
                "&7-----------------------------------\n" +
                "&cPowód: &f" + record.getReason() + "\n" +
                "&cWygasa: " + expires + "\n" +
                "&cMuter: &f" + record.getMuterName() + "\n" +
                "&cData wyciszenia: &f" + formatDateTime(record.getMuteTime()) + "\n" +
                "&7-----------------------------------";
        return ColorUtils.toComponent(raw);
    }
}