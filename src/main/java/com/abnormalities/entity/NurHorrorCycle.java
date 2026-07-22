package com.abnormalities.entity;

import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;

public class NurHorrorCycle {
    private static final Map<UUID, Set<UUID>> playerNurs = new HashMap<>();
    private static final Map<UUID, Long> chaseStart = new HashMap<>();
    public static int speedMultiplier = 20;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ServerLevel overworld = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        playerNurs.entrySet().removeIf(e -> {
            ServerPlayer p = overworld.getServer().getPlayerList().getPlayer(e.getKey());
            if (p == null || !p.isAlive()) return true;
            e.getValue().removeIf(id -> {
                Entity en = overworld.getEntity(id);
                return en == null || !en.isAlive();
            });
            if (e.getValue().isEmpty()) {
                chaseStart.remove(e.getKey());
                if (p.connection != null)
                    p.connection.send(new ClientboundSetTimePacket(overworld.getGameTime(), overworld.getDayTime(), true));
                return true;
            }
            return false;
        });

        if (playerNurs.isEmpty()) return;

        long realDayTime = overworld.getDayTime();
        long realGameTime = overworld.getGameTime();
        for (var entry : playerNurs.entrySet()) {
            ServerPlayer p = overworld.getServer().getPlayerList().getPlayer(entry.getKey());
            if (p == null || p.connection == null) continue;
            long start = chaseStart.get(entry.getKey());
            long perceived = realDayTime + (realGameTime - start) * speedMultiplier;
            p.connection.send(new ClientboundSetTimePacket(realGameTime, perceived, true));
        }
    }

    public static void start(UUID playerId, UUID nurId) {
        if (!playerNurs.containsKey(playerId)) {
            ServerLevel overworld = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getLevel(Level.OVERWORLD);
            if (overworld == null) return;
            chaseStart.put(playerId, overworld.getGameTime());
            playerNurs.put(playerId, new HashSet<>());
        }
        playerNurs.get(playerId).add(nurId);
    }

    public static void stop(UUID playerId, UUID nurId) {
        Set<UUID> nurs = playerNurs.get(playerId);
        if (nurs == null) return;
        nurs.remove(nurId);
        if (nurs.isEmpty()) {
            playerNurs.remove(playerId);
            chaseStart.remove(playerId);
            ServerLevel overworld = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getLevel(Level.OVERWORLD);
            if (overworld == null) return;
            ServerPlayer p = overworld.getServer().getPlayerList().getPlayer(playerId);
            if (p != null && p.connection != null)
                p.connection.send(new ClientboundSetTimePacket(overworld.getGameTime(), overworld.getDayTime(), true));
        }
    }
}
