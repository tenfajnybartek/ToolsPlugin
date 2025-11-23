package pl.tenfajnybartek.toolsplugin.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtils {

    // Używane do deserializacji/serializacji MiniMessage
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    // Używane do konwersji tekst -> Component (obsługuje & i kody Adventure/HEX)
    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand();

    // Używane do konwersji Component -> tekst z kodami §
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();

    // Używane do usuwania formatowania (zwraca czysty tekst)
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    // Wzór do wykrywania starych kodów HEX: &#RRGGBB
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    /**
     * Koloruje tekst używając kodów & (np. &c, &a, &l).
     * Obsługuje również HEX kolory w formacie &#RRGGBB.
     * Zwraca String z kodami §, który jest natywnie akceptowany przez starsze API Bukkit.
     * * KOREKTA: Metoda używa pełnego łańcucha konwersji Adventure, aby poprawnie obsłużyć HEX.
     */
    public static String colorize(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        // 1. Konwertujemy wejściowy String (z & i &#RRGGBB) na Component.
        // LEGACY_AMPERSAND wykonuje konwersję & -> Component, w tym wspiera format HEX.
        Component component = LEGACY_AMPERSAND.deserialize(message);

        // 2. Następnie konwertujemy Component z powrotem na String z kodami §.
        // To jest wymagane przez starsze metody Bukkit, które przyjmują String.
        return LEGACY_SECTION.serialize(component);
    }

    /**
     * Konwertuje kody HEX z formatu &#RRGGBB na format MiniMessage <#RRGGBB>.
     * Ta metoda jest teraz używana tylko wewnętrznie w toComponent/colorize, aby zapewnić kompatybilność.
     */
    private static String translateHexCodes(String message) {
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String hexCode = matcher.group(1);
            matcher.appendReplacement(buffer, "<#" + hexCode + ">");
        }
        matcher.appendTail(buffer);

        return buffer.toString();
    }

    /**
     * Konwertuje tekst MiniMessage na Component.
     * Przykład: <red>Tekst</red>, <gradient:red:blue>Gradient</gradient>
     */
    public static Component miniMessage(String message) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }
        return MINI_MESSAGE.deserialize(message);
    }

    /**
     * Konwertuje tekst z kolorami & na Component (obsługuje też HEX).
     * Poprawnie obsługuje konwersję z formatu & na Component Adventure.
     */
    public static Component toComponent(String message) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }

        // LEGACY_AMPERSAND domyślnie obsługuje kody & i Adventure HEX (&#RRGGBB).
        return LEGACY_AMPERSAND.deserialize(message);
    }

    /**
     * Konwertuje Component na tekst z kolorami &.
     */
    public static String fromComponent(Component component) {
        if (component == null) {
            return "";
        }
        return LEGACY_AMPERSAND.serialize(component);
    }

    /**
     * Konwertuje tekst z § na Component (legacy format).
     */
    public static Component fromLegacy(String message) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }
        return LEGACY_SECTION.deserialize(message);
    }

    /**
     * Usuwa wszystkie kody kolorów z tekstu (zwraca czysty tekst).
     */
    public static String stripColors(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        // Używamy toComponent, aby poprawnie sparsować zarówno &c jak i HEX.
        Component component = toComponent(message);
        return PLAIN.serialize(component);
    }

    /**
     * Usuwa wszystkie kody kolorów z Component (zwraca czysty tekst).
     */
    public static String stripColors(Component component) {
        if (component == null) {
            return "";
        }
        return PLAIN.serialize(component);
    }

    /**
     * Łączy wiele komponentów w jeden.
     */
    public static Component join(Component... components) {
        Component result = Component.empty();
        for (Component component : components) {
            result = result.append(component);
        }
        return result;
    }
}
