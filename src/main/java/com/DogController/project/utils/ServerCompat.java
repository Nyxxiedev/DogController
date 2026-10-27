package com.DogController.project.utils;

import java.lang.reflect.Method;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Small compatibility layer for API that changed across Minecraft versions.
 *
 * <p>Nothing in here may reference a symbol that does not exist in Spigot
 * 1.8.8; version-specific names are resolved reflectively at runtime.
 */
public final class ServerCompat {
    private static PotionEffectType resistanceType;
    private static boolean resistanceResolved;
    private static boolean hexSupported;
    private static boolean hexResolved;
    private static Method getEntityMethod;
    private static boolean getEntityResolved;

    private ServerCompat() {
    }

    public static boolean isHexColorSupported() {
        if (!hexResolved) {
            hexResolved = true;
            hexSupported = supportsHex(Bukkit.getBukkitVersion());
        }
        return hexSupported;
    }

    static boolean supportsHex(String bukkitVersion) {
        int[] version = parseVersion(bukkitVersion);
        return version[0] > 1 || (version[0] == 1 && version[1] >= 16);
    }

    static int[] parseVersion(String bukkitVersion) {
        int[] version = new int[] {0, 0};
        if (bukkitVersion == null) {
            return version;
        }

        String[] parts = bukkitVersion.split("[.-]");
        try {
            if (parts.length > 0) {
                version[0] = Integer.parseInt(parts[0]);
            }
            if (parts.length > 1) {
                version[1] = Integer.parseInt(parts[1]);
            }
        } catch (NumberFormatException ignored) {
            // Keep the segments that parsed before the unexpected one.
        }
        return version;
    }

    /**
     * Returns the resistance potion type for this server.
     *
     * <p>Called {@code DAMAGE_RESISTANCE} through 1.20.4 and {@code RESISTANCE}
     * from 1.20.5 onwards. Returns {@code null} only if neither exists, in
     * which case callers should skip the effect instead of failing.
     */
    public static PotionEffectType resistanceType() {
        if (!resistanceResolved) {
            resistanceResolved = true;
            resistanceType = resolveField("RESISTANCE");
            if (resistanceType == null) {
                resistanceType = resolveField("DAMAGE_RESISTANCE");
            }
            if (resistanceType == null) {
                resistanceType = resolveByName("RESISTANCE");
            }
            if (resistanceType == null) {
                resistanceType = resolveByName("DAMAGE_RESISTANCE");
            }
        }
        return resistanceType;
    }

    /**
     * Creates a hidden potion effect, matching the old six-argument effect on
     * versions that have it and falling back to the five-argument form on
     * 1.8-1.12 where the icon flag did not exist.
     */
    public static PotionEffect createHiddenEffect(PotionEffectType type, int duration, int amplifier) {
        try {
            return PotionEffect.class
                    .getConstructor(PotionEffectType.class, int.class, int.class,
                            boolean.class, boolean.class, boolean.class)
                    .newInstance(type, duration, amplifier, false, false, false);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return new PotionEffect(type, duration, amplifier, false, false);
        }
    }

    /**
     * Looks up a loaded entity by UUID through {@code Bukkit.getEntity}, which
     * was added after 1.8.8. Returns {@code null} when the method or entity is
     * unavailable; callers on legacy servers fall back to tracked chunks.
     */
    public static Entity getEntity(UUID id) {
        Method method = getEntityMethod();
        if (method == null) {
            return null;
        }
        try {
            return (Entity) method.invoke(null, id);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    /**
     * Whether {@code Bukkit.getEntity(UUID)} exists on this server.
     */
    public static boolean hasEntityLookup() {
        return getEntityMethod() != null;
    }

    private static Method getEntityMethod() {
        if (!getEntityResolved) {
            getEntityResolved = true;
            try {
                getEntityMethod = Bukkit.class.getMethod("getEntity", UUID.class);
            } catch (NoSuchMethodException | LinkageError ignored) {
                getEntityMethod = null;
            }
        }
        return getEntityMethod;
    }

    private static PotionEffectType resolveField(String name) {
        try {
            return (PotionEffectType) PotionEffectType.class.getField(name).get(null);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    private static PotionEffectType resolveByName(String name) {
        try {
            return PotionEffectType.getByName(name);
        } catch (RuntimeException | LinkageError ignored) {
            return null;
        }
    }
}
