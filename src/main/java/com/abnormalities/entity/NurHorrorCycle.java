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
    public static void onWorldUnload(net.minecraftforge.event.level.LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        if (sl.dimension() != Level.OVERWORLD) return;
        var srv = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (srv != null) {
            for (var entry : playerNurs.entrySet()) {
                ServerPlayer p = srv.getPlayerList().getPlayer(entry.getKey());
                if (p != null && p.connection != null) {
                    Long orig = originalDayTime.get(entry.getKey());
                    p.connection.send(new ClientboundSetTimePacket(sl.getGameTime(), orig != null ? orig : sl.getDayTime(), true));
                }
            }
        }
        playerNurs.clear();
        chaseStart.clear();
        originalDayTime.clear();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var srv = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        ServerLevel overworld = srv.getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        for (UUID playerId : Set.copyOf(playerNurs.keySet())) {
            ServerPlayer p = overworld.getServer().getPlayerList().getPlayer(playerId);
            if (p == null || !p.isAlive() || p.isCreative() || p.isSpectator()) {
                playerNurs.remove(playerId);
                chaseStart.remove(playerId);
                Long orig = originalDayTime.remove(playerId);
                if (p != null && p.connection != null)
                    p.connection.send(new ClientboundSetTimePacket(overworld.getGameTime(), orig != null ? orig : overworld.getDayTime(), true));
                continue;
            }
            Set<UUID> nurs = playerNurs.get(playerId);
            if (nurs == null) continue;
            Long cap = chaseStart.get(playerId);
            if (cap != null && overworld.getGameTime() - cap > 12000L) {
                playerNurs.remove(playerId);
                chaseStart.remove(playerId);
                Long origCap = originalDayTime.remove(playerId);
                if (p.connection != null)
                    p.connection.send(new ClientboundSetTimePacket(overworld.getGameTime(), origCap != null ? origCap : overworld.getDayTime(), true));
                continue;
            }
            nurs.removeIf(id -> {
                for (ServerLevel dim : srv.getAllLevels()) {
                    var en = dim.getEntity(id);
                    if (en != null && en.isAlive()) return false;
                }
                return true;
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
        for (UUID pid : Set.copyOf(playerNurs.keySet())) {
            ServerPlayer p = overworld.getServer().getPlayerList().getPlayer(pid);
            if (p == null || p.connection == null) continue;
            Long start = chaseStart.get(pid);
            Long orig = originalDayTime.get(pid);
            if (start == null || orig == null) {
                playerNurs.remove(pid);
                chaseStart.remove(pid);
                originalDayTime.remove(pid);
                if (p.connection != null)
                    p.connection.send(new ClientboundSetTimePacket(overworld.getGameTime(), overworld.getDayTime(), true));
                continue;
            }
            long perceived = orig + (realGameTime - start) * speedMultiplier;
            p.connection.send(new ClientboundSetTimePacket(realGameTime, perceived, true));
        }
    }

    public static void start(UUID playerId, UUID nurId) {
        if (!playerNurs.containsKey(playerId)) {
            var srv = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (srv == null) return;
            ServerLevel overworld = srv.getLevel(Level.OVERWORLD);
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
