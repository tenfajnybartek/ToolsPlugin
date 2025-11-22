package pl.tenfajnybartek.toolsplugin.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtils {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    /**
     * Koloruje tekst używając kodów & (np. &c, &a, &l)
     * Obsługuje również HEX kolory w formacie &#RRGGBB
     * Zwraca gotowy String do wysłania
     */
    public static String colorize(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        // Obsługa HEX kolorów (&#RRGGBB -> <#RRGGBB>)
        message = translateHexCodes(message);

        // Konwertuj & na § dla Legacy
        message = message.replace('&', '§');

        return message;
    }

    /**
     * Konwertuje kody HEX z formatu &#RRGGBB na format MiniMessage <#RRGGBB>
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
     * Konwertuje tekst MiniMessage na Component
     * Przykład: <red>Tekst</red>, <gradient:red:blue>Gradient</gradient>
     */
    public static Component miniMessage(String message) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }
        return MINI_MESSAGE.deserialize(message);
    }

    /**
     * Konwertuje tekst z kolorami & na Component (obsługuje też HEX)
     */
    public static Component toComponent(String message) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }

        // Najpierw tłumaczymy HEX kody
        message = translateHexCodes(message);

        // Potem konwertujemy & na component
        return LEGACY_AMPERSAND.deserialize(message);
    }

    /**
     * Konwertuje Component na tekst z kolorami &
     */
    public static String fromComponent(Component component) {
        if (component == null) {
            return "";
        }
        return LEGACY_AMPERSAND.serialize(component);
    }

    /**
     * Konwertuje tekst z § na Component (legacy format)
     */
    public static Component fromLegacy(String message) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }
        return LEGACY_SECTION.deserialize(message);
    }

    /**
     * Usuwa wszystkie kody kolorów z tekstu (zwraca czysty tekst)
     */
    public static String stripColors(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        Component component = toComponent(message);
        return PLAIN.serialize(component);
    }

    /**
     * Usuwa wszystkie kody kolorów z Component (zwraca czysty tekst)
     */
    public static String stripColors(Component component) {
        if (component == null) {
            return "";
        }
        return PLAIN.serialize(component);
    }

    /**
     * Łączy wiele komponentów w jeden
     */
    public static Component join(Component... components) {
        Component result = Component.empty();
        for (Component component : components) {
            result = result.append(component);
        }
        return result;
    }
}
