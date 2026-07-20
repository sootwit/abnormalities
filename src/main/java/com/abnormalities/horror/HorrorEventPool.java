package com.abnormalities.horror;

import com.abnormalities.ReputationManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;
import java.util.stream.Collectors;

public class HorrorEventPool {
    private static final List<AbstractHorrorEvent> EVENTS = new ArrayList<>();
    private static final Map<UUID, Map<String, Long>> COOLDOWNS = new HashMap<>();
    private static final Map<UUID, AbstractHorrorEvent> ONGOING = new HashMap<>();
    private static final Random RNG = new Random();

    public static void register(AbstractHorrorEvent event) {
        EVENTS.add(event);
    }

    public static List<AbstractHorrorEvent> getRegistered() {
        return Collections.unmodifiableList(EVENTS);
    }

    public static boolean isOnCooldown(ServerPlayer player, AbstractHorrorEvent event) {
        Map<String, Long> pcd = COOLDOWNS.get(player.getUUID());
        if (pcd == null) return false;
        Long until = pcd.get(event.getName());
        if (until == null) return false;
        return player.level().getGameTime() < until;
    }

    public static void setCooldown(ServerPlayer player, AbstractHorrorEvent event) {
        COOLDOWNS.computeIfAbsent(player.getUUID(), k -> new HashMap<>())
            .put(event.getName(), player.level().getGameTime() + event.getCooldownTicks());
    }

    public static AbstractHorrorEvent getOngoing(ServerPlayer player) {
        return ONGOING.get(player.getUUID());
    }

    public static boolean hasOngoing(ServerPlayer player) {
        return ONGOING.containsKey(player.getUUID());
    }

    public static void setOngoing(ServerPlayer player, AbstractHorrorEvent event) {
        if (event != null && event.allowsOngoing()) {
            ONGOING.put(player.getUUID(), event);
        } else {
            ONGOING.remove(player.getUUID());
        }
    }

    public static void clearOngoing(ServerPlayer player) {
        AbstractHorrorEvent cur = ONGOING.remove(player.getUUID());
        if (cur != null) cur.onCleanup(player);
    }

    public static AbstractHorrorEvent selectEvent(ServerPlayer player, long currentTick) {
        List<AbstractHorrorEvent> eligible = EVENTS.stream()
            .filter(e -> !isOnCooldown(player, e))
            .filter(e -> !hasOngoing(player) || (e.allowsOngoing() && e != ONGOING.get(player.getUUID())))
            .filter(e -> e.canTrigger(player, currentTick))
            .filter(e -> {
                int rep = ReputationManager.getRep(player);
                return rep >= e.getMinRep() && rep <= e.getMaxRep();
            })
            .collect(Collectors.toList());

        if (eligible.isEmpty()) return null;

        double totalWeight = 0;
        double[] weights = new double[eligible.size()];
        for (int i = 0; i < eligible.size(); i++) {
            AbstractHorrorEvent e = eligible.get(i);
            double mult = ReputationManager.getWeightMultiplier(player, e.getHostilityFactor());
            double w = e.getBaseWeight() * mult;
            weights[i] = w;
            totalWeight += w;
        }

        if (totalWeight <= 0) return null;

        double roll = RNG.nextDouble() * totalWeight;
        double cumulative = 0;
        for (int i = 0; i < eligible.size(); i++) {
            cumulative += weights[i];
            if (roll <= cumulative) return eligible.get(i);
        }

        return eligible.get(eligible.size() - 1);
    }

    public static void fireEvent(ServerPlayer player, AbstractHorrorEvent event) {
        if (event == null) return;
        setCooldown(player, event);
        if (event.allowsOngoing()) setOngoing(player, event);
        event.execute(player);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ServerLevel overworld = ServerLifecycleHooks.getCurrentServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return;
        long gt = overworld.getGameTime();

        for (ServerPlayer player : overworld.getServer().getPlayerList().getPlayers()) {
            if (player.tickCount % 100 != 0) continue;
            if (overworld.random.nextInt(200) != 0) continue;

            AbstractHorrorEvent selected = selectEvent(player, gt);
            if (selected != null) {
                fireEvent(player, selected);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.level().isClientSide) return;
        if (!(event.player instanceof ServerPlayer sp)) return;

        AbstractHorrorEvent ongoing = ONGOING.get(sp.getUUID());
        if (ongoing != null) {
            ongoing.onPlayerTick(sp);
        }
    }

    public static void cleanup(ServerPlayer player) {
        COOLDOWNS.remove(player.getUUID());
        clearOngoing(player);
    }

    @SubscribeEvent
    public static void onPlayerLogout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            cleanup(sp);
        }
    }
}
