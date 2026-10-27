package com.DogController.project.listeners;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import com.DogController.project.managers.DogControlManager;

public class PlayerListener implements Listener {

    private final DogControlManager dogControlManager;

    public PlayerListener(DogControlManager dogControlManager) {
        this.dogControlManager = dogControlManager;
    }

    @EventHandler
    public void onWolfTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof Wolf) || !(event.getTarget() instanceof Player)) {
            return;
        }
        Wolf wolf = (Wolf) event.getEntity();
        Player target = (Player) event.getTarget();
        if (!wolf.isTamed()) {
            return;
        }
        AnimalTamer owner = wolf.getOwner();
        if (owner != null && dogControlManager.shouldPreventTargeting(owner.getUniqueId(), target.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        dogControlManager.trackChunkDogs(event.getChunk());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        dogControlManager.onChunkUnload(event.getChunk());
    }

    @EventHandler
    public void onWolfTamed(EntityTameEvent event) {
        if (event.getEntity() instanceof Wolf) {
            dogControlManager.trackTamedWolfNextTick((Wolf) event.getEntity());
        }
    }

    @EventHandler
    public void onWolfDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Wolf) {
            dogControlManager.onWolfDeath((Wolf) event.getEntity());
        }
    }
}
