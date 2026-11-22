package es.odissey.protecciones.gui;

import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility to translate colours in strings, supporting both the classic {@code &a} codes
 * and the {@code &#RRGGBB} hex format.
 */
public final class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private ColorUtil() {
    }

    public static String color(String input) {
        if (input == null) return "";
        Matcher matcher = HEX_PATTERN.matcher(input);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            StringBuilder replacement = new StringBuilder("&x");
            for (char c : matcher.group(1).toCharArray()) {
                replacement.append('&').append(c);
            }
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    public static String stripColor(String input) {
        return ChatColor.stripColor(color(input));
    }
}
