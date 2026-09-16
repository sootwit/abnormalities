package com.abnormalities.sign;

import com.abnormalities.config.AbnormalitiesConfig;
import com.abnormalities.network.SignTransitionPacket;
import com.abnormalities.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SignDimension {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|SignDimension");
    public static final ResourceKey<Level> LEVEL_KEY = ResourceKey.create(
            net.minecraft.core.registries.Registries.DIMENSION,
            new ResourceLocation("abnormalities", "sign")
    );
    private static final Map<UUID, BlockPos> PRE_ENTRY_POS = new HashMap<>();
    private static final Map<UUID, Long> ENTRY_TIME = new HashMap<>();
    private static int spawnTickAccum = 0;

    public static void teleportToSign(ServerPlayer player, BlockPos overworldPos) {
        if (player.level().isClientSide) return;
        if (!com.abnormalities.config.AbnormalitiesConfig.SIGN_ENABLED.get()) return;
        var srv = player.getServer();
        if (srv == null) return;
        ServerLevel signLevel = srv.getLevel(LEVEL_KEY);
        if (signLevel == null) return;
        PRE_ENTRY_POS.put(player.getUUID(), overworldPos);
        ENTRY_TIME.put(player.getUUID(), signLevel.getGameTime());
        int spawnX = overworldPos.getX() % 256;
        int spawnZ = overworldPos.getZ() % 256;
        int spawnY = signLevel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawnX, spawnZ);
        if (spawnY < signLevel.getMinBuildHeight() + 10) {
            int origX = spawnX;
            int origZ = spawnZ;
            int bestY = 64;
            int bestDist = Integer.MAX_VALUE;
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    int hx = origX + dx;
                    int hz = origZ + dz;
                    int hy = signLevel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, hx, hz);
                    if (hy >= signLevel.getMinBuildHeight() + 10) {
                        int d = dx * dx + dz * dz;
                        if (d < bestDist) {
                            bestDist = d;
                            bestY = hy;
                            spawnX = hx;
                            spawnZ = hz;
                        }
                    }
                }
            }
            spawnY = bestY;
        }
        BlockPos tpPos = new BlockPos(spawnX, spawnY, spawnZ);
        while (tpPos.getY() < signLevel.getMaxBuildHeight() && !signLevel.getBlockState(tpPos).canBeReplaced()) {
            tpPos = tpPos.above();
        }
        if (tpPos.getY() >= signLevel.getMaxBuildHeight()) {
            tpPos = new BlockPos(spawnX, spawnY, spawnZ);
        }
        player.teleportTo(signLevel, tpPos.getX() + 0.5, tpPos.getY(), tpPos.getZ() + 0.5, player.getYRot(), player.getXRot());
        player.fallDistance = 0.0F;
        player.hurtMarked = true;
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));
        LOGGER.info("[SignDimension] {} teleported to sign dimension at {}", player.getName().getString(), tpPos);
        com.abnormalities.AbnormalitiesMod.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                new SignTransitionPacket()
        );
    }

    public static void returnFromSign(ServerPlayer player) {
        if (player.level().isClientSide) return;
        var srv = player.getServer();
        if (srv == null) return;
        UUID uuid = player.getUUID();
        BlockPos returnPos = PRE_ENTRY_POS.remove(uuid);
        ENTRY_TIME.remove(uuid);
        if (returnPos == null) {
            ServerLevel overworld = srv.getLevel(Level.OVERWORLD);
            if (overworld != null) {
                BlockPos bedPos = player.getRespawnPosition();
                if (bedPos != null) {
                    returnPos = bedPos;
                } else {
                    returnPos = overworld.getSharedSpawnPos();
                }
            }
        }
        if (returnPos != null) {
            ServerLevel targetLevel = srv.getLevel(Level.OVERWORLD);
            if (targetLevel != null) {
                int y = targetLevel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, returnPos.getX(), returnPos.getZ());
                BlockPos tpPos = new BlockPos(returnPos.getX(), y + 1, returnPos.getZ());
                player.teleportTo(targetLevel, tpPos.getX() + 0.5, tpPos.getY(), tpPos.getZ() + 0.5, player.getYRot(), player.getXRot());
                player.fallDistance = 0.0F;
                player.hurtMarked = true;
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));
                LOGGER.info("[SignDimension] {} returned from sign dimension to {}", player.getName().getString(), tpPos);
                com.abnormalities.AbnormalitiesMod.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                        new SignTransitionPacket()
                );
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var srv = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        ServerLevel signLevel = srv.getLevel(LEVEL_KEY);
        if (signLevel == null) return;
        long now = signLevel.getGameTime();
        long timerTicks = (long) AbnormalitiesConfig.SIGN_TIMER_MINUTES.get() * 60L * 20L;
        for (ServerPlayer player : new java.util.ArrayList<>(srv.getPlayerList().getPlayers())) {
            if (player.level().dimension() != LEVEL_KEY) continue;
            Long entry = ENTRY_TIME.get(player.getUUID());
            if (entry == null) {
                entry = now;
                ENTRY_TIME.put(player.getUUID(), now);
            }
            if (now - entry >= timerTicks) {
                LOGGER.info("[SignDimension] {} timer expired, returning", player.getName().getString());
                returnFromSign(player);
                continue;
            }
            BlockPos ppos = player.blockPosition();
            if (ppos.getY() <= signLevel.getMinBuildHeight() + 2) {
                LOGGER.info("[SignDimension] {} fell into void, returning", player.getName().getString());
                returnFromSign(player);
            }
        }
        int interval = AbnormalitiesConfig.SIGN_HIM_SPAWN_INTERVAL.get();
        int batchSize = AbnormalitiesConfig.SIGN_HIM_BATCH_SIZE.get();
        spawnTickAccum++;
        if (spawnTickAccum >= interval) {
            spawnTickAccum = 0;
            var allHimSigns = signLevel.getEntitiesOfClass(HimSignEntity.class, new net.minecraft.world.phys.AABB(-32000, -64, -32000, 32000, 320, 32000));
            if (allHimSigns.size() < 20) {
                for (ServerPlayer player : srv.getPlayerList().getPlayers()) {
                    if (player.level().dimension() != LEVEL_KEY) continue;
                    var nearby = signLevel.getEntitiesOfClass(HimSignEntity.class, player.getBoundingBox().inflate(64.0D));
                    if (nearby.size() >= 5) continue;
                    for (int i = 0; i < batchSize; i++) {
                        double angle = signLevel.random.nextDouble() * Math.PI * 2;
                        double dist = 16.0 + signLevel.random.nextDouble() * 24.0;
                        double sx = player.getX() + Math.cos(angle) * dist;
                        double sz = player.getZ() + Math.sin(angle) * dist;
                        int sy = signLevel.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) sx, (int) sz);
                        if (sy < signLevel.getMinBuildHeight() + 5) sy = 64;
                        HimSignEntity himSign = ModEntities.HIM_SIGN.get().create(signLevel);
                        if (himSign != null) {
                            himSign.moveTo(sx + 0.5, sy + 1, sz + 0.5, signLevel.random.nextFloat() * 360, 0);
                            signLevel.addFreshEntity(himSign);
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.level().dimension() == LEVEL_KEY) {
            ENTRY_TIME.putIfAbsent(sp.getUUID(), sp.level().getGameTime());
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        UUID uuid = sp.getUUID();
        if (sp.level().dimension() != LEVEL_KEY) {
            PRE_ENTRY_POS.remove(uuid);
            ENTRY_TIME.remove(uuid);
        }
    }

    public static boolean isInSignDimension(ServerPlayer player) {
        return player.level().dimension() == LEVEL_KEY;
    }
}
