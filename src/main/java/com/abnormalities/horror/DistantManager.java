package com.abnormalities.horror;

import com.abnormalities.config.AbnormalitiesConfig;
import com.abnormalities.entity.DistantEntity;
import com.abnormalities.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class DistantManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|DistantManager");
    private static final Random RNG = new Random();
    private static final Map<UUID, Long> DESPAWN_COOLDOWN = new HashMap<>();
    private static final Map<UUID, Integer> FLASHBACK_STAGE = new HashMap<>();
    private static final Map<UUID, Integer> FLASHBACK_TICKS = new HashMap<>();
    private static final Map<UUID, Integer> FLASHBACK_GRACE = new HashMap<>();
    private static final Map<UUID, UUID> FLASHBACK_ENTITY = new HashMap<>();

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!AbnormalitiesConfig.DISTANT_ENABLED.get()) {
            DESPAWN_COOLDOWN.clear();
            FLASHBACK_STAGE.clear();
            FLASHBACK_TICKS.clear();
            FLASHBACK_GRACE.clear();
            FLASHBACK_ENTITY.clear();
            return;
        }
        var srv = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        ServerLevel overworld = srv.getLevel(Level.OVERWORLD);
        if (overworld == null) return;
        long now = overworld.getGameTime();

        for (ServerPlayer player : new java.util.ArrayList<>(srv.getPlayerList().getPlayers())) {
            if (player.level().dimension() != Level.OVERWORLD) continue;
            UUID uuid = player.getUUID();

            Long cd = DESPAWN_COOLDOWN.get(uuid);
            if (cd != null && now < cd) continue;
            if (cd != null) DESPAWN_COOLDOWN.remove(uuid);

            boolean hasDistant = false;
            for (DistantEntity d : overworld.getEntitiesOfClass(DistantEntity.class, player.getBoundingBox().inflate(128.0D))) {
                hasDistant = true;
                break;
            }
            if (hasDistant) continue;

            if (player.tickCount % 200 != 0) continue;
            if (overworld.random.nextInt(AbnormalitiesConfig.DISTANT_SPAWN_WEIGHT.get()) != 0) continue;

            spawnDistant(overworld, player);
        }

        for (ServerPlayer player : new java.util.ArrayList<>(srv.getPlayerList().getPlayers())) {
            if (player.level().dimension() != Level.OVERWORLD) continue;
            UUID uuid = player.getUUID();
            Integer stage = FLASHBACK_STAGE.get(uuid);
            if (stage == null) continue;

            UUID entityUuid = FLASHBACK_ENTITY.get(uuid);
            DistantEntity entity = null;
            if (entityUuid != null) {
                for (DistantEntity de : overworld.getEntitiesOfClass(DistantEntity.class, player.getBoundingBox().inflate(128.0D))) {
                    if (de.getUUID().equals(entityUuid)) {
                        entity = de;
                        break;
                    }
                }
            }
            if (entity == null || entity.isRemoved()) {
                LOGGER.info("[DistantManager] {} flashback cancelled (entity gone)", player.getName().getString());
                cancelFlashback(player);
                continue;
            }
            FLASHBACK_GRACE.remove(uuid);

            int ticks = FLASHBACK_TICKS.getOrDefault(uuid, 0) + 1;
            FLASHBACK_TICKS.put(uuid, ticks);

            if (stage == 1 && ticks >= 20) {
                FLASHBACK_STAGE.put(uuid, 2);
                FLASHBACK_TICKS.put(uuid, 0);
                LOGGER.info("[DistantManager] {} flashback stage 1->2 (1.0s, screenshot 1)", player.getName().getString());
                com.abnormalities.AbnormalitiesMod.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                    new com.abnormalities.network.DistantFlashbackPacket(2));
            } else if (stage == 2 && ticks >= 20) {
                FLASHBACK_STAGE.put(uuid, 3);
                FLASHBACK_TICKS.put(uuid, 0);
                LOGGER.info("[DistantManager] {} flashback stage 2->3 (2.0s, screenshot 2)", player.getName().getString());
                com.abnormalities.AbnormalitiesMod.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                    new com.abnormalities.network.DistantFlashbackPacket(3));
            } else if (stage == 3 && ticks >= 20) {
                FLASHBACK_STAGE.put(uuid, 4);
                FLASHBACK_TICKS.put(uuid, 0);
                LOGGER.info("[DistantManager] {} flashback stage 3->4 (3.0s, screenshot 3)", player.getName().getString());
                com.abnormalities.AbnormalitiesMod.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                    new com.abnormalities.network.DistantFlashbackPacket(4));
            } else if (stage == 4 && ticks >= 20) {
                FLASHBACK_STAGE.put(uuid, 5);
                FLASHBACK_TICKS.put(uuid, 0);
                LOGGER.info("[DistantManager] {} flashback stage 4->5 (4.0s, screenshot 4)", player.getName().getString());
                com.abnormalities.AbnormalitiesMod.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                    new com.abnormalities.network.DistantFlashbackPacket(5));
            } else if (stage == 5 && ticks >= 20) {
                FLASHBACK_STAGE.put(uuid, 6);
                FLASHBACK_TICKS.put(uuid, 0);
                LOGGER.info("[DistantManager] {} flashback stage 5->6 (5.0s, screenshot 5)", player.getName().getString());
                com.abnormalities.AbnormalitiesMod.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                    new com.abnormalities.network.DistantFlashbackPacket(6));
            } else if (stage == 6 && ticks >= 20) {
                FLASHBACK_STAGE.remove(uuid);
                FLASHBACK_TICKS.remove(uuid);
                FLASHBACK_GRACE.remove(uuid);
                FLASHBACK_ENTITY.remove(uuid);
                if (entity != null) entity.discard();
                LOGGER.info("[DistantManager] {} flashback complete (6.0s)", player.getName().getString());
                summonNur(player);
                com.abnormalities.AbnormalitiesMod.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                    new com.abnormalities.network.DistantFlashbackPacket(0));
                DESPAWN_COOLDOWN.put(uuid, now + 200);
            }
        }
    }

    private static void cancelFlashback(ServerPlayer player) {
        UUID uuid = player.getUUID();
        FLASHBACK_STAGE.remove(uuid);
        FLASHBACK_TICKS.remove(uuid);
        FLASHBACK_GRACE.remove(uuid);
        UUID entityUuid = FLASHBACK_ENTITY.remove(uuid);
        if (entityUuid != null) {
            var level = player.serverLevel();
            for (DistantEntity de : level.getEntitiesOfClass(DistantEntity.class, player.getBoundingBox().inflate(128.0D))) {
                if (de.getUUID().equals(entityUuid)) {
                    de.discard();
                    break;
                }
            }
        }
        com.abnormalities.AbnormalitiesMod.CHANNEL.send(
            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
            new com.abnormalities.network.DistantFlashbackPacket(0));
    }

    private static void spawnDistant(ServerLevel level, ServerPlayer player) {
        double angle = RNG.nextDouble() * Math.PI * 2;
        double dist = 20.0D + RNG.nextDouble() * 30.0D;
        double sx = player.getX() + Math.cos(angle) * dist;
        double sz = player.getZ() + Math.sin(angle) * dist;
        int sy = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) sx, (int) sz);
        if (level.getBlockState(new BlockPos((int) sx, sy, (int) sz)).getFluidState().is(net.minecraft.tags.FluidTags.WATER)) {
            LOGGER.debug("[DistantManager] heightmap hit water at ({}, {}, {}), scanning up", (int) sx, sy, (int) sz);
            while (sy < level.getMaxBuildHeight() - 2
                    && (level.getBlockState(new BlockPos((int) sx, sy, (int) sz)).getFluidState().is(net.minecraft.tags.FluidTags.WATER)
                        || !level.getBlockState(new BlockPos((int) sx, sy, (int) sz)).isAir())) {
                sy++;
            }
        }
        if (RNG.nextBoolean()) {
            int candidateY = (int) player.getY() + RNG.nextInt(20) - 10;
            candidateY = Math.max(level.getMinBuildHeight() + 2, Math.min(level.getMaxBuildHeight() - 2, candidateY));
            if (!level.getBlockState(new BlockPos((int) sx, candidateY, (int) sz)).getFluidState().is(net.minecraft.tags.FluidTags.WATER)
                    && level.getBlockState(new BlockPos((int) sx, candidateY, (int) sz)).isAir()
                    && level.getBlockState(new BlockPos((int) sx, candidateY + 1, (int) sz)).isAir()) {
                boolean waterBelow = false;
                for (int dy = 1; dy <= 8; dy++) {
                    if (level.getBlockState(new BlockPos((int) sx, candidateY - dy, (int) sz)).getFluidState().is(net.minecraft.tags.FluidTags.WATER)) {
                        waterBelow = true;
                        break;
                    }
                }
                if (!waterBelow) {
                    sy = candidateY;
                }
            }
        }
        sy = Math.max(level.getMinBuildHeight() + 2, Math.min(level.getMaxBuildHeight() - 2, sy));
        BlockPos spawnPos = new BlockPos((int) sx, sy, (int) sz);
        if (!level.getBlockState(spawnPos).isAir() || !level.getBlockState(spawnPos.above()).isAir()) {
            LOGGER.debug("[DistantManager] spawn pos invalid at ({}, {}, {}), aborting", spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());
            return;
        }
        for (int dy = 1; dy <= 8; dy++) {
            if (level.getBlockState(spawnPos.below(dy)).getFluidState().is(net.minecraft.tags.FluidTags.WATER)) {
                LOGGER.debug("[DistantManager] spawn pos over water at ({}, {}, {}), aborting", spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());
                return;
            }
        }
        DistantEntity distant = ModEntities.DISTANT.get().create(level);
        if (distant == null) return;
        distant.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, level.random.nextFloat() * 360, 0);
        distant.setTargetPlayer(player);
        level.addFreshEntity(distant);
    }

    private static void summonNur(ServerPlayer player) {
        com.abnormalities.registry.ModEvents.forceNurSpawn(player);
    }

    public static boolean startFlashbackFor(ServerPlayer player, DistantEntity entity) {
        UUID uuid = player.getUUID();
        if (!com.abnormalities.config.AbnormalitiesConfig.NUR_ENABLED.get()) {
            LOGGER.debug("[DistantManager] {} flashback rejected, nur disabled", player.getName().getString());
            return false;
        }
        if (FLASHBACK_STAGE.containsKey(uuid)) {
            LOGGER.debug("[DistantManager] {} flashback rejected, already active", player.getName().getString());
            return false;
        }
        Long cd = DESPAWN_COOLDOWN.get(uuid);
        if (cd != null && player.serverLevel().getGameTime() < cd) {
            LOGGER.debug("[DistantManager] {} flashback rejected, cooldown active", player.getName().getString());
            return false;
        }
        FLASHBACK_STAGE.put(uuid, 1);
        FLASHBACK_TICKS.put(uuid, 0);
        FLASHBACK_GRACE.remove(uuid);
        FLASHBACK_ENTITY.put(uuid, entity.getUUID());
        LOGGER.info("[DistantManager] {} flashback started", player.getName().getString());
        return true;
    }

    public static boolean isFlashbackActive(ServerPlayer player) {
        return FLASHBACK_STAGE.containsKey(player.getUUID());
    }

    public static void onProximityDespawn(ServerPlayer player) {
        DESPAWN_COOLDOWN.put(player.getUUID(), player.serverLevel().getGameTime() + 400);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() == null) return;
        UUID uuid = event.getEntity().getUUID();
        UUID entityUuid = FLASHBACK_ENTITY.remove(uuid);
        if (entityUuid != null) {
            var level = event.getEntity().level();
            for (DistantEntity de : level.getEntitiesOfClass(DistantEntity.class, event.getEntity().getBoundingBox().inflate(128.0D))) {
                if (de.getUUID().equals(entityUuid)) {
                    de.discard();
                    break;
                }
            }
        }
        FLASHBACK_STAGE.remove(uuid);
        FLASHBACK_TICKS.remove(uuid);
        FLASHBACK_GRACE.remove(uuid);
        DESPAWN_COOLDOWN.remove(uuid);
    }

    public static void forceSpawn(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        spawnDistant(level, player);
    }

    public static void forceCircle(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        int radius = 20 + RNG.nextInt(11);
        int count = 12 + RNG.nextInt(7);
        LOGGER.info("[DistantManager] {} circle radius={} count={}", player.getName().getString(), radius, count);
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2 * i) / count;
            double sx = player.getX() + Math.cos(angle) * radius;
            double sz = player.getZ() + Math.sin(angle) * radius;
            int sy = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) sx, (int) sz);
            sy = Math.max(level.getMinBuildHeight() + 2, Math.min(level.getMaxBuildHeight() - 2, sy));
            BlockPos spawnPos = new BlockPos((int) sx, sy, (int) sz);
            if (!level.getBlockState(spawnPos).isAir() || !level.getBlockState(spawnPos.above()).isAir()) continue;
            boolean waterFound = false;
            for (int dy = 1; dy <= 8; dy++) {
                if (level.getBlockState(spawnPos.below(dy)).getFluidState().is(net.minecraft.tags.FluidTags.WATER)) {
                    waterFound = true;
                    break;
                }
            }
            if (waterFound) continue;
            DistantEntity distant = ModEntities.DISTANT.get().create(level);
            if (distant == null) continue;
            distant.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, level.random.nextFloat() * 360, 0);
            distant.setTargetPlayer(player);
            level.addFreshEntity(distant);
        }
    }
}