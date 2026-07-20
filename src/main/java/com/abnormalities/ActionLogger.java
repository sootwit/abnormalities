package com.abnormalities;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;
import java.util.stream.Collectors;

public class ActionLogger {
    public static class ActionEntry {
        public final long gameTime;
        public final String type;
        public final String detail;

        public ActionEntry(long gameTime, String type, String detail) {
            this.gameTime = gameTime;
            this.type = type;
            this.detail = detail;
        }
    }

    private static final int RETENTION_TICKS = 36000;
    private static final Map<UUID, Deque<ActionEntry>> LOGS = new HashMap<>();
    private static final Map<UUID, long[]> LAST_POS = new HashMap<>();

    public static void log(Player player, String type, String detail) {
        UUID uuid = player.getUUID();
        Deque<ActionEntry> log = LOGS.computeIfAbsent(uuid, k -> new ArrayDeque<>());
        long gt = 0;
        if (player.level() != null) gt = player.level().getGameTime();
        log.addLast(new ActionEntry(gt, type, detail));
        prune(log, gt);
    }

    public static List<ActionEntry> getRecent(Player player, int secondsBack) {
        Deque<ActionEntry> log = LOGS.get(player.getUUID());
        if (log == null) return List.of();
        long gt = player.level() != null ? player.level().getGameTime() : 0;
        long threshold = gt - secondsBack * 20L;
        return log.stream().filter(e -> e.gameTime >= threshold).collect(Collectors.toList());
    }

    public static List<ActionEntry> getSince(Player player, int minutesBack) {
        return getRecent(player, minutesBack * 60);
    }

    public static List<ActionEntry> getByType(Player player, String type) {
        Deque<ActionEntry> log = LOGS.get(player.getUUID());
        if (log == null) return List.of();
        long gt = player.level() != null ? player.level().getGameTime() : 0;
        return log.stream().filter(e -> e.type.equals(type) && gt - e.gameTime < RETENTION_TICKS).collect(Collectors.toList());
    }

    public static ActionEntry getOldestAction(Player player, int minutesBackMin) {
        Deque<ActionEntry> log = LOGS.get(player.getUUID());
        if (log == null || log.isEmpty()) return null;
        long gt = player.level() != null ? player.level().getGameTime() : 0;
        long minTime = gt - minutesBackMin * 60L * 20L;
        long maxTime = gt - 5 * 20L;
        return log.stream()
            .filter(e -> e.gameTime >= minTime && e.gameTime <= maxTime)
            .findFirst().orElse(null);
    }

    private static void prune(Deque<ActionEntry> log, long gameTime) {
        while (!log.isEmpty() && gameTime - log.peekFirst().gameTime > RETENTION_TICKS) {
            log.pollFirst();
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        var pos = event.getPos();
        log(player, "break", pos.getX() + " " + pos.getY() + " " + pos.getZ() + " " + event.getState().getBlock().getDescriptionId());
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var pos = event.getPos();
        log(player, "place", pos.getX() + " " + pos.getY() + " " + pos.getZ() + " " + event.getState().getBlock().getDescriptionId());
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead instanceof Player) return;
        if (dead.level().isClientSide) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        log(player, "kill", dead.getType().getDescriptionId() + " at " + (int)dead.getX() + " " + (int)dead.getY() + " " + (int)dead.getZ());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.level().isClientSide) return;
        Player player = event.player;
        if (player.tickCount % 10 != 0) return;

        long[] last = LAST_POS.get(player.getUUID());
        double dx = last != null ? player.getX() - last[0] : 0;
        double dz = last != null ? player.getZ() - last[2] : 0;
        double moved = Math.sqrt(dx * dx + dz * dz);
        if (moved > 0.5) {
            log(player, "move", String.format("%.0f %.0f %.0f", player.getX(), player.getY(), player.getZ()));
            LAST_POS.put(player.getUUID(), new long[]{(long)player.getX(), (long)player.getY(), (long)player.getZ()});
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() != null) {
            LOGS.remove(event.getEntity().getUUID());
            LAST_POS.remove(event.getEntity().getUUID());
        }
    }
}
