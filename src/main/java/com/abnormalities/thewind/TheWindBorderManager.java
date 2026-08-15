package com.abnormalities.thewind;

import com.abnormalities.config.AbnormalitiesConfig;
import com.abnormalities.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

public class TheWindBorderManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|TheWind|Border");
    private static final Map<UUID, Long> playerCooldowns = new HashMap<>();
    private static final int FLAG = 34;
    private static final int CHUNKS_PER_TICK = 6;

    private static final Map<UUID, Queue<long[]>> pendingChunks = new HashMap<>();
    private static final Map<UUID, Integer> pendingDestroyed = new HashMap<>();

    public static void forceBorder(ServerPlayer player) {
        playerCooldowns.put(player.getUUID(), player.level().getGameTime());
        execute(player);
    }

    static void tick(ServerPlayer player, long now) {
        if (!AbnormalitiesConfig.TW_ENABLED.get()) return;
        if (!AbnormalitiesConfig.TW_BORDER_ENABLED.get()) return;

        UUID uuid = player.getUUID();
        if (pendingChunks.containsKey(uuid)) {
            processPending(player);
            return;
        }

        if (player.tickCount % 200 != 0) return;

        long graceTicks = (long) AbnormalitiesConfig.GRACE_PERIOD_DAYS.get() * 24000L;
        if (now < graceTicks) return;

        Long last = playerCooldowns.get(uuid);
        long cooldown = AbnormalitiesConfig.TW_BORDER_COOLDOWN.get();
        if (last != null && now - last < cooldown) return;

        int chance = AbnormalitiesConfig.TW_BORDER_CHANCE.get();
        if (player.level().random.nextInt(chance) != 0) return;

        playerCooldowns.put(uuid, now);
        execute(player);
    }

    private static void execute(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        int borderDist = AbnormalitiesConfig.TW_BORDER_DISTANCE.get();
        BlockPos center = player.blockPosition();
        int playerChunkX = center.getX() >> 4;
        int playerChunkZ = center.getZ() >> 4;

        Queue<long[]> queue = new LinkedList<>();
        for (int ring = Math.max(0, borderDist - 1); ring <= borderDist + 1; ring++) {
            for (int cx = playerChunkX - ring; cx <= playerChunkX + ring; cx++) {
                queue.add(new long[]{cx, playerChunkZ - ring});
                if (ring > 0) queue.add(new long[]{cx, playerChunkZ + ring});
            }
            for (int cz = playerChunkZ - ring + 1; cz <= playerChunkZ + ring - 1; cz++) {
                queue.add(new long[]{playerChunkX - ring, cz});
                if (ring > 0) queue.add(new long[]{playerChunkX + ring, cz});
            }
        }

        UUID uuid = player.getUUID();
        pendingChunks.put(uuid, queue);
        pendingDestroyed.put(uuid, 0);

        int totalChunks = queue.size();
        LOGGER.info("[THE_WIND|Border] Queued {} chunks (3-wide ring) around {} at distances {}-{}",
                totalChunks, player.getName().getString(), Math.max(0, borderDist - 1), borderDist + 1);

        level.playSound(null, player.blockPosition(), ModSounds.NUR_SOUND.get(), SoundSource.AMBIENT, 8.0f, 0.3f);
        level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE, SoundSource.AMBIENT, 6.0f, 0.2f);

        processPending(player);
    }

    private static void processPending(ServerPlayer player) {
        UUID uuid = player.getUUID();
        Queue<long[]> queue = pendingChunks.get(uuid);
        if (queue == null) return;

        ServerLevel level = (ServerLevel) player.level();
        int processed = 0;

        while (!queue.isEmpty() && processed < CHUNKS_PER_TICK) {
            long[] chunk = queue.poll();
            int cx = (int) chunk[0];
            int cz = (int) chunk[1];
            level.setChunkForced(cx, cz, true);
            int destroyed = clearChunkColumn(level, cx, cz);
            pendingDestroyed.merge(uuid, destroyed, Integer::sum);
            processed++;
        }

        if (queue.isEmpty()) {
            int total = pendingDestroyed.remove(uuid);
            pendingChunks.remove(uuid);
            LOGGER.info("[THE_WIND|Border] Cleared {} blocks around {}", total, player.getName().getString());
            if (AbnormalitiesConfig.TW_SHAKE_ENABLED.get()) {
                TheWindShakeHandler.sendShake(player, 2.0f, 200);
            }
        }
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

                    if (state.hasBlockEntity() && level.getBlockEntity(pos) instanceof BaseContainerBlockEntity container) {
                        for (int i = 0; i < container.getContainerSize(); i++) {
                            var stack = container.getItem(i);
                            if (!stack.isEmpty()) {
                                level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level, wx + 0.5, y + 0.5, wz + 0.5, stack.copy()));
                            }
                        }
                    }

                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), FLAG);
                    destroyed++;
                }
            }
        }
        return destroyed;
    }
}
