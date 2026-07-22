package com.abnormalities.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class NurHorrorCycle {
    private static final Set<UUID> chasingNurs = new HashSet<>();
    public static int speedMultiplier = 20;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ServerLevel overworld = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        chasingNurs.removeIf(uuid -> {
            Entity e = overworld.getEntity(uuid);
            return e == null || !e.isAlive();
        });

        if (chasingNurs.isEmpty()) return;
        long currentTime = overworld.getDayTime();
        overworld.setDayTime(currentTime + speedMultiplier);
    }

    public static void start(UUID entityId) {
        chasingNurs.add(entityId);
    }

    public static void stop(UUID entityId) {
        chasingNurs.remove(entityId);
    }
}
