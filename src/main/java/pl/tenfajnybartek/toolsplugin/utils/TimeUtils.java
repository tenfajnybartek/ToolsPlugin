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

    // Używamy formatu DATETIME zgodnego z MySQL
    private static final java.time.format.DateTimeFormatter formatter =
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // =========================================================================
    // NOWA METODA: KONWERSJA STRINGA NA DATĘ (PARSOWANIE CZASU TRWANIA)
    // =========================================================================

    /**
     * Parsuje ciąg znaków czasu (np. 1h, 30d, perm) na LocalDateTime.
     * Zwraca NULL dla bana/muta permanentnego (perm).
     */
    public static LocalDateTime parseTime(String timeString) {
        if (timeString == null || timeString.equalsIgnoreCase("perm") || timeString.equalsIgnoreCase("permanent")) {
            return null;
        }

        long duration = 0;

        // Wzorzec: [Liczba][Jednostka]
        Pattern pattern = Pattern.compile("(\\d+)([a-zA-Z])");
        Matcher matcher = pattern.matcher(timeString);

        if (matcher.find()) {
            try {
                long amount = Long.parseLong(matcher.group(1));
                String unit = matcher.group(2).toLowerCase();

                switch (unit) {
                    case "s": // Sekundy
                        duration = amount;
                        break;
                    case "m": // Minuty
                        duration = amount * 60;
                        break;
                    case "h": // Godziny
                        duration = amount * 60 * 60;
                        break;
                    case "d": // Dni
                        duration = amount * 60 * 60 * 24;
                        break;
                    case "w": // Tygodnie
                        duration = amount * 60 * 60 * 24 * 7;
                        break;
                    case "y": // Lata
                        duration = amount * 60 * 60 * 24 * 365;
                        break;
                    default:
                        return null; // Nieznana jednostka
                }

                // Zwraca czas aktualny plus obliczona długość trwania (w sekundach)
                return LocalDateTime.now().plusSeconds(duration);

            } catch (NumberFormatException e) {
                // Błąd parsowania liczby
                return null;
            }
        }

        return null; // Niepoprawny format
    }

    // =========================================================================
    // Twoje istniejące metody
    // =========================================================================

    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "N/A";
        }
        return dateTime.format(DATE_FORMATTER);
    }

    // Używamy enum java.util.concurrent.TimeUnit
    private enum TimeUnit {
        YEARS(31536000L), WEEKS(604800L), DAYS(86400L), HOURS(3600L), MINUTES(60L), SECONDS(1L);

        private final long seconds;
        TimeUnit(long seconds) { this.seconds = seconds; }
        public long getSeconds() { return seconds; }
    }

    public static String formatDuration(Duration duration) {
        if (duration == null || duration.isNegative() || duration.isZero()) {
            return "Wygasł";
        }

        long seconds = duration.getSeconds();

        long years = seconds / TimeUnit.YEARS.getSeconds();
        seconds %= TimeUnit.YEARS.getSeconds();

        long weeks = seconds / TimeUnit.WEEKS.getSeconds();
        seconds %= TimeUnit.WEEKS.getSeconds();

        long days = seconds / TimeUnit.DAYS.getSeconds();
        seconds %= TimeUnit.DAYS.getSeconds();

        long hours = seconds / TimeUnit.HOURS.getSeconds();
        seconds %= TimeUnit.HOURS.getSeconds();

        long minutes = seconds / TimeUnit.MINUTES.getSeconds();
        seconds %= TimeUnit.MINUTES.getSeconds();

        StringBuilder sb = new StringBuilder();

        if (years > 0) sb.append(years).append(" lata, ");
        if (weeks > 0) sb.append(weeks).append(" tyg., ");
        if (days > 0) sb.append(days).append(" dni, ");
        if (hours > 0) sb.append(hours).append(" godz., ");
        if (minutes > 0) sb.append(minutes).append(" min., ");
        if (sb.length() == 0 && seconds > 0) sb.append(seconds).append(" sek.");

        String result = sb.toString().trim();
        if (result.endsWith(",")) result = result.substring(0, result.length() - 1);

        if (result.isEmpty() && duration.getSeconds() > 0) {
            return duration.getSeconds() + " sek.";
        } else if (result.isEmpty()) {
            return "Wygasł";
        }

        return result;
    }

    public static String formatDuration(LocalDateTime expireTime) {
        if (expireTime == null) {
            return "NA ZAWSZE";
        }

        Duration remaining = Duration.between(LocalDateTime.now(), expireTime);
        return formatDuration(remaining);
    }

    /**
     * Tworzy formatowaną wiadomość kicka dla zbanowanego gracza jako Component.
     */
    public static Component getBanMessage(BanRecord record) {
        if (record == null) {
            // Zabezpieczenie na wypadek błędu bazy danych
            return mm.deserialize("<red>Błąd komunikacji z bazą danych.</red>");
        }

        String expires;
        if (record.isPermanent()) {
            expires = "&cpermanetnie";
        }
        else {
            expires = "&fpozostało: &e" + formatDuration(record.getExpireTime());
        }

        String rawMessage =
                "&cZostałeś zbanowany na serwerze!\n" +
                        "&7-----------------------------------\n" +
                        "&cPowód: &f" + record.getReason() + "\n" +
                        "&cWygasa: " + expires + "\n" +
                        "&cBanner: &f" + record.getBannerName() + "\n" +
                        "&cData bana: &f" + formatDateTime(record.getBanTime()) + "\n" +
                        "&7-----------------------------------";

        // WAŻNE: Wymaga ColorUtils.toComponent!
        return ColorUtils.toComponent(rawMessage);
    }

    public static Component getMuteMessage(MuteRecord record) {
        if (record == null) {
            return mm.deserialize("<red>Błąd podczas komunikacji z bazą danych lub wyciszenie wygasło.</red>");
        }

        Component header = mm.deserialize("<red>Zostałeś wyciszony na serwerze!</red>");
        Component reason = mm.deserialize("<gray>Powód: <white>" + record.getReason() + "</white></gray>");

        String expiresText;
        if (record.isPermanent()) {
            expiresText = "<red>permanentnie</red>";
        } else {
            String duration = formatDuration(record.getExpireTime());
            expiresText = "<yellow>" + duration + "</yellow>";
        }
        Component expires = mm.deserialize("<gray>Wygasa: " + expiresText + "</gray>");

        Component muter = mm.deserialize("<gray>Muter: <white>" + record.getMuterName() + "</white></gray>");

        Component muteTime = mm.deserialize("<gray>Data wyciszenia: <white>" + record.getMuteTime().format(formatter) + "</white></gray>");

        return mm.deserialize("<dark_gray>-------------------------</dark_gray>")
                .append(Component.newline())
                .append(header)
                .append(Component.newline())
                .append(mm.deserialize("<dark_gray>-------------------------</dark_gray>"))
                .append(Component.newline())
                .append(reason)
                .append(Component.newline())
                .append(expires)
                .append(Component.newline())
                .append(muter)
                .append(Component.newline())
                .append(muteTime)
                .append(Component.newline())
                .append(mm.deserialize("<dark_gray>-------------------------</dark_gray>"));
    }
}
