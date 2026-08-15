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
import java.util.Map;
import java.util.UUID;

public class TheWindChunkManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|TheWind|Chunk");
    private static final Map<UUID, Long> playerCooldowns = new HashMap<>();
    private static final int FLAG = 34;

    public static void forceChunk(ServerPlayer player) {
        playerCooldowns.put(player.getUUID(), player.level().getGameTime());
        execute(player);
    }

    static void tick(ServerPlayer player, long now) {
        if (!AbnormalitiesConfig.TW_ENABLED.get()) return;
        if (!AbnormalitiesConfig.TW_CHUNK_ENABLED.get()) return;
        if (player.tickCount % 200 != 0) return;

        long graceTicks = (long) AbnormalitiesConfig.GRACE_PERIOD_DAYS.get() * 24000L;
        if (now < graceTicks) return;

        Long last = playerCooldowns.get(player.getUUID());
        long cooldown = AbnormalitiesConfig.TW_CHUNK_COOLDOWN.get();
        if (last != null && now - last < cooldown) return;

        int chance = AbnormalitiesConfig.TW_CHUNK_CHANCE.get();
        if (player.level().random.nextInt(chance) != 0) return;

        playerCooldowns.put(player.getUUID(), now);
        execute(player);
    }

    private static void execute(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        int range = AbnormalitiesConfig.TW_CHUNK_RANGE.get();
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
        for (int cx = minCX - 1; cx <= maxCX + 1; cx++) {
            for (int cz = minCZ - 1; cz <= maxCZ + 1; cz++) {
                level.setChunkForced(cx, cz, true);
            }
        }

        int destroyed = 0;
        for (int x = x1; x < x2; x++) {
            for (int z = z1; z < z2; z++) {
                for (int y = startY; y < endY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!level.isLoaded(pos)) continue;
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir()) continue;
                    if (state.getBlock() == Blocks.BEDROCK) continue;

                    if (state.hasBlockEntity() && level.getBlockEntity(pos) instanceof BaseContainerBlockEntity container) {
                        for (int i = 0; i < container.getContainerSize(); i++) {
                            var stack = container.getItem(i);
                            if (!stack.isEmpty()) {
                                level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level, x + 0.5, y + 0.5, z + 0.5, stack.copy()));
                            }
                        }
                    }

                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), FLAG);
                    destroyed++;
                }
            }
        }

        String axis = alignX ? "X" : "Z";
        LOGGER.info("[THE_WIND] Chunk removed: {}x{}x{} ({}) at ({},{})-({},{},{}) - {} blocks",
                width, height, depth, axis, x1, startY, x2, z1, z2, destroyed);

        level.playSound(null, player.blockPosition(), ModSounds.NUR_SOUND.get(), SoundSource.AMBIENT, 5.0f, 0.4f);
        level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE, SoundSource.AMBIENT, 3.0f, 0.3f);

        double cx = (x1 + x2) / 2.0;
        double cz = (z1 + z2) / 2.0;
        double shakeRangeSq = 128.0 * 128.0;
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            double dist = p.distanceToSqr(cx, startY, cz);
            if (dist < shakeRangeSq) {
                TheWindShakeHandler.sendShake(p, 2.0f, 200);
            }
        }
    }
}
