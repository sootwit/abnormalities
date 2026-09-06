package com.abnormalities.hexnil;

import com.abnormalities.config.AbnormalitiesConfig;
import com.abnormalities.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HexNilChunkManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|0x0000|Chunk");
    private static final Map<UUID, Long> playerCooldowns = new HashMap<>();
    private static final int FLAG = 2;
    private static final int BLOCKS_PER_TICK = 15000;
    private static final int MAX_ITERATIONS_PER_TICK = 20000;
    private static final List<ChunkDestructionJob> activeJobs = new ArrayList<>();

    private static class ChunkDestructionJob {
        ServerLevel level;
        List<BlockPos> positions;
        int totalBlocks;
        int destroyed;
        boolean soundPlayed;
        UUID playerUuid;
        String axisName;
        int x1, startY, x2, z1, z2;
        List<long[]> forcedChunks;
        boolean cancelled;

        ChunkDestructionJob(ServerLevel level, List<BlockPos> positions, int totalBlocks, UUID playerUuid,
                           String axisName, int x1, int startY, int x2, int z1, int z2, List<long[]> forcedChunks) {
            this.level = level;
            this.positions = positions;
            this.totalBlocks = totalBlocks;
            this.destroyed = 0;
            this.soundPlayed = false;
            this.playerUuid = playerUuid;
            this.axisName = axisName;
            this.x1 = x1;
            this.startY = startY;
            this.x2 = x2;
            this.z1 = z1;
            this.z2 = z2;
            this.forcedChunks = forcedChunks;
            this.cancelled = false;
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Iterator<ChunkDestructionJob> it = activeJobs.iterator();
        while (it.hasNext()) {
            ChunkDestructionJob job = it.next();
            if (job.cancelled || job.level == null || job.level.getServer() == null) {
                unforceChunks(job);
                it.remove();
                continue;
            }
            int iterations = 0;
            while (iterations < MAX_ITERATIONS_PER_TICK && job.destroyed < BLOCKS_PER_TICK && !job.positions.isEmpty()) {
                BlockPos pos = job.positions.remove(job.positions.size() - 1);
                iterations++;
                if (!job.level.isLoaded(pos)) continue;
                BlockState state = job.level.getBlockState(pos);
                if (state.isAir()) continue;
                if (state.getBlock() == Blocks.BEDROCK) continue;
                try {
                    job.level.setBlock(pos, Blocks.AIR.defaultBlockState(), FLAG);
                } catch (Throwable t) {
                    LOGGER.debug("[0x0000] setBlock failed at {}: {}", pos, t.getMessage());
                    continue;
                }
                job.destroyed++;
            }
            if (!job.soundPlayed && job.destroyed > 0) {
                ServerPlayer player = job.level.getServer().getPlayerList().getPlayer(job.playerUuid);
                if (player != null) {
                    job.level.playSound(null, player.blockPosition(), ModSounds.NUR_SOUND.get(), SoundSource.AMBIENT, 5.0f, 0.4f);
                    job.level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE, SoundSource.AMBIENT, 3.0f, 0.3f);
                    job.soundPlayed = true;
                }
            }
            if (job.positions.isEmpty()) {
                LOGGER.info("[0x0000] Chunk removed: 16x512x320 ({}) at ({},{})-({},{},{}) - {}/{} blocks",
                        job.axisName, job.x1, job.startY, job.x2, job.z1, job.z2, job.destroyed, job.totalBlocks);
                unforceChunks(job);
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        Iterator<ChunkDestructionJob> it = activeJobs.iterator();
        while (it.hasNext()) {
            ChunkDestructionJob job = it.next();
            if (job.playerUuid.equals(uuid)) {
                job.cancelled = true;
            }
        }
        playerCooldowns.remove(uuid);
    }

    private static void unforceChunks(ChunkDestructionJob job) {
        if (job.forcedChunks == null) return;
        for (long[] pair : job.forcedChunks) {
            try {
                if (job.level != null) {
                    job.level.setChunkForced((int) pair[0], (int) pair[1], false);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    public static void forceChunk(ServerPlayer player) {
        playerCooldowns.put(player.getUUID(), player.level().getGameTime());
        execute(player);
    }

    static void tick(ServerPlayer player, long now) {
        if (!AbnormalitiesConfig.HN_ENABLED.get()) return;
        if (!AbnormalitiesConfig.HN_CHUNK_ENABLED.get()) return;
        if (player.tickCount % 200 != 0) return;

        long graceTicks = (long) AbnormalitiesConfig.GRACE_PERIOD_DAYS.get() * 24000L;
        if (now < graceTicks) return;

        Long last = playerCooldowns.get(player.getUUID());
        long cooldown = AbnormalitiesConfig.HN_CHUNK_COOLDOWN.get();
        if (last != null && now - last < cooldown) return;

        int chance = AbnormalitiesConfig.HN_CHUNK_CHANCE.get();
        if (player.level().random.nextInt(chance) != 0) return;

        playerCooldowns.put(player.getUUID(), now);
        execute(player);
    }

    private static void execute(ServerPlayer player) {
        UUID uuid = player.getUUID();
        for (ChunkDestructionJob j : activeJobs) {
            if (j.playerUuid.equals(uuid) && !j.cancelled) return;
        }

        ServerLevel level = (ServerLevel) player.level();
        int range = AbnormalitiesConfig.HN_CHUNK_RANGE.get();
        boolean alignX = level.random.nextBoolean();

        int px = (int) Math.floor(player.getX());
        int py = (int) Math.floor(player.getY());
        int pz = (int) Math.floor(player.getZ());

        int offset1 = level.random.nextInt(range * 2 + 1) - range;
        int offset2 = level.random.nextInt(range * 2 + 1) - range;

        int baseX, baseZ;
        if (alignX) {
            baseX = px + offset1;
            baseZ = pz + offset2;
        } else {
            baseX = px + offset2;
            baseZ = pz + offset1;
        }

        int width = 16;
        int height = 512;
        int depth = 320;

        int startY = Math.max(-64, py - height / 2);
        int endY = Math.min(320, startY + height);
        startY = Math.max(-64, endY - height);

        int x1, x2, z1, z2;
        if (alignX) {
            x1 = baseX;
            x2 = baseX + width;
            z1 = baseZ;
            z2 = baseZ + depth;
        } else {
            x1 = baseX;
            x2 = baseX + depth;
            z1 = baseZ;
            z2 = baseZ + width;
        }

        int minCX = x1 >> 4;
        int maxCX = x2 >> 4;
        int minCZ = z1 >> 4;
        int maxCZ = z2 >> 4;
        List<long[]> forcedChunks = new ArrayList<>();
        for (int cx = minCX - 1; cx <= maxCX + 1; cx++) {
            for (int cz = minCZ - 1; cz <= maxCZ + 1; cz++) {
                level.setChunkForced(cx, cz, true);
                forcedChunks.add(new long[]{cx, cz});
            }
        }

        List<BlockPos> positions = new ArrayList<>();
        for (int x = x1; x < x2; x++) {
            for (int z = z1; z < z2; z++) {
                for (int y = startY; y < endY; y++) {
                    positions.add(new BlockPos(x, y, z));
                }
            }
        }
        java.util.Collections.shuffle(positions);

        ChunkDestructionJob job = new ChunkDestructionJob(level, positions, positions.size(),
                uuid, alignX ? "X" : "Z", x1, startY, x2, z1, z2, forcedChunks);
        activeJobs.add(job);

        String axis = alignX ? "X" : "Z";
        LOGGER.info("[0x0000] Chunk queued: 16x512x320 ({}) at ({},{})-({},{},{}) - {} blocks ({} ticks est.)",
                axis, x1, startY, x2, z1, z2, positions.size(), (positions.size() + BLOCKS_PER_TICK - 1) / BLOCKS_PER_TICK);

        double cx = (x1 + x2) / 2.0;
        double cz = (z1 + z2) / 2.0;
        double shakeRangeSq = 128.0 * 128.0;
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            double dist = p.distanceToSqr(cx, startY, cz);
            if (dist < shakeRangeSq) {
                HexNilShakeHandler.sendShake(p, 2.0f, 200);
            }
        }
    }
}
