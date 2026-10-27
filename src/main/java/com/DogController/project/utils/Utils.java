package com.DogController.project.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.ChatColor;

public final class Utils {
    private static final Pattern HEX_PATTERN = Pattern.compile("#[a-fA-F0-9]{6}");
    private static final char[] LEGACY_CODES = {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };
    private static final int[][] LEGACY_RGB = {
            {0x00, 0x00, 0x00}, {0x00, 0x00, 0xAA}, {0x00, 0xAA, 0x00}, {0x00, 0xAA, 0xAA},
            {0xAA, 0x00, 0x00}, {0xAA, 0x00, 0xAA}, {0xFF, 0xAA, 0x00}, {0xAA, 0xAA, 0xAA},
            {0x55, 0x55, 0x55}, {0x55, 0x55, 0xFF}, {0x55, 0xFF, 0x55}, {0x55, 0xFF, 0xFF},
            {0xFF, 0x55, 0x55}, {0xFF, 0x55, 0xFF}, {0xFF, 0xFF, 0x55}, {0xFF, 0xFF, 0xFF}
    };

    private Utils() {
    }

    public static String colorize(String message) {
        return colorize(message, ServerCompat.isHexColorSupported());
    }

    static String colorize(String message, boolean hexSupported) {
        if (message == null) {
            return null;
        }

        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String replacement = hexSupported
                    ? toSectionSign(matcher.group())
                    : nearestLegacyColor(matcher.group());
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    static String toSectionSign(String hex) {
        StringBuilder result = new StringBuilder();
        result.append(ChatColor.COLOR_CHAR).append('x');
        for (int index = 1; index < hex.length(); index++) {
            result.append(ChatColor.COLOR_CHAR).append(Character.toLowerCase(hex.charAt(index)));
        }
        return result.toString();
    }

    static String nearestLegacyColor(String hex) {
        int rgb = Integer.parseInt(hex.substring(1), 16);
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;

        int closestIndex = 0;
        int closestDistance = Integer.MAX_VALUE;
        for (int index = 0; index < LEGACY_RGB.length; index++) {
            int redDistance = red - LEGACY_RGB[index][0];
            int greenDistance = green - LEGACY_RGB[index][1];
            int blueDistance = blue - LEGACY_RGB[index][2];
            int distance = redDistance * redDistance
                    + greenDistance * greenDistance
                    + blueDistance * blueDistance;
            if (distance < closestDistance) {
                closestDistance = distance;
                closestIndex = index;
            }
        }
        return String.valueOf(ChatColor.COLOR_CHAR) + LEGACY_CODES[closestIndex];
    }
}
