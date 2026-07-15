package com.abnormalities.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class NurHorrorCycle {
    public static boolean active = false;
    public static int speedMultiplier = 20;
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!active) return;
        ServerLevel overworld = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return;
        long currentTime = overworld.getDayTime();
        overworld.setDayTime(currentTime + speedMultiplier);
    }
    public static void start() {
        active = true;
    }
    public static void stop() {
        active = false;
    }
}