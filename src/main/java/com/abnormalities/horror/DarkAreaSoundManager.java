package com.abnormalities.horror;

import com.abnormalities.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class DarkAreaSoundManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|DarkArea");
    private static final Random RNG = new Random();
    private static final Map<UUID, Long> NEXT_THUMP = new HashMap<>();
    private static final Map<UUID, Long> NEXT_DOOR = new HashMap<>();
    private static final Map<UUID, Boolean> WAS_DARK = new HashMap<>();

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var srv = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        ServerLevel overworld = srv.getLevel(Level.OVERWORLD);
        if (overworld == null) return;
        long now = overworld.getGameTime();

        for (ServerPlayer player : new java.util.ArrayList<>(srv.getPlayerList().getPlayers())) {
            if (player.level().dimension() != Level.OVERWORLD) continue;
            if (player.tickCount % 20 != 0) continue;

            BlockPos pos = player.blockPosition();
            int blockLight = player.level().getBrightness(LightLayer.BLOCK, pos);
            int skyLight = player.level().getBrightness(LightLayer.SKY, pos);
            boolean dark = blockLight < 5 && skyLight < 5 && !player.level().canSeeSky(pos);

            if (!dark) {
                WAS_DARK.put(player.getUUID(), false);
                continue;
            }
            Boolean wasDark = WAS_DARK.get(player.getUUID());
            if (wasDark == null || !wasDark) {
                LOGGER.debug("[DarkArea] {} entered dark area at ({}, {}, {})", player.getName().getString(), pos.getX(), pos.getY(), pos.getZ());
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                    net.minecraft.core.Holder.direct(ModSounds.LOWFREQ.get()), SoundSource.MASTER,
                    player.getX(), player.getY(), player.getZ(), 0.5f, 1.0f, 0));
            }
            WAS_DARK.put(player.getUUID(), true);

            Long nt = NEXT_THUMP.get(player.getUUID());
            if (nt == null) {
                NEXT_THUMP.put(player.getUUID(), now + 600 + RNG.nextInt(600));
            } else if (now >= nt) {
                double ox = (RNG.nextDouble() - 0.5) * 12;
                double oz = (RNG.nextDouble() - 0.5) * 12;
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                    net.minecraft.core.Holder.direct(ModSounds.THUMP.get()), SoundSource.MASTER,
                    player.getX() + ox, player.getY(), player.getZ() + oz,
                    0.8f, 0.5f + RNG.nextFloat() * 0.5f, 0));
                NEXT_THUMP.put(player.getUUID(), now + 800 + RNG.nextInt(800));
            }

            Long nd = NEXT_DOOR.get(player.getUUID());
            if (nd == null) {
                NEXT_DOOR.put(player.getUUID(), now + 4000 + RNG.nextInt(4000));
            } else if (now >= nd) {
                double ox = (RNG.nextDouble() - 0.5) * 16;
                double oz = (RNG.nextDouble() - 0.5) * 16;
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                    net.minecraft.core.Holder.direct(net.minecraft.sounds.SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR), SoundSource.MASTER,
                    player.getX() + ox, player.getY(), player.getZ() + oz,
                    0.6f, 0.8f + RNG.nextFloat() * 0.4f, 0));
                LOGGER.debug("[DarkArea] {} zombie door sound at ({}, {}, {})", player.getName().getString(), (int)(player.getX() + ox), (int)player.getY(), (int)(player.getZ() + oz));
                NEXT_DOOR.put(player.getUUID(), now + 4000 + RNG.nextInt(4000));
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() == null) return;
        UUID uuid = event.getEntity().getUUID();
        NEXT_THUMP.remove(uuid);
        NEXT_DOOR.remove(uuid);
        WAS_DARK.remove(uuid);
    }

    public static void forceDarkArea(ServerPlayer player) {
        if (player.connection == null) return;
        LOGGER.info("[DarkArea] {} forced dark area event", player.getName().getString());
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
            net.minecraft.core.Holder.direct(ModSounds.LOWFREQ.get()), SoundSource.MASTER,
            player.getX(), player.getY(), player.getZ(), 0.5f, 1.0f, 0));
        double ox = (RNG.nextDouble() - 0.5) * 12;
        double oz = (RNG.nextDouble() - 0.5) * 12;
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
            net.minecraft.core.Holder.direct(ModSounds.THUMP.get()), SoundSource.MASTER,
            player.getX() + ox, player.getY(), player.getZ() + oz,
            0.8f, 0.5f + RNG.nextFloat() * 0.5f, 0));
    }
}