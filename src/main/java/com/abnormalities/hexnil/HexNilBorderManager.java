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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class HexNilBorderManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|0x0000|Border");
    private static final int FLAG = 2;
    private static final int MAX_CHUNKS = 200;
    private static final int CHUNKS_PER_TICK = 1;
    private static final List<BorderDestructionJob> activeJobs = new ArrayList<>();

    private static class BorderDestructionJob {
        ServerLevel level;
        List<Long> chunkKeys;
        UUID playerUuid;
        int borderDist;
        int totalChunks;
        int clearedChunks;
        int totalDestroyed;
        Set<Long> forcedChunks;
        boolean cancelled;

        BorderDestructionJob(ServerLevel level, List<Long> chunkKeys, UUID playerUuid, int borderDist,
                            int totalChunks, Set<Long> forcedChunks) {
            this.level = level;
            this.chunkKeys = chunkKeys;
            this.playerUuid = playerUuid;
            this.borderDist = borderDist;
            this.totalChunks = totalChunks;
            this.clearedChunks = 0;
            this.totalDestroyed = 0;
            this.forcedChunks = forcedChunks;
            this.cancelled = false;
        }
    }

    private static final java.util.Map<UUID, Long> playerCooldowns = new java.util.HashMap<>();

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Iterator<BorderDestructionJob> it = activeJobs.iterator();
        while (it.hasNext()) {
            BorderDestructionJob job = it.next();
            if (job.cancelled || job.level == null || job.level.getServer() == null) {
                unforceJobChunks(job);
                it.remove();
                continue;
            }
            int processed = 0;
            while (processed < CHUNKS_PER_TICK && !job.chunkKeys.isEmpty()) {
                long key = job.chunkKeys.remove(job.chunkKeys.size() - 1);
                int cx = (int) (key >> 32);
                int cz = (int) key;
                try {
                    job.totalDestroyed += clearChunkColumn(job.level, cx, cz);
                } catch (Throwable t) {
                    LOGGER.debug("[0x0000|Border] clearChunkColumn failed at {},{}: {}", cx, cz, t.getMessage());
                }
                job.clearedChunks++;
                processed++;
            }
            if (job.chunkKeys.isEmpty()) {
                LOGGER.info("[0x0000|Border] Cleared {}/{} blocks across {} chunks around {}",
                        job.totalDestroyed, job.totalDestroyed, job.clearedChunks, job.playerUuid);
                unforceJobChunks(job);
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        Iterator<BorderDestructionJob> it = activeJobs.iterator();
        while (it.hasNext()) {
            BorderDestructionJob job = it.next();
            if (job.playerUuid.equals(uuid)) {
                job.cancelled = true;
            }
        }
        playerCooldowns.remove(uuid);
    }

    private static void unforceJobChunks(BorderDestructionJob job) {
        if (job.forcedChunks == null) return;
        for (long key : job.forcedChunks) {
            int cx = (int) (key >> 32);
            int cz = (int) key;
            try {
                if (job.level != null) {
                    job.level.setChunkForced(cx, cz, false);
                }
            } catch (Throwable t) {
            }
        }
    }

    public static void forceBorder(ServerPlayer player) {
        playerCooldowns.put(player.getUUID(), player.level().getGameTime());
        execute(player);
    }

    static void tick(ServerPlayer player, long now) {
        if (!AbnormalitiesConfig.HN_ENABLED.get()) return;
        if (!AbnormalitiesConfig.HN_BORDER_ENABLED.get()) return;

        UUID uuid = player.getUUID();

        if (player.tickCount % 200 != 0) return;

        long graceTicks = (long) AbnormalitiesConfig.GRACE_PERIOD_DAYS.get() * 24000L;
        if (now < graceTicks) return;

        Long last = playerCooldowns.get(uuid);
        long cooldown = AbnormalitiesConfig.HN_BORDER_COOLDOWN.get();
        if (last != null && now - last < cooldown) return;

        int chance = AbnormalitiesConfig.HN_BORDER_CHANCE.get();
        if (player.level().random.nextInt(chance) != 0) return;

        playerCooldowns.put(uuid, now);
        execute(player);
    }

    private static void execute(ServerPlayer player) {
        UUID uuid = player.getUUID();
        for (BorderDestructionJob j : activeJobs) {
            if (j.playerUuid.equals(uuid) && !j.cancelled) return;
        }

        ServerLevel level = (ServerLevel) player.level();
        int borderDist = AbnormalitiesConfig.HN_BORDER_DISTANCE.get();
        BlockPos center = player.blockPosition();
        int playerChunkX = center.getX() >> 4;
        int playerChunkZ = center.getZ() >> 4;

        Set<Long> chunksToProcess = new HashSet<>();
        for (int ring = Math.max(0, borderDist - 1); ring <= borderDist + 1; ring++) {
            for (int cx = playerChunkX - ring; cx <= playerChunkX + ring; cx++) {
                chunksToProcess.add(chunkKey(cx, playerChunkZ - ring));
                if (ring > 0) chunksToProcess.add(chunkKey(cx, playerChunkZ + ring));
            }
            for (int cz = playerChunkZ - ring + 1; cz <= playerChunkZ + ring - 1; cz++) {
                chunksToProcess.add(chunkKey(playerChunkX - ring, cz));
                if (ring > 0) chunksToProcess.add(chunkKey(playerChunkX + ring, cz));
            }
        }

        Set<Long> forced = new HashSet<>();
        int count = 0;
        for (long key : chunksToProcess) {
            if (count >= MAX_CHUNKS) break;
            int cx = (int) (key >> 32);
            int cz = (int) key;
            level.setChunkForced(cx, cz, true);
            forced.add(key);
            count++;
        }

        List<Long> shuffledKeys = new ArrayList<>(chunksToProcess);
        java.util.Collections.shuffle(shuffledKeys);

        int totalChunks = Math.min(chunksToProcess.size(), MAX_CHUNKS);
        BorderDestructionJob job = new BorderDestructionJob(level, shuffledKeys, uuid, borderDist, totalChunks, forced);
        activeJobs.add(job);

        LOGGER.info("[0x0000|Border] Clearing {} chunks around {} at distances {}-{} ({} ticks est.)",
                totalChunks, player.getName().getString(), Math.max(0, borderDist - 1), borderDist + 1,
                (totalChunks + CHUNKS_PER_TICK - 1) / CHUNKS_PER_TICK);

        try {
            level.playSound(null, player.blockPosition(), ModSounds.NUR_SOUND.get(), SoundSource.AMBIENT, 8.0f, 0.3f);
            level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE, SoundSource.AMBIENT, 6.0f, 0.2f);
        } catch (Throwable t) {
            LOGGER.error("[0x0000|Border] playSound failed for {}", player.getName().getString(), t);
        }

        if (AbnormalitiesConfig.HN_SHAKE_ENABLED.get()) {
            HexNilShakeHandler.sendShake(player, 2.0f, 200);
        }
    }

    private static long chunkKey(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }

    private static int clearChunkColumn(ServerLevel level, int chunkX, int chunkZ) {
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        int destroyed = 0;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int wx = baseX + x;
                int wz = baseZ + z;
                for (int y = -64; y < 320; y++) {
                    BlockPos pos = new BlockPos(wx, y, wz);
                    if (!level.isLoaded(pos)) continue;
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir()) continue;
                    if (state.getBlock() == Blocks.BEDROCK) continue;

                    try {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), FLAG);
                    } catch (Throwable t) {
                        continue;
                    }
                    destroyed++;
                }
            }
        }
        return destroyed;
    }
}
