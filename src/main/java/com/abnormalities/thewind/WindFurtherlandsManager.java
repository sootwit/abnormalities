package com.abnormalities.thewind;

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

public class WindFurtherlandsManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|TheWind|Furtherlands");
    private static long lastFurtherlands = 0;
    private static final Map<Long, Long> GENERATED_CHUNKS = new HashMap<>();

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!AbnormalitiesConfig.TW_ENABLED.get()) return;
        if (!AbnormalitiesConfig.TW_FURTHERLANDS_ENABLED.get()) return;
        var srv = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        ServerLevel overworld = srv.getLevel(net.minecraft.world.level.Level.OVERWORLD);
        if (overworld == null) return;
        long now = overworld.getGameTime();

        long graceTicks = (long) AbnormalitiesConfig.GRACE_PERIOD_DAYS.get() * 24000L;
        if (now < graceTicks) return;

        if (now - lastFurtherlands < AbnormalitiesConfig.TW_FURTHERLANDS_COOLDOWN.get()) return;
        if (overworld.random.nextInt(AbnormalitiesConfig.TW_FURTHERLANDS_CHANCE.get()) != 0) return;

        for (ServerPlayer player : srv.getPlayerList().getPlayers()) {
            int rep = ReputationManager.getRep(player);
            if (rep > AbnormalitiesConfig.TW_FURTHERLANDS_MAX_REP.get()) continue;

            lastFurtherlands = now;
            LOGGER.info("[THE_WIND|Furtherlands] Furtherlands triggered for {} (rep={})", player.getName().getString(), rep);
            generateFurtherlands(overworld, player);
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("the earth remembers things it should not.").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE), false);
            break;
        }
    }

    public static void generateFurtherlands(ServerLevel level, ServerPlayer player) {
        BlockPos center = player.blockPosition();
        int radius = AbnormalitiesConfig.TW_FURTHERLANDS_RADIUS.get();
        int chunkX = (center.getX() >> 4) + level.random.nextInt(radius * 2 + 1) - radius;
        int chunkZ = (center.getZ() >> 4) + level.random.nextInt(radius * 2 + 1) - radius;

        for (int cx = chunkX - 4; cx <= chunkX + 3; cx++) {
            for (int cz = chunkZ - 4; cz <= chunkZ + 3; cz++) {
                long key = ((long) cx & 0xFFFFFFFFL) << 32 | ((long) cz & 0xFFFFFFFFL);
                if (GENERATED_CHUNKS.containsKey(key)) continue;
                if (GENERATED_CHUNKS.size() > 40) GENERATED_CHUNKS.clear();
                GENERATED_CHUNKS.put(key, level.getGameTime());
                LOGGER.info("[THE_WIND|Furtherlands] Generating chunk ({}, {}) [total chunks: {}]", cx, cz, GENERATED_CHUNKS.size());
                generateFurtherlandChunk(level, cx, cz);
            }
        }
    }

    private static void generateFurtherlandChunk(ServerLevel level, int chunkX, int chunkZ) {
        Random rng = new Random(level.getSeed() ^ ((long) chunkX << 32) ^ chunkZ);
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;

        int baseY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, baseX + 8, baseZ + 8);

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int wx = baseX + x;
                int wz = baseZ + z;

                double noise = Math.sin(wx * 0.12) * Math.cos(wz * 0.12) * 30
                        + Math.sin(wx * 0.06 + wz * 0.09) * 40
                        + Math.sin(wx * 0.03 - wz * 0.05) * 15
                        + rng.nextDouble() * 20;

                int topY = (int) (baseY + noise + 40);
                int bottomY = baseY - 15 - rng.nextInt(25);

                for (int y = bottomY; y <= topY; y++) {
                    BlockPos pos = new BlockPos(wx, y, wz);
                    if (y == topY) {
                        if (topY > baseY + 20 && rng.nextInt(2) == 0) {
                            level.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                            if (rng.nextInt(3) == 0) {
                                level.setBlock(pos.above(), Blocks.TALL_GRASS.defaultBlockState(), 2);
                            }
                            if (rng.nextInt(5) == 0) {
                                level.setBlock(pos.above(), Blocks.OAK_LEAVES.defaultBlockState(), 2);
                                level.setBlock(pos.above(2), Blocks.OAK_LEAVES.defaultBlockState(), 2);
                            }
                        } else if (topY > baseY + 25) {
                            level.setBlock(pos, rng.nextBoolean() ? Blocks.DIRT.defaultBlockState() : Blocks.STONE.defaultBlockState(), 2);
                        } else {
                            level.setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
                        }
                    } else if (y == bottomY) {
                        level.setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
                    } else if (y > topY - 5) {
                        level.setBlock(pos, rng.nextInt(2) == 0 ? Blocks.DIRT.defaultBlockState() : Blocks.STONE.defaultBlockState(), 2);
                    } else if (rng.nextInt(6) == 0) {
                        level.setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), 2);
                    } else {
                        level.setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
                    }
                }

                for (int y = bottomY; y < topY - 4; y++) {
                    if (rng.nextInt(7) == 0) {
                        BlockPos cave = new BlockPos(wx, y, wz);
                        for (int dx = -2; dx <= 2; dx++) {
                            for (int dz = -2; dz <= 2; dz++) {
                                if (rng.nextInt(3) == 0) {
                                    level.setBlock(cave.offset(dx, 0, dz), Blocks.AIR.defaultBlockState(), 2);
                                    level.setBlock(cave.offset(dx, 1, dz), Blocks.AIR.defaultBlockState(), 2);
                                    if (rng.nextInt(4) == 0) {
                                        level.setBlock(cave.offset(dx, 2, dz), Blocks.AIR.defaultBlockState(), 2);
                                    }
                                }
                            }
                        }
                    }
                }

                if (topY > baseY + 30 && rng.nextInt(4) == 0) {
                    for (int dy = 0; dy < 4; dy++) {
                        level.setBlock(new BlockPos(wx, topY + dy + 1, wz), Blocks.AIR.defaultBlockState(), 2);
                    }
                    for (int dx = -2; dx <= 2; dx++) {
                        for (int dz = -2; dz <= 2; dz++) {
                            if (rng.nextInt(2) == 0) {
                                level.setBlock(new BlockPos(wx + dx, topY + 1, wz + dz), Blocks.DIRT.defaultBlockState(), 2);
                                level.setBlock(new BlockPos(wx + dx, topY + 2, wz + dz), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                            }
                        }
                    }
                }

                if (topY > baseY + 40 && rng.nextInt(6) == 0) {
                    level.setBlock(new BlockPos(wx, topY + 1, wz), Blocks.OAK_LOG.defaultBlockState(), 2);
                    level.setBlock(new BlockPos(wx, topY + 2, wz), Blocks.OAK_LOG.defaultBlockState(), 2);
                    level.setBlock(new BlockPos(wx, topY + 3, wz), Blocks.OAK_LOG.defaultBlockState(), 2);
                    for (int dx = -3; dx <= 3; dx++) {
                        for (int dz = -3; dz <= 3; dz++) {
                            if (Math.abs(dx) + Math.abs(dz) <= 4 && rng.nextInt(3) != 0) {
                                level.setBlock(new BlockPos(wx + dx, topY + 4, wz + dz), Blocks.OAK_LEAVES.defaultBlockState(), 2);
                            }
                        }
                    }
                }

                if (rng.nextInt(12) == 0) {
                    int spireH = 5 + rng.nextInt(10);
                    for (int dy = 0; dy < spireH; dy++) {
                        level.setBlock(new BlockPos(wx, topY + dy + 1, wz), Blocks.OBSIDIAN.defaultBlockState(), 2);
                    }
                }

                if (rng.nextInt(8) == 0) {
                    for (int dy = 0; dy < 3; dy++) {
                        level.setBlock(new BlockPos(wx, bottomY - 1 - dy, wz), Blocks.LAVA.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    public static void forceFurtherlands(ServerPlayer player) {
        generateFurtherlands((ServerLevel) player.level(), player);
    }
}
