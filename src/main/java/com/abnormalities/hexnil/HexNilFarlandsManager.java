package com.abnormalities.hexnil;

import com.abnormalities.AbnormalitiesMod;
import com.abnormalities.ReputationManager;
import com.abnormalities.config.AbnormalitiesConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class HexNilFarlandsManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|0x0000|Farlands");
    private static long lastFarlands = 0;
    private static final Map<Long, Long> GENERATED_CHUNKS = new HashMap<>();

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!AbnormalitiesConfig.HN_ENABLED.get()) return;
        if (com.abnormalities.horror.PeakDayManager.isWindDisabled()) return;
        var srv = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        ServerLevel overworld = srv.getLevel(net.minecraft.world.level.Level.OVERWORLD);
        if (overworld == null) return;
        long now = overworld.getGameTime();

        long graceTicks = (long) AbnormalitiesConfig.GRACE_PERIOD_DAYS.get() * 24000L;
        if (now < graceTicks) return;

        if (now - lastFarlands < 60000) return;
        if (overworld.random.nextInt(5000) != 0) return;

        for (ServerPlayer player : srv.getPlayerList().getPlayers()) {
            int rep = ReputationManager.getRep(player);
            if (rep > 800) continue;

            lastFarlands = now;
            LOGGER.info("[0x0000|Farlands] Farlands triggered for {} (rep={})", player.getName().getString(), rep);
            generateFarlands(overworld, player);
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("the world is wrong here.").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE), false);
            break;
        }
    }

    public static void generateFarlands(ServerLevel level, ServerPlayer player) {
        BlockPos center = player.blockPosition();
        int chunkX = (center.getX() >> 4) + level.random.nextInt(11) - 5;
        int chunkZ = (center.getZ() >> 4) + level.random.nextInt(11) - 5;

        for (int cx = chunkX - 1; cx <= chunkX + 1; cx++) {
            for (int cz = chunkZ - 1; cz <= chunkZ + 1; cz++) {
                long key = ((long) cx & 0xFFFFFFFFL) << 32 | ((long) cz & 0xFFFFFFFFL);
                if (GENERATED_CHUNKS.containsKey(key)) continue;
                if (GENERATED_CHUNKS.size() > 20) GENERATED_CHUNKS.clear();
                GENERATED_CHUNKS.put(key, level.getGameTime());
                LOGGER.info("[0x0000|Farlands] Generating chunk ({}, {}) [total chunks: {}]", cx, cz, GENERATED_CHUNKS.size());
                generateFarlandChunk(level, cx, cz);
            }
        }
    }

    private static void generateFarlandChunk(ServerLevel level, int chunkX, int chunkZ) {
        Random rng = new Random(level.getSeed() ^ ((long) chunkX << 32) ^ chunkZ);
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;

        int baseY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, baseX + 8, baseZ + 8);

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int wx = baseX + x;
                int wz = baseZ + z;

                double noise = Math.sin(wx * 0.1) * Math.cos(wz * 0.1) * 20
                        + Math.sin(wx * 0.05 + wz * 0.07) * 30
                        + rng.nextDouble() * 15;

                int topY = (int) (baseY + noise + 30);
                int bottomY = baseY - 10 - rng.nextInt(20);

                for (int y = bottomY; y <= topY; y++) {
                    BlockPos pos = new BlockPos(wx, y, wz);
                    if (y == topY) {
                        if (topY > baseY + 15 && rng.nextInt(3) == 0) {
                            level.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), 34);
                            if (rng.nextInt(4) == 0) {
                                level.setBlock(pos.above(), Blocks.TALL_GRASS.defaultBlockState(), 34);
                            }

                        } else if (topY > baseY + 20) {
                            level.setBlock(pos, rng.nextBoolean() ? Blocks.DIRT.defaultBlockState() : Blocks.STONE.defaultBlockState(), 34);
                        } else {
                            level.setBlock(pos, Blocks.STONE.defaultBlockState(), 34);
                        }
                    } else if (y == bottomY) {
                        level.setBlock(pos, Blocks.STONE.defaultBlockState(), 34);
                    } else if (y > topY - 4) {
                        level.setBlock(pos, rng.nextInt(3) == 0 ? Blocks.DIRT.defaultBlockState() : Blocks.STONE.defaultBlockState(), 34);
                    } else {
                        level.setBlock(pos, Blocks.STONE.defaultBlockState(), 34);
                    }
                }

                for (int y = bottomY; y < topY - 3; y++) {
                    if (rng.nextInt(12) == 0) {
                        BlockPos cave = new BlockPos(wx, y, wz);
                        for (int dx = -1; dx <= 1; dx++) {
                            for (int dz = -1; dz <= 1; dz++) {
                                if (rng.nextInt(3) == 0) {
                                    level.setBlock(cave.offset(dx, 0, dz), Blocks.AIR.defaultBlockState(), 34);
                                    level.setBlock(cave.offset(dx, 1, dz), Blocks.AIR.defaultBlockState(), 34);
                                }
                            }
                        }
                    }
                }

                if (topY > baseY + 25 && rng.nextInt(6) == 0) {
                    for (int dy = 0; dy < 3; dy++) {
                        level.setBlock(new BlockPos(wx, topY + dy + 1, wz), Blocks.AIR.defaultBlockState(), 34);
                    }
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            if (rng.nextInt(2) == 0) {
                                level.setBlock(new BlockPos(wx + dx, topY + 1, wz + dz), Blocks.DIRT.defaultBlockState(), 34);
                                level.setBlock(new BlockPos(wx + dx, topY + 2, wz + dz), Blocks.GRASS_BLOCK.defaultBlockState(), 34);
                            }
                        }
                    }
                }

                if (topY > baseY + 35 && rng.nextInt(10) == 0) {
                    level.setBlock(new BlockPos(wx, topY + 1, wz), Blocks.OAK_LOG.defaultBlockState(), 34);
                    level.setBlock(new BlockPos(wx, topY + 2, wz), Blocks.OAK_LOG.defaultBlockState(), 34);
                    for (int dx = -2; dx <= 2; dx++) {
                        for (int dz = -2; dz <= 2; dz++) {
                            if (Math.abs(dx) + Math.abs(dz) <= 3 && rng.nextInt(3) != 0) {
                                level.setBlock(new BlockPos(wx + dx, topY + 3, wz + dz), Blocks.OAK_LEAVES.defaultBlockState(), 34);
                            }
                        }
                    }
                }
            }
        }
    }

    public static void forceFarlands(ServerPlayer player) {
        generateFarlands((ServerLevel) player.level(), player);
    }
}
