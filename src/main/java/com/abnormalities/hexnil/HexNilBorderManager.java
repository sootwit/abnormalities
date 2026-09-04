package com.abnormalities.hexnil;

import com.abnormalities.config.AbnormalitiesConfig;
import com.abnormalities.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class HexNilBorderManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|0x0000|Border");
    private static final Map<UUID, Long> playerCooldowns = new HashMap<>();
    private static final int FLAG = 34;
    private static final int MAX_CHUNKS = 200;

    private static final Map<UUID, Set<Long>> forcedChunks = new HashMap<>();

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
        ServerLevel level = (ServerLevel) player.level();
        int borderDist = AbnormalitiesConfig.HN_BORDER_DISTANCE.get();
        BlockPos center = player.blockPosition();
        int playerChunkX = center.getX() >> 4;
        int playerChunkZ = center.getZ() >> 4;

        UUID uuid = player.getUUID();

        Set<Long> oldForced = forcedChunks.remove(uuid);
        if (oldForced != null) {
            for (long key : oldForced) {
                int cx = (int) (key >> 32);
                int cz = (int) key;
                level.setChunkForced(cx, cz, false);
            }
        }

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
        forcedChunks.put(uuid, forced);

        int totalChunks = chunksToProcess.size();
        LOGGER.info("[0x0000|Border] Clearing {} chunks INSTANTLY around {} at distances {}-{}",
                totalChunks, player.getName().getString(), Math.max(0, borderDist - 1), borderDist + 1);

        int totalDestroyed = 0;
        for (long key : chunksToProcess) {
            int cx = (int) (key >> 32);
            int cz = (int) key;
            totalDestroyed += clearChunkColumn(level, cx, cz);
        }

        for (long key : forced) {
            int cx = (int) (key >> 32);
            int cz = (int) key;
            level.setChunkForced(cx, cz, false);
        }
        forcedChunks.remove(uuid);

        LOGGER.info("[0x0000|Border] Cleared {} blocks around {}", totalDestroyed, player.getName().getString());

        try {
            level.playSound(null, player.blockPosition(), ModSounds.NUR_SOUND.get(), SoundSource.AMBIENT, 8.0f, 0.3f);
            level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE, SoundSource.AMBIENT, 6.0f, 0.2f);
        } catch (Exception e) {
            LOGGER.error("[0x0000|Border] playSound failed for {}", player.getName().getString(), e);
        }

        if (AbnormalitiesConfig.HN_SHAKE_ENABLED.get()) {
            HexNilShakeHandler.sendShake(player, 2.0f, 200);
        }
    }

    private static long chunkKey(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        Set<Long> forced = forcedChunks.remove(uuid);
        if (forced == null || forced.isEmpty()) return;
        ServerPlayer sp = (ServerPlayer) event.getEntity();
        ServerLevel level = (ServerLevel) sp.level();
        for (long key : forced) {
            int cx = (int) (key >> 32);
            int cz = (int) key;
            level.setChunkForced(cx, cz, false);
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
