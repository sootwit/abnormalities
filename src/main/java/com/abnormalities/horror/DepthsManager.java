package com.abnormalities.horror;

import com.abnormalities.config.AbnormalitiesConfig;
import com.abnormalities.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class DepthsManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|Depths");
    private static final Random RNG = new Random();
    private static final Map<UUID, DepthsState> ACTIVE = new HashMap<>();
    private static final Map<UUID, Map<BlockPos, BlockState>> SAVED_BLOCKS = new HashMap<>();
    private static final Map<UUID, Long> NEXT_CHECK = new HashMap<>();

    private static class DepthsState {
        int pullTicks = 0;
        boolean noisePlayed = false;
        BlockPos preDepthsPos;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!AbnormalitiesConfig.DEPTHS_ENABLED.get()) return;
        var srv = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        ServerLevel overworld = srv.getLevel(Level.OVERWORLD);
        if (overworld == null) return;
        long now = overworld.getGameTime();

        for (ServerPlayer player : new java.util.ArrayList<>(srv.getPlayerList().getPlayers())) {
            if (player.level().dimension() != Level.OVERWORLD) continue;
            UUID uuid = player.getUUID();
            DepthsState state = ACTIVE.get(uuid);

            if (state == null) {
                if (player.tickCount % 20 != 0) continue;
                Long next = NEXT_CHECK.get(uuid);
                if (next != null && now < next) continue;
                NEXT_CHECK.put(uuid, now + 200);
                boolean inWater = player.isInWater() || player.getVehicle() instanceof Boat;
                if (inWater && isWaterBiome(player)) {
                    if (overworld.random.nextInt(AbnormalitiesConfig.DEPTHS_CHANCE.get()) == 0) {
                        startDepths(player);
                    }
                }
                continue;
            }

            tickDepths(player, state, overworld, now);
        }
    }

    private static boolean isWaterBiome(ServerPlayer player) {
        var holder = player.level().getBiome(player.blockPosition());
        var key = holder.unwrapKey();
        if (key.isEmpty()) return false;
        String id = key.get().location().toString();
        return id.contains("ocean") || id.contains("river") || id.contains("sea") || id.contains("deep");
    }

    private static void startDepths(ServerPlayer player) {
        UUID uuid = player.getUUID();
        DepthsState state = new DepthsState();
        state.preDepthsPos = player.blockPosition();
        ACTIVE.put(uuid, state);
        LOGGER.info("[Depths] {} depths started at ({}, {}, {})", player.getName().getString(), (int)player.getX(), (int)player.getY(), (int)player.getZ());
        removeConnectedWaterFloor(player, uuid);
        com.abnormalities.AbnormalitiesMod.CHANNEL.send(
            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
            new com.abnormalities.network.DepthsPacket(true));
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
            net.minecraft.core.Holder.direct(ModSounds.HEARTBEAT_SOUND.get()), SoundSource.MASTER,
            player.getX(), player.getY(), player.getZ(), 0.5f, 0.3f, 0));
    }

    private static void removeConnectedWaterFloor(ServerPlayer player, UUID uuid) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos start = player.blockPosition();
        if (!level.getBlockState(start).getFluidState().is(net.minecraft.tags.FluidTags.WATER)) {
            BlockPos below = start.below();
            if (level.getBlockState(below).getFluidState().is(net.minecraft.tags.FluidTags.WATER)) {
                start = below;
            }
        }
        Set<BlockPos> waterBlocks = floodFillWater(level, start);
        for (BlockPos waterPos : waterBlocks) {
            BlockPos below = waterPos.below();
            while (below.getY() >= level.getMinBuildHeight()) {
                BlockState state = level.getBlockState(below);
                if (state.isAir()) break;
                if (state.is(Blocks.BEDROCK) && !AbnormalitiesConfig.DEPTHS_BREAK_BEDROCK.get()) break;
                SAVED_BLOCKS.computeIfAbsent(uuid, k -> new HashMap<>()).put(below.immutable(), state);
                level.setBlock(below, Blocks.AIR.defaultBlockState(), 2);
                below = below.below();
            }
        }
    }

    private static Set<BlockPos> floodFillWater(ServerLevel level, BlockPos start) {
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        int maxBlocks = 4096;
        while (!queue.isEmpty() && visited.size() < maxBlocks) {
            BlockPos pos = queue.poll();
            if (visited.contains(pos)) continue;
            if (!level.isLoaded(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (!state.getFluidState().is(net.minecraft.tags.FluidTags.WATER)) continue;
            visited.add(pos);
            for (var dir : net.minecraft.core.Direction.values()) {
                BlockPos next = pos.relative(dir);
                if (!visited.contains(next)) queue.add(next);
            }
        }
        return visited;
    }

    private static void tickDepths(ServerPlayer player, DepthsState state, ServerLevel level, long now) {
        boolean inWater = player.isInWater() || player.getVehicle() instanceof Boat;
        if (!inWater) {
            endDepths(player, false);
            return;
        }
        if (player.getVehicle() instanceof Boat boat) {
            boat.discard();
            player.stopRiding();
        }
        state.pullTicks++;
        double pull = 0.3D + state.pullTicks * 0.02D;
        player.setDeltaMovement(player.getDeltaMovement().x, -pull, player.getDeltaMovement().z);
        player.hurtMarked = true;
        player.fallDistance += 1.0F;

        if (state.pullTicks > 100 && !state.noisePlayed) {
            state.noisePlayed = true;
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                net.minecraft.core.Holder.direct(ModSounds.THUMP.get()), SoundSource.MASTER,
                player.getX(), player.getY(), player.getZ(), 2.0f, 0.1f, 0));
        }

        if (player.getY() <= level.getMinBuildHeight() + 2) {
            endDepths(player, true);
        }
    }

    private static void endDepths(ServerPlayer player, boolean kick) {
        UUID uuid = player.getUUID();
        ACTIVE.remove(uuid);
        NEXT_CHECK.put(uuid, player.level().getGameTime() + 12000);
        LOGGER.info("[Depths] {} depths ended kick={}", player.getName().getString(), kick);
        com.abnormalities.AbnormalitiesMod.CHANNEL.send(
            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
            new com.abnormalities.network.DepthsPacket(false));
        restorePlayer(uuid);
        if (kick && player.connection != null) {
            player.connection.disconnect(net.minecraft.network.chat.Component.literal("[Forge Error]: depths"));
        }
    }

    public static void forceDepths(ServerPlayer player) {
        if (player.level().isClientSide) return;
        startDepths(player);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        DepthsState state = ACTIVE.remove(sp.getUUID());
        com.abnormalities.AbnormalitiesMod.CHANNEL.send(
            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp),
            new com.abnormalities.network.DepthsPacket(false));
        if (state != null && state.preDepthsPos != null) {
            ServerLevel overworld = sp.getServer() != null ? sp.getServer().getLevel(Level.OVERWORLD) : null;
            if (overworld != null) {
                int y = overworld.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, state.preDepthsPos.getX(), state.preDepthsPos.getZ());
                sp.teleportTo(overworld, state.preDepthsPos.getX() + 0.5, Math.max(y + 1, state.preDepthsPos.getY()), state.preDepthsPos.getZ() + 0.5, sp.getYRot(), sp.getXRot());
            }
        }
        restorePlayer(sp.getUUID());
    }

    private static void restorePlayer(UUID uuid) {
        Map<BlockPos, BlockState> saved = SAVED_BLOCKS.remove(uuid);
        if (saved == null || saved.isEmpty()) return;
        var srv = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        ServerLevel overworld = srv.getLevel(Level.OVERWORLD);
        if (overworld == null) return;
        Set<Long> forcedChunks = new HashSet<>();
        for (BlockPos pos : saved.keySet()) {
            forcedChunks.add(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
        }
        for (long chunkLong : forcedChunks) {
            overworld.setChunkForced(ChunkPos.getX(chunkLong), ChunkPos.getZ(chunkLong), true);
        }
        Iterator<Map.Entry<BlockPos, BlockState>> it = saved.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            if (overworld.isLoaded(entry.getKey())) {
                overworld.setBlock(entry.getKey(), entry.getValue(), 2);
                it.remove();
            }
        }
        for (long chunkLong : forcedChunks) {
            overworld.setChunkForced(ChunkPos.getX(chunkLong), ChunkPos.getZ(chunkLong), false);
        }
    }
}