package com.DogController.project.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;

class UtilsTest {

    @Test
    void formatsHexAsSectionSignSequence() {
        assertEquals("§x§1§2§a§b§e§f", Utils.toSectionSign("#12AbEf"));
    }

    @Test
    void mapsHexToNearestLegacyColor() {
        assertEquals("§4", Utils.nearestLegacyColor("#FF0000"));
        assertEquals("§c", Utils.nearestLegacyColor("#FF5555"));
        assertEquals("§f", Utils.nearestLegacyColor("#FFFFFF"));
        assertEquals("§0", Utils.nearestLegacyColor("#000000"));
    }

    @Test
    void translatesAmpersandCodes() {
        assertEquals("§6hi", Utils.colorize("&6hi", true));
    }

    @Test
    void usesHexWhenSupported() {
        assertEquals("§x§f§f§5§5§5§5x", Utils.colorize("#FF5555x", true));
    }

    @Test
    void fallsBackToLegacyColorWhenHexIsUnsupported() {
        String result = Utils.colorize("#FF5555x", false);
        assertEquals("§cx", result);
        assertFalse(result.contains("#"));
    }
}
