package com.DogController.project.utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.Test;

class ServerCompatTest {

    @Test
    void hexIsUnsupportedBeforeOneSixteen() {
        assertFalse(ServerCompat.supportsHex("1.8.8-R0.1-SNAPSHOT"));
        assertFalse(ServerCompat.supportsHex("1.15.2-R0.1-SNAPSHOT"));
    }

    @Test
    void hexIsSupportedFromOneSixteen() {
        assertTrue(ServerCompat.supportsHex("1.16.5-R0.1-SNAPSHOT"));
        assertTrue(ServerCompat.supportsHex("1.20.6-R0.1-SNAPSHOT"));
        assertTrue(ServerCompat.supportsHex("26.3-R0.1-SNAPSHOT"));
    }

    @Test
    void parsesMajorAndMinorFromBukkitVersion() {
        assertArrayEquals(new int[] {1, 8}, ServerCompat.parseVersion("1.8.8-R0.1-SNAPSHOT"));
        assertArrayEquals(new int[] {26, 3}, ServerCompat.parseVersion("26.3-R0.1-SNAPSHOT"));
        assertArrayEquals(new int[] {0, 0}, ServerCompat.parseVersion(null));
    }

    @Test
    void resolvesResistanceOnLegacyApi() {
        assertNotNull(ServerCompat.resistanceType());
    }

    @Test
    void hiddenEffectUsesTheAvailableConstructor() {
        PotionEffectType type = ServerCompat.resistanceType();
        assertNotNull(type);
        PotionEffect effect = ServerCompat.createHiddenEffect(type, 60, 5);

        assertEquals(type, effect.getType());
        assertEquals(60, effect.getDuration());
        assertEquals(5, effect.getAmplifier());
        assertFalse(effect.isAmbient());
        assertFalse(effect.hasParticles());
    }
}
