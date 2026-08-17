package com.abnormalities.horror;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParkourTracker {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|Parkour");

    private static final Map<UUID, ParkourState> STATES = new HashMap<>();

    private static class ParkourState {
        int jumpCount = 0;
        long windowStart = 0;
        double takeoffX = 0;
        double takeoffZ = 0;
        boolean wasOnGround = true;
        boolean isParkouring = false;
        int parkourTicks = 0;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;
        UUID uuid = player.getUUID();
        ParkourState s = STATES.computeIfAbsent(uuid, k -> new ParkourState());

        boolean onGround = player.onGround();
        long now = player.level().getGameTime();

        if (!onGround && s.wasOnGround && player.getDeltaMovement().y > 0) {
            s.takeoffX = player.getX();
            s.takeoffZ = player.getZ();

            if (s.windowStart == 0) s.windowStart = now;
            if (now - s.windowStart > 60) {
                s.jumpCount = 0;
                s.windowStart = now;
            }

            if (player.isSprinting() && player.zza > 0) {
                s.jumpCount++;
            }

            if (s.jumpCount >= 3) {
                s.isParkouring = true;
                s.parkourTicks = 100;
            }
        }

        if (!onGround) {
            s.wasOnGround = false;
        } else if (!s.wasOnGround) {
            s.wasOnGround = true;
            double dx = player.getX() - s.takeoffX;
            double dz = player.getZ() - s.takeoffZ;
            double horizontalDist = Math.sqrt(dx * dx + dz * dz);
            if (horizontalDist > 5.0) {
                s.isParkouring = true;
                s.parkourTicks = 100;
            }
        }

        if (s.parkourTicks > 0) {
            s.parkourTicks--;
            if (s.parkourTicks <= 0) {
                s.isParkouring = false;
                s.jumpCount = 0;
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        STATES.remove(event.getEntity().getUUID());
    }

    public static boolean isParkouring(UUID uuid) {
        ParkourState s = STATES.get(uuid);
        return s != null && s.isParkouring;
    }
}
