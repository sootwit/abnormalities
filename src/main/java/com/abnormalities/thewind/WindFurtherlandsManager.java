package com.abnormalities.thewind;

import com.abnormalities.AbnormalitiesMod;
import com.abnormalities.ReputationManager;
import com.abnormalities.config.AbnormalitiesConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
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
    private static int chunksGeneratedThisSession = 0;

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

        GameRules rules = level.getGameRules();
        boolean prevMobSpawning = rules.getBoolean(GameRules.RULE_DOMOBSPAWNING);
        boolean prevTileDrops = rules.getBoolean(GameRules.RULE_DOBLOCKDROPS);
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
        rules.getRule(GameRules.RULE_DOBLOCKDROPS).set(false, level.getServer());

        try {
            List<BlockPos> generatedLeaves = new ArrayList<>();
            for (int cx = chunkX - 4; cx <= chunkX + 3; cx++) {
                for (int cz = chunkZ - 4; cz <= chunkZ + 3; cz++) {
                    long key = ((long) cx & 0xFFFFFFFFL) << 32 | ((long) cz & 0xFFFFFFFFL);
                    if (GENERATED_CHUNKS.containsKey(key)) continue;
                    if (GENERATED_CHUNKS.size() > 40) GENERATED_CHUNKS.clear();
                    GENERATED_CHUNKS.put(key, level.getGameTime());
                    chunksGeneratedThisSession++;
                    LOGGER.info("[THE_WIND|Furtherlands] Generating chunk ({}, {}) [total chunks: {}]", cx, cz, GENERATED_CHUNKS.size());
                    generateFurtherlandChunk(level, cx, cz, generatedLeaves);
                }
            }
            removeOrphanLeaves(level, generatedLeaves);
        } finally {
            rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(prevMobSpawning, level.getServer());
            rules.getRule(GameRules.RULE_DOBLOCKDROPS).set(prevTileDrops, level.getServer());
        }
    }

    private static void generateFurtherlandChunk(ServerLevel level, int chunkX, int chunkZ, List<BlockPos> generatedLeaves) {
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
                            level.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), 34);
                            if (rng.nextInt(3) == 0) {
                                level.setBlock(pos.above(), Blocks.TALL_GRASS.defaultBlockState(), 34);
                            }

                        } else if (topY > baseY + 25) {
                            level.setBlock(pos, rng.nextBoolean() ? Blocks.DIRT.defaultBlockState() : Blocks.STONE.defaultBlockState(), 34);
                        } else {
                            level.setBlock(pos, Blocks.STONE.defaultBlockState(), 34);
                        }
                    } else if (y == bottomY) {
                        level.setBlock(pos, Blocks.STONE.defaultBlockState(), 34);
                    } else if (y > topY - 5) {
                        int midR = rng.nextInt(5);
                        BlockState midBlock = midR == 0 ? Blocks.NETHERRACK.defaultBlockState()
                                : midR == 1 ? Blocks.SOUL_SAND.defaultBlockState()
                                : midR == 2 ? Blocks.END_STONE.defaultBlockState()
                                : rng.nextInt(2) == 0 ? Blocks.DIRT.defaultBlockState()
                                : Blocks.STONE.defaultBlockState();
                        level.setBlock(pos, midBlock, 34);
                    } else if (rng.nextInt(6) == 0) {
                        level.setBlock(pos, Blocks.BEDROCK.defaultBlockState(), 34);
                    } else {
                        BlockState[] deep = {Blocks.STONE.defaultBlockState(), Blocks.DEEPSLATE.defaultBlockState(), Blocks.TUFF.defaultBlockState(), Blocks.BLACKSTONE.defaultBlockState(), Blocks.BASALT.defaultBlockState()};
                        level.setBlock(pos, deep[rng.nextInt(deep.length)], 34);
                    }
                }

                for (int y = bottomY; y < topY - 4; y++) {
                    if (rng.nextInt(7) == 0) {
                        BlockPos cave = new BlockPos(wx, y, wz);
                        for (int dx = -2; dx <= 2; dx++) {
                            for (int dz = -2; dz <= 2; dz++) {
                                if (rng.nextInt(3) == 0) {
                                    level.setBlock(cave.offset(dx, 0, dz), Blocks.AIR.defaultBlockState(), 34);
                                    level.setBlock(cave.offset(dx, 1, dz), Blocks.AIR.defaultBlockState(), 34);
                                    if (rng.nextInt(4) == 0) {
                                        level.setBlock(cave.offset(dx, 2, dz), Blocks.AIR.defaultBlockState(), 34);
                                    }
                                }
                            }
                        }
                    }
                }

                if (topY > baseY + 30 && rng.nextInt(4) == 0) {
                    for (int dy = 0; dy < 4; dy++) {
                        level.setBlock(new BlockPos(wx, topY + dy + 1, wz), Blocks.AIR.defaultBlockState(), 34);
                    }
                    for (int dx = -2; dx <= 2; dx++) {
                        for (int dz = -2; dz <= 2; dz++) {
                            if (rng.nextInt(2) == 0) {
                                level.setBlock(new BlockPos(wx + dx, topY + 1, wz + dz), Blocks.DIRT.defaultBlockState(), 34);
                                level.setBlock(new BlockPos(wx + dx, topY + 2, wz + dz), Blocks.GRASS_BLOCK.defaultBlockState(), 34);
                            }
                        }
                    }
                }

                if (topY > baseY + 40 && rng.nextInt(6) == 0) {
                    int woodType = rng.nextInt(4);
                    net.minecraft.world.level.block.Block log, leaves;
                    if (woodType == 1) { log = Blocks.SPRUCE_LOG; leaves = Blocks.SPRUCE_LEAVES; }
                    else if (woodType == 2) { log = Blocks.DARK_OAK_LOG; leaves = Blocks.DARK_OAK_LEAVES; }
                    else if (woodType == 3) { log = Blocks.CHERRY_LOG; leaves = Blocks.CHERRY_LEAVES; }
                    else { log = Blocks.OAK_LOG; leaves = Blocks.OAK_LEAVES; }
                    level.setBlock(new BlockPos(wx, topY + 1, wz), log.defaultBlockState(), 34);
                    level.setBlock(new BlockPos(wx, topY + 2, wz), log.defaultBlockState(), 34);
                    level.setBlock(new BlockPos(wx, topY + 3, wz), log.defaultBlockState(), 34);
                    for (int dx = -3; dx <= 3; dx++) {
                        for (int dz = -3; dz <= 3; dz++) {
                            if (Math.abs(dx) + Math.abs(dz) <= 4 && rng.nextInt(3) != 0) {
                                BlockPos leafPos = new BlockPos(wx + dx, topY + 4, wz + dz);
                                level.setBlock(leafPos, leaves.defaultBlockState(), 34);
                                generatedLeaves.add(leafPos);
                            }
                        }
                    }
                }

                if (rng.nextInt(12) == 0) {
                    int spireH = 5 + rng.nextInt(10);
                    for (int dy = 0; dy < spireH; dy++) {
                        level.setBlock(new BlockPos(wx, topY + dy + 1, wz), Blocks.BEDROCK.defaultBlockState(), 34);
                    }
                }

                if (rng.nextInt(8) == 0) {
                    for (int dy = 0; dy < 3; dy++) {
                        level.setBlock(new BlockPos(wx, bottomY - 1 - dy, wz), Blocks.LAVA.defaultBlockState(), 34);
                    }
                }
            }
        }
    }

    private static void removeOrphanLeaves(ServerLevel level, List<BlockPos> leaves) {
        Iterator<BlockPos> it = leaves.iterator();
        while (it.hasNext()) {
            BlockPos pos = it.next();
            BlockState state = level.getBlockState(pos);
            if (!state.is(Blocks.OAK_LEAVES) && !state.is(Blocks.SPRUCE_LEAVES)
                    && !state.is(Blocks.DARK_OAK_LEAVES) && !state.is(Blocks.CHERRY_LEAVES)) {
                it.remove();
                continue;
            }
            boolean nearLog = false;
            for (int dx = -4; dx <= 4 && !nearLog; dx++) {
                for (int dy = -4; dy <= 4 && !nearLog; dy++) {
                    for (int dz = -4; dz <= 4 && !nearLog; dz++) {
                        if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) <= 4) {
                            BlockState neighbor = level.getBlockState(pos.offset(dx, dy, dz));
                            if (neighbor.is(Blocks.OAK_LOG) || neighbor.is(Blocks.SPRUCE_LOG)
                                    || neighbor.is(Blocks.DARK_OAK_LOG) || neighbor.is(Blocks.CHERRY_LOG)) {
                                nearLog = true;
                            }
                        }
                    }
                }
            }
            if (!nearLog) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
            }
        }
    }

    public static void forceFurtherlands(ServerPlayer player) {
        generateFurtherlands((ServerLevel) player.level(), player);
    }
}
