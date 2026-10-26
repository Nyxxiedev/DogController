package com.DogController.project.managers;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntConsumer;
import java.util.function.Predicate;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

public class DogControlManager {
    private static final double DEFENSE_RADIUS = 12.0;
    private static final int MAX_TELEPORT_RINGS = 12;
    private static final int TELEPORT_RESISTANCE_TICKS = 60;
    private static final int TELEPORT_RESISTANCE_AMPLIFIER = 5;
    private static final String TRACKED_WOLVES_KEY = "wolves";

    public enum Disposition {
        AGGRESSIVE,
        PASSIVE,
        NEUTRAL
    }

    private final Map<UUID, Map<UUID, Disposition>> dispositions = new HashMap<>();
    private final Set<UUID> defenseOwners = new HashSet<>();
    private final Map<UUID, TrackedDog> trackedDogs = new HashMap<>();
    private final JavaPlugin plugin;
    private final File trackingFile;
    private final YamlConfiguration trackingData = new YamlConfiguration();
    private BukkitTask pendingSave;

    public DogControlManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.trackingFile = new File(plugin.getDataFolder(), "dog-tracking.yml");
        loadTrackedDogs();
    }

    public void trackLoadedDogs() {
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                trackChunkDogs(chunk);
            }
        }
    }

    public void trackChunkDogs(Chunk chunk) {
        for (var entity : chunk.getEntities()) {
            if (entity instanceof Wolf wolf) {
                trackWolf(wolf);
            }
        }
    }

    public void trackTamedWolfNextTick(Wolf wolf) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (wolf.isValid() && !wolf.isDead()) {
                trackWolf(wolf);
            }
        });
    }

    public void onWolfDeath(Wolf wolf) {
        if (trackedDogs.remove(wolf.getUniqueId()) != null) {
            scheduleSave();
        }
    }

    public void saveAndStop() {
        for (UUID dogId : new ArrayList<>(trackedDogs.keySet())) {
            if (Bukkit.getEntity(dogId) instanceof Wolf wolf && wolf.isValid() && !wolf.isDead()) {
                trackWolf(wolf);
            }
        }
        if (pendingSave != null) {
            pendingSave.cancel();
            pendingSave = null;
        }
        saveTrackedDogs();
    }

    public void setDisposition(Player owner, Player target, Disposition disposition) {
        dispositions.computeIfAbsent(owner.getUniqueId(), ignored -> new HashMap<>())
                .put(target.getUniqueId(), disposition);
        processOwnedDogs(owner, wolf -> {
            if (disposition == Disposition.AGGRESSIVE) {
                if (target.isOnline() && !wolf.isSitting()) {
                    wolf.setTarget(target);
                }
            } else if (wolf.getTarget() != null
                    && wolf.getTarget().getUniqueId().equals(target.getUniqueId())) {
                wolf.setTarget(null);
            }
            return true;
        }, ignored -> {});
    }

    public Disposition getDisposition(UUID ownerId, UUID targetId) {
        return dispositions.getOrDefault(ownerId, Map.of())
                .getOrDefault(targetId, Disposition.NEUTRAL);
    }

    public boolean isDefenseEnabled(UUID ownerId) {
        return defenseOwners.contains(ownerId);
    }

    public void setDefenseEnabled(Player owner, boolean enabled) {
        if (enabled) {
            defenseOwners.add(owner.getUniqueId());
        } else {
            defenseOwners.remove(owner.getUniqueId());
        }
    }

    public boolean toggleDefense(Player owner) {
        boolean enabled = !isDefenseEnabled(owner.getUniqueId());
        setDefenseEnabled(owner, enabled);
        return enabled;
    }

    public void teleportDogs(Player owner, IntConsumer completion) {
        Location center = owner.getLocation().clone();
        List<Location> occupiedSpots = new ArrayList<>();
        processOwnedDogs(owner, wolf -> {
            Location destination = findTeleportSpot(center, occupiedSpots);
            if (destination == null || !wolf.teleport(destination)) {
                return false;
            }
            wolf.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE,
                    TELEPORT_RESISTANCE_TICKS, TELEPORT_RESISTANCE_AMPLIFIER, false, false, false));
            occupiedSpots.add(destination);
            return true;
        }, completion);
    }

    public void setDogsSitting(Player owner, boolean sitting, IntConsumer completion) {
        processOwnedDogs(owner, wolf -> {
            wolf.setSitting(sitting);
            return true;
        }, completion);
    }

    private Location findTeleportSpot(Location center, List<Location> occupiedSpots) {
        for (int ring = 1; ring <= MAX_TELEPORT_RINGS; ring++) {
            for (int offsetX = -ring; offsetX <= ring; offsetX++) {
                for (int offsetZ = -ring; offsetZ <= ring; offsetZ++) {
                    if (Math.max(Math.abs(offsetX), Math.abs(offsetZ)) != ring) {
                        continue;
                    }
                    int blockX = center.getBlockX() + offsetX;
                    int blockZ = center.getBlockZ() + offsetZ;
                    World world = center.getWorld();
                    if (!world.isChunkLoaded(blockX >> 4, blockZ >> 4)) {
                        continue;
                    }
                    int groundY = world.getHighestBlockYAt(blockX, blockZ);
                    Location candidate = new Location(world, blockX + 0.5, groundY + 1, blockZ + 0.5,
                            center.getYaw(), center.getPitch());

                    if (hasSafeStandingSpace(candidate) && !isTooClose(candidate, occupiedSpots)) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    private void processOwnedDogs(Player owner, Predicate<Wolf> action, IntConsumer completion) {
        UUID ownerId = owner.getUniqueId();

        Map<ChunkKey, Set<UUID>> dogsByChunk = new LinkedHashMap<>();
        for (Map.Entry<UUID, TrackedDog> entry : trackedDogs.entrySet()) {
            TrackedDog dog = entry.getValue();
            if (!dog.ownerId().equals(ownerId)) {
                continue;
            }
            ChunkKey chunkKey = new ChunkKey(dog.worldId(), dog.chunkX(), dog.chunkZ());
            dogsByChunk.computeIfAbsent(chunkKey, ignored -> new HashSet<>()).add(entry.getKey());
        }

        List<Map.Entry<ChunkKey, Set<UUID>>> chunks = new ArrayList<>(dogsByChunk.entrySet());
        if (chunks.isEmpty()) {
            completion.accept(0);
            return;
        }

        new BukkitRunnable() {
            private int nextChunk;
            private int affected;

            @Override
            public void run() {
                if (!owner.isOnline() || nextChunk >= chunks.size()) {
                    cancel();
                    if (owner.isOnline()) {
                        completion.accept(affected);
                    }
                    return;
                }

                Map.Entry<ChunkKey, Set<UUID>> entry = chunks.get(nextChunk++);
                ChunkKey key = entry.getKey();
                World world = Bukkit.getWorld(key.worldId());
                if (world == null) {
                    return;
                }

                if (!world.isChunkLoaded(key.chunkX(), key.chunkZ())
                        && !world.loadChunk(key.chunkX(), key.chunkZ(), false)) {
                    return;
                }

                Chunk chunk = world.getChunkAt(key.chunkX(), key.chunkZ());
                for (var entity : chunk.getEntities()) {
                    if (!(entity instanceof Wolf wolf) || !entry.getValue().contains(wolf.getUniqueId())) {
                        continue;
                    }
                    AnimalTamer actualOwner = wolf.getOwner();
                    if (!wolf.isTamed() || actualOwner == null || !ownerId.equals(actualOwner.getUniqueId())) {
                        trackedDogs.remove(wolf.getUniqueId());
                        scheduleSave();
                        continue;
                    }
                    boolean changed = action.test(wolf);
                    trackWolf(wolf);
                    if (changed) {
                        affected++;
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private boolean hasSafeStandingSpace(Location location) {
        var feet = location.getBlock();
        var head = feet.getRelative(0, 1, 0);
        var ground = feet.getRelative(0, -1, 0);
        return feet.getType().isAir() && head.getType().isAir() && ground.getType().isSolid();
    }

    private boolean isTooClose(Location candidate, List<Location> occupiedSpots) {
        for (Location occupied : occupiedSpots) {
            double xDistance = candidate.getX() - occupied.getX();
            double zDistance = candidate.getZ() - occupied.getZ();
            if (xDistance * xDistance + zDistance * zDistance < 1.0) {
                return true;
            }
        }
        return false;
    }

    public boolean shouldPreventTargeting(UUID ownerId, UUID targetId) {
        return ownerId.equals(targetId) || getDisposition(ownerId, targetId) == Disposition.PASSIVE;
    }

    public void updateTargets() {
        for (Player owner : Bukkit.getOnlinePlayers()) {
            UUID ownerId = owner.getUniqueId();
            for (Wolf wolf : getOwnedWolves(ownerId)) {
                if (wolf.isSitting()) {
                    continue;
                }
                Player target = findAggressiveTarget(ownerId, wolf);
                if (target == null && isDefenseEnabled(ownerId)) {
                    target = findDefenseTarget(owner, wolf);
                }
                if (target != null && !target.equals(wolf.getTarget())) {
                    wolf.setTarget(target);
                }
            }
        }
    }

    private Player findAggressiveTarget(UUID ownerId, Wolf wolf) {
        Player closest = null;
        double closestDistance = Double.MAX_VALUE;
        Map<UUID, Disposition> ownerDispositions = dispositions.getOrDefault(ownerId, Map.of());
        for (Map.Entry<UUID, Disposition> entry : ownerDispositions.entrySet()) {
            if (entry.getValue() != Disposition.AGGRESSIVE) {
                continue;
            }
            Player candidate = Bukkit.getPlayer(entry.getKey());
            if (candidate == null || !candidate.getWorld().equals(wolf.getWorld())) {
                continue;
            }
            double distance = candidate.getLocation().distanceSquared(wolf.getLocation());
            if (distance < closestDistance) {
                closest = candidate;
                closestDistance = distance;
            }
        }
        return closest;
    }

    private Player findDefenseTarget(Player owner, Wolf wolf) {
        Player closest = null;
        double closestOwnerDistance = DEFENSE_RADIUS * DEFENSE_RADIUS;
        double closestWolfDistance = Double.MAX_VALUE;
        for (Player candidate : Bukkit.getOnlinePlayers()) {
            if (candidate.getUniqueId().equals(owner.getUniqueId())
                    || shouldPreventTargeting(owner.getUniqueId(), candidate.getUniqueId())
                    || !candidate.getWorld().equals(owner.getWorld())) {
                continue;
            }
            double ownerDistance = candidate.getLocation().distanceSquared(owner.getLocation());
            if (ownerDistance > closestOwnerDistance) {
                continue;
            }
            if (!candidate.getWorld().equals(wolf.getWorld())) {
                continue;
            }
            double wolfDistance = candidate.getLocation().distanceSquared(wolf.getLocation());
            if (closest == null || ownerDistance < closestOwnerDistance
                    || (ownerDistance == closestOwnerDistance && wolfDistance < closestWolfDistance)) {
                closest = candidate;
                closestOwnerDistance = ownerDistance;
                closestWolfDistance = wolfDistance;
            }
        }
        return closest;
    }

    private List<Wolf> getOwnedWolves(UUID ownerId) {
        List<Wolf> wolves = new ArrayList<>();
        List<UUID> dogIds = trackedDogs.entrySet().stream()
                .filter(entry -> entry.getValue().ownerId().equals(ownerId))
                .map(Map.Entry::getKey)
                .toList();
        for (UUID dogId : dogIds) {
            if (!(Bukkit.getEntity(dogId) instanceof Wolf wolf)) {
                continue;
            }
            if (!wolf.isValid() || wolf.isDead()) {
                trackedDogs.remove(dogId);
                scheduleSave();
                continue;
            }

            AnimalTamer owner = wolf.isTamed() ? wolf.getOwner() : null;
            if (owner == null) {
                trackedDogs.remove(dogId);
                scheduleSave();
                continue;
            }

            trackWolf(wolf);
            if (owner.getUniqueId().equals(ownerId)) {
                wolves.add(wolf);
            }
        }
        return wolves;
    }

    private void trackWolf(Wolf wolf) {
        UUID wolfId = wolf.getUniqueId();
        AnimalTamer owner = wolf.isTamed() ? wolf.getOwner() : null;
        if (owner == null) {
            if (trackedDogs.remove(wolfId) != null) {
                scheduleSave();
            }
            return;
        }

        TrackedDog trackedDog = new TrackedDog(owner.getUniqueId(), wolf.getWorld().getUID(),
                wolf.getLocation().getChunk().getX(), wolf.getLocation().getChunk().getZ());
        if (!trackedDog.equals(trackedDogs.put(wolfId, trackedDog))) {
            scheduleSave();
        }
    }

    private void loadTrackedDogs() {
        if (!trackingFile.exists()) {
            return;
        }

        try {
            trackingData.load(trackingFile);
        } catch (IOException | InvalidConfigurationException exception) {
            throw new IllegalStateException("Could not load tracked dog locations from " + trackingFile, exception);
        }

        var section = trackingData.getConfigurationSection(TRACKED_WOLVES_KEY);
        if (section == null) {
            return;
        }
        for (String wolfIdText : section.getKeys(false)) {
            try {
                UUID wolfId = UUID.fromString(wolfIdText);
                String path = TRACKED_WOLVES_KEY + "." + wolfIdText;
                UUID ownerId = UUID.fromString(trackingData.getString(path + ".owner", ""));
                UUID worldId = UUID.fromString(trackingData.getString(path + ".world", ""));
                int chunkX = trackingData.getInt(path + ".chunk-x");
                int chunkZ = trackingData.getInt(path + ".chunk-z");
                trackedDogs.put(wolfId, new TrackedDog(ownerId, worldId, chunkX, chunkZ));
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Ignoring invalid dog tracking entry: " + wolfIdText);
            }
        }
    }

    private void scheduleSave() {
        if (pendingSave != null) {
            return;
        }
        pendingSave = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pendingSave = null;
            saveTrackedDogs();
        }, 20L);
    }

    private void saveTrackedDogs() {
        trackingData.set(TRACKED_WOLVES_KEY, null);
        for (Map.Entry<UUID, TrackedDog> entry : trackedDogs.entrySet()) {
            String path = TRACKED_WOLVES_KEY + "." + entry.getKey();
            TrackedDog dog = entry.getValue();
            trackingData.set(path + ".owner", dog.ownerId().toString());
            trackingData.set(path + ".world", dog.worldId().toString());
            trackingData.set(path + ".chunk-x", dog.chunkX());
            trackingData.set(path + ".chunk-z", dog.chunkZ());
        }

        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            plugin.getLogger().severe("Could not create plugin data folder to save tracked dog locations.");
            return;
        }
        try {
            trackingData.save(trackingFile);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save tracked dog locations: " + exception.getMessage());
            exception.printStackTrace();
        }
    }

    private record TrackedDog(UUID ownerId, UUID worldId, int chunkX, int chunkZ) {}

    private record ChunkKey(UUID worldId, int chunkX, int chunkZ) {}
}
