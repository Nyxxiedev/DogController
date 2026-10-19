package com.DogController.project.managers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;

public class DogControlManager {
    private static final double DEFENSE_RADIUS = 12.0;

    public enum Disposition {
        AGGRESSIVE,
        PASSIVE,
        NEUTRAL
    }

    private final Map<UUID, Map<UUID, Disposition>> dispositions = new HashMap<>();
    private final Set<UUID> defenseOwners = new HashSet<>();

    public void setDisposition(Player owner, Player target, Disposition disposition) {
        dispositions.computeIfAbsent(owner.getUniqueId(), ignored -> new HashMap<>())
                .put(target.getUniqueId(), disposition);
        if (disposition != Disposition.AGGRESSIVE) {
            clearTargeting(owner, target);
        }
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

    public int teleportDogs(Player owner) {
        int teleported = 0;
        Location destination = owner.getLocation();
        for (Wolf wolf : getOwnedWolves(owner.getUniqueId())) {
            if (wolf.teleport(destination)) {
                teleported++;
            }
        }
        return teleported;
    }

    public boolean shouldPreventTargeting(UUID ownerId, UUID targetId) {
        return ownerId.equals(targetId) || getDisposition(ownerId, targetId) == Disposition.PASSIVE;
    }

    public void updateTargets() {
        for (Player owner : Bukkit.getOnlinePlayers()) {
            UUID ownerId = owner.getUniqueId();
            for (Wolf wolf : getOwnedWolves(ownerId)) {
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

    private void clearTargeting(Player owner, Player target) {
        for (Wolf wolf : getOwnedWolves(owner.getUniqueId())) {
            LivingEntity currentTarget = wolf.getTarget();
            if (currentTarget != null && currentTarget.getUniqueId().equals(target.getUniqueId())) {
                wolf.setTarget(null);
            }
        }
    }

    private List<Wolf> getOwnedWolves(UUID ownerId) {
        List<Wolf> wolves = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            for (Wolf wolf : world.getEntitiesByClass(Wolf.class)) {
                if (!wolf.isTamed()) {
                    continue;
                }
                AnimalTamer owner = wolf.getOwner();
                if (owner != null && owner.getUniqueId().equals(ownerId)) {
                    wolves.add(wolf);
                }
            }
        }
        return wolves;
    }
}
