package com.abnormalities.thewind;

import com.abnormalities.config.AbnormalitiesConfig;
import com.abnormalities.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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

public class TheWindBorderManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|TheWind|Border");
    private static final Map<UUID, Long> playerCooldowns = new HashMap<>();
    private static final int FLAG = 34;

    public static void forceBorder(ServerPlayer player) {
        playerCooldowns.put(player.getUUID(), player.level().getGameTime());
        execute(player);
    }

    static void tick(ServerPlayer player, long now) {
        if (!AbnormalitiesConfig.TW_ENABLED.get()) return;
        if (!AbnormalitiesConfig.TW_BORDER_ENABLED.get()) return;
        if (player.tickCount % 200 != 0) return;

        long graceTicks = (long) AbnormalitiesConfig.GRACE_PERIOD_DAYS.get() * 24000L;
        if (now < graceTicks) return;

        Long last = playerCooldowns.get(player.getUUID());
        long cooldown = AbnormalitiesConfig.TW_BORDER_COOLDOWN.get();
        if (last != null && now - last < cooldown) return;

        int chance = AbnormalitiesConfig.TW_BORDER_CHANCE.get();
        if (player.level().random.nextInt(chance) != 0) return;

        playerCooldowns.put(player.getUUID(), now);
        execute(player);
    }

    private static void execute(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        int borderDist = AbnormalitiesConfig.TW_BORDER_DISTANCE.get();
        BlockPos center = player.blockPosition();
        int playerChunkX = center.getX() >> 4;
        int playerChunkZ = center.getZ() >> 4;

        for (int cx = playerChunkX - borderDist; cx <= playerChunkX + borderDist; cx++) {
            level.setChunkForced(cx, playerChunkZ - borderDist, true);
            level.setChunkForced(cx, playerChunkZ + borderDist, true);
        }
        for (int cz = playerChunkZ - borderDist + 1; cz <= playerChunkZ + borderDist - 1; cz++) {
            level.setChunkForced(playerChunkX - borderDist, cz, true);
            level.setChunkForced(playerChunkX + borderDist, cz, true);
        }

        int totalDestroyed = 0;

        for (int cx = playerChunkX - borderDist; cx <= playerChunkX + borderDist; cx++) {
            totalDestroyed += clearChunkColumn(level, cx, playerChunkZ - borderDist);
            totalDestroyed += clearChunkColumn(level, cx, playerChunkZ + borderDist);
        }
        for (int cz = playerChunkZ - borderDist + 1; cz <= playerChunkZ + borderDist - 1; cz++) {
            totalDestroyed += clearChunkColumn(level, playerChunkX - borderDist, cz);
            totalDestroyed += clearChunkColumn(level, playerChunkX + borderDist, cz);
        }

        int ringChunks = (borderDist * 2 + 1) * 2 + (borderDist * 2 - 1) * 2;
        LOGGER.info("[THE_WIND|Border] Cleared {}/{} chunks ({} blocks) around {} at distance {}",
                ringChunks, ringChunks, totalDestroyed, player.getName().getString(), borderDist);

        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSystemChatPacket(
                Component.literal("the ground screams beneath you").withStyle(net.minecraft.ChatFormatting.DARK_GRAY, net.minecraft.ChatFormatting.ITALIC), false));

        level.playSound(null, player.blockPosition(), ModSounds.NUR_SOUND.get(), SoundSource.AMBIENT, 8.0f, 0.3f);
        level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE, SoundSource.AMBIENT, 6.0f, 0.2f);
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
