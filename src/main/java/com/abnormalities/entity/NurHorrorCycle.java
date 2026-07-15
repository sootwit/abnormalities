package com.abnormalities.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.concurrent.atomic.AtomicInteger;

public class NurHorrorCycle {
    public static final AtomicInteger chaseCount = new AtomicInteger(0);
    public static int speedMultiplier = 20;
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (chaseCount.get() <= 0) return;
        ServerLevel overworld = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return;
        long currentTime = overworld.getDayTime();
        overworld.setDayTime(currentTime + speedMultiplier);
    }
    public static void start() {
        chaseCount.incrementAndGet();
    }
    public static void stop() {
        if (chaseCount.get() > 0) chaseCount.decrementAndGet();
    }
}