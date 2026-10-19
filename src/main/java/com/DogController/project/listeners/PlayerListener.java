package com.DogController.project.listeners;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
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
        if (!(event.getEntity() instanceof Wolf wolf) || !(event.getTarget() instanceof Player target)
                || !wolf.isTamed()) {
            return;
        }
        AnimalTamer owner = wolf.getOwner();
        if (owner != null && dogControlManager.shouldPreventTargeting(owner.getUniqueId(), target.getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
