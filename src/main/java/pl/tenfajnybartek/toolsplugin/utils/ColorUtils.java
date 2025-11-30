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

    public static String colorize(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        Component component = LEGACY_AMPERSAND.deserialize(message);

        return LEGACY_SECTION.serialize(component);
    }

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

    public static Component miniMessage(String message) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }
        return MINI_MESSAGE.deserialize(message);
    }

    public static Component toComponent(String message) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }

        return LEGACY_AMPERSAND.deserialize(message);
    }


    public static String fromComponent(Component component) {
        if (component == null) {
            return "";
        }
        return LEGACY_AMPERSAND.serialize(component);
    }


    public static Component fromLegacy(String message) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }
        return LEGACY_SECTION.deserialize(message);
    }

    public static String stripColors(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        Component component = toComponent(message);
        return PLAIN.serialize(component);
    }

    public static String stripColors(Component component) {
        if (component == null) {
            return "";
        }
        return PLAIN.serialize(component);
    }

    public static Component join(Component... components) {
        Component result = Component.empty();
        for (Component component : components) {
            result = result.append(component);
        }
        return result;
    }
}
