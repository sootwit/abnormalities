package com.abnormalities.thewind;

import com.abnormalities.config.AbnormalitiesConfig;
import com.abnormalities.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TheWindController {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|TheWind");
    private static long lastCorruption = 0;
    private static long lastDestructive = 0;
    private static long lastPillar = 0;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!AbnormalitiesConfig.TW_ENABLED.get()) return;
        var srv = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        ServerLevel overworld = srv.getLevel(Level.OVERWORLD);
        if (overworld == null) return;
        long now = overworld.getGameTime();

        for (ServerPlayer player : new java.util.ArrayList<>(srv.getPlayerList().getPlayers())) {
            if (player.tickCount % 200 != 0) continue;
            if (overworld.random.nextInt(200) != 0) continue;

            long graceTicks = (long) AbnormalitiesConfig.GRACE_PERIOD_DAYS.get() * 24000L;
            if (now < graceTicks) continue;

            double graceMult = AbnormalitiesConfig.TW_GRACE_MULT.get();

            if (AbnormalitiesConfig.TW_CORRUPTION_ENABLED.get()) {
                long cooldown = (long) (AbnormalitiesConfig.TW_CORRUPTION_COOLDOWN.get() * graceMult);
                if (now - lastCorruption >= cooldown && overworld.random.nextInt(2000) == 0) {
                    lastCorruption = now;
                    LOGGER.info("[THE_WIND] Corruption triggered for {} (cooldown remaining: {} ticks)", player.getName().getString(), cooldown - (now - lastCorruption));
                    triggerCorruption(player);
                }
            }

            if (AbnormalitiesConfig.TW_DESTRUCTIVE_ENABLED.get()) {
                long cooldown = (long) (AbnormalitiesConfig.TW_DESTRUCTIVE_COOLDOWN.get() * graceMult);
                if (now - lastDestructive >= cooldown && overworld.random.nextInt(6000) == 0) {
                    lastDestructive = now;
                    LOGGER.info("[THE_WIND] Destructive corruption triggered for {}", player.getName().getString());
                    triggerDestructiveCorruption(player);
                }
            }

            if (AbnormalitiesConfig.TW_PILLARS_ENABLED.get()) {
                long cooldown = (long) (AbnormalitiesConfig.TW_PILLARS_COOLDOWN.get() * graceMult);
                if (now - lastPillar >= cooldown && overworld.random.nextInt(3000) == 0) {
                    lastPillar = now;
                    TheWindPillarManager.spawnPillar(player);
                }
            }

            if (AbnormalitiesConfig.TW_THEWIND_ENABLED.get() && overworld.random.nextInt(5000) == 0) {
                spawnTheWind(player);
            }
        }
    }

    public static void triggerCorruption(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        int range = AbnormalitiesConfig.TW_CORRUPTION_RANGE.get() / 2;
        int chance = AbnormalitiesConfig.TW_CORRUPTION_CHANCE.get();
        int airChance = AbnormalitiesConfig.TW_CORRUPTION_AIR_CHANCE.get();
        BlockPos center = player.blockPosition();
        int corrupted = 0;
        LOGGER.info("[THE_WIND] Corruption scan: center={}, range={} (diameter {}), chance={}%, airChance={}%", center, range, range * 2, chance, airChance);

        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (!level.isLoaded(pos)) continue;
                    net.minecraft.world.level.block.state.BlockState current = level.getBlockState(pos);
                    if (current.is(com.abnormalities.registry.ModBlocks.CORRUPTION_BLOCK.get()) ||
                        current.is(com.abnormalities.registry.ModBlocks.DESTRUCTIVE_CORRUPTION_BLOCK.get()) ||
                        current.is(com.abnormalities.registry.ModBlocks.WIND_PILLAR_BLOCK.get())) continue;
                    if (current.getBlock() == net.minecraft.world.level.block.Blocks.BEDROCK) continue;

                    boolean isAir = current.isAir();
                    int rollChance = isAir ? airChance : chance;
                    if (level.random.nextInt(100) >= rollChance) continue;

                    com.abnormalities.thewind.CorruptionBlockEntity cbe = new com.abnormalities.thewind.CorruptionBlockEntity(pos, com.abnormalities.registry.ModBlocks.CORRUPTION_BLOCK.get().defaultBlockState());
                    cbe.setOriginalState(current);
                    level.setBlock(pos, com.abnormalities.registry.ModBlocks.CORRUPTION_BLOCK.get().defaultBlockState(), 3);
                    level.setBlockEntity(cbe);
                    corrupted++;
                }
            }
        }
        if (corrupted > 0) {
            LOGGER.info("[THE_WIND] Corruption placed {} blocks", corrupted);
            com.abnormalities.WhisperManager.sendWhisper(player, "the wind is touching your world. you'll see it soon.");
            scheduleCorruptionDecay(level, center, range, false);
        }
    }

    public static void triggerDestructiveCorruption(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        int range = AbnormalitiesConfig.TW_DESTRUCTIVE_RANGE.get() / 2;
        int chance = AbnormalitiesConfig.TW_DESTRUCTIVE_CHANCE.get();
        BlockPos center = player.blockPosition();
        int corrupted = 0;

        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (!level.isLoaded(pos)) continue;
                    net.minecraft.world.level.block.state.BlockState current = level.getBlockState(pos);
                    if (current.is(com.abnormalities.registry.ModBlocks.CORRUPTION_BLOCK.get()) ||
                        current.is(com.abnormalities.registry.ModBlocks.DESTRUCTIVE_CORRUPTION_BLOCK.get()) ||
                        current.is(com.abnormalities.registry.ModBlocks.WIND_PILLAR_BLOCK.get())) continue;
                    if (current.getBlock() == net.minecraft.world.level.block.Blocks.BEDROCK) continue;

                    if (level.random.nextInt(100) >= chance) continue;

                    com.abnormalities.thewind.CorruptionBlockEntity cbe = new com.abnormalities.thewind.CorruptionBlockEntity(pos, com.abnormalities.registry.ModBlocks.DESTRUCTIVE_CORRUPTION_BLOCK.get().defaultBlockState());
                    cbe.setOriginalState(current);
                    level.setBlock(pos, com.abnormalities.registry.ModBlocks.DESTRUCTIVE_CORRUPTION_BLOCK.get().defaultBlockState(), 3);
                    level.setBlockEntity(cbe);
                    corrupted++;
                }
            }
        }
        if (corrupted > 0) {
            LOGGER.info("[THE_WIND] Destructive corruption placed {} blocks", corrupted);
            com.abnormalities.WhisperManager.sendWhisper(player, "this one is different. don't break the dark ones. they don't come back.");
            scheduleCorruptionDecay(level, center, range, true);
        }
    }

    public static void forceCorruption(ServerPlayer player) {
        lastCorruption = player.level().getGameTime();
        triggerCorruption(player);
    }

    public static void forceDestructive(ServerPlayer player) {
        lastDestructive = player.level().getGameTime();
        triggerDestructiveCorruption(player);
    }

    public static void forcePillar(ServerPlayer player) {
        lastPillar = player.level().getGameTime();
        TheWindPillarManager.spawnPillar(player);
    }

    public static void forceRandom(ServerPlayer player) {
        int roll = player.level().random.nextInt(3);
        if (roll == 0) forceCorruption(player);
        else if (roll == 1) forceDestructive(player);
        else forcePillar(player);
    }

    private static void spawnTheWind(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        for (var existing : level.getEntitiesOfClass(TheWindEntity.class, player.getBoundingBox().inflate(16.0))) {
            if (existing.getTargetUUID() != null && existing.getTargetUUID().equals(player.getUUID())) return;
        }
        TheWindEntity wind = ModEntities.THE_WIND.get().create(level);
        if (wind != null) {
            wind.setTarget(player);
            float yaw = player.getYRot();
            double bx = player.getX() - Math.sin(Math.toRadians(yaw)) * 3.0;
            double bz = player.getZ() + Math.cos(Math.toRadians(yaw)) * 3.0;
            wind.moveTo(bx, player.getY(), bz, yaw, 0);
            level.addFreshEntity(wind);
        }
    }

    private static void scheduleCorruptionDecay(ServerLevel level, BlockPos center, int range, boolean isDestructive) {
        final int decayRange = range;
        final BlockPos decayCenter = center;
        final boolean decayDestructive = isDestructive;
        level.getServer().tell(new net.minecraft.server.TickTask(level.getServer().getTickCount() + 200, () -> {
            for (int x = -decayRange; x <= decayRange; x++) {
                for (int y = -decayRange; y <= decayRange; y++) {
                    for (int z = -decayRange; z <= decayRange; z++) {
                        BlockPos pos = decayCenter.offset(x, y, z);
                        if (!level.isLoaded(pos)) continue;
                        net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
                        if (decayDestructive && state.is(com.abnormalities.registry.ModBlocks.DESTRUCTIVE_CORRUPTION_BLOCK.get())) {
                            level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                        } else if (!decayDestructive && state.is(com.abnormalities.registry.ModBlocks.CORRUPTION_BLOCK.get())) {
                            BlockEntity be = level.getBlockEntity(pos);
                            if (be instanceof CorruptionBlockEntity cbe) {
                                net.minecraft.world.level.block.state.BlockState original = cbe.getOriginalState();
                                if (original != null) {
                                    level.setBlock(pos, original, 3);
                                } else {
                                    level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                                }
                            } else {
                                level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                            }
                        }
                    }
                }
            }
        }));
    }
}
