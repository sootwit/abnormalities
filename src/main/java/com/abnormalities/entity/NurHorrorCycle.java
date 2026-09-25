package com.abnormalities.entity;

import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class NurHorrorCycle {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|NurHorrorCycle");
    private static final Map<UUID, Set<UUID>> playerNurs = new HashMap<>();
    private static final Map<UUID, Long> chaseStart = new HashMap<>();
    private static final Map<UUID, Long> originalDayTime = new HashMap<>();
    public static int speedMultiplier = 100;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ServerLevel overworld = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        for (UUID playerId : Set.copyOf(playerNurs.keySet())) {
            ServerPlayer p = overworld.getServer().getPlayerList().getPlayer(playerId);
            if (p == null || !p.isAlive()) {
                playerNurs.remove(playerId);
                chaseStart.remove(playerId);
                originalDayTime.remove(playerId);
                continue;
            }
            Set<UUID> nurs = playerNurs.get(playerId);
            if (nurs == null) continue;
            nurs.removeIf(id -> {
                Entity en = overworld.getEntity(id);
                return en == null || !en.isAlive();
            });
            if (nurs.isEmpty()) {
                playerNurs.remove(playerId);
                chaseStart.remove(playerId);
                Long orig = originalDayTime.remove(playerId);
                if (p.connection != null)
                    p.connection.send(new ClientboundSetTimePacket(overworld.getGameTime(), orig != null ? orig : overworld.getDayTime(), true));
            }
        }

        if (playerNurs.isEmpty()) return;

        long realGameTime = overworld.getGameTime();
        for (var entry : playerNurs.entrySet()) {
            ServerPlayer p = overworld.getServer().getPlayerList().getPlayer(entry.getKey());
            if (p == null || p.connection == null) continue;
            long start = chaseStart.get(entry.getKey());
            long orig = originalDayTime.get(entry.getKey());
            long perceived = orig + (realGameTime - start) * speedMultiplier;
            p.connection.send(new ClientboundSetTimePacket(realGameTime, perceived, true));
        }
    }

    public static void start(UUID playerId, UUID nurId) {
        if (!playerNurs.containsKey(playerId)) {
            ServerLevel overworld = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getLevel(Level.OVERWORLD);
            if (overworld == null) return;
            originalDayTime.put(playerId, overworld.getDayTime());
            chaseStart.put(playerId, overworld.getGameTime());
            playerNurs.put(playerId, new HashSet<>());
            LOGGER.info("[NurHorrorCycle] time acceleration started for player {}", playerId);
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
            Long orig = originalDayTime.remove(playerId);
            LOGGER.info("[NurHorrorCycle] time acceleration stopped for player {}", playerId);
            var srv = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (srv == null) return;
            ServerLevel overworld = srv.getLevel(Level.OVERWORLD);
            if (overworld == null) return;
            ServerPlayer p = overworld.getServer().getPlayerList().getPlayer(playerId);
            if (p != null && p.connection != null)
                p.connection.send(new ClientboundSetTimePacket(overworld.getGameTime(), orig != null ? orig : overworld.getDayTime(), true));
        }
    }
}
