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
            if (player.level().dimension() != Level.OVERWORLD) continue;

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

            TheWindChunkManager.tick(player, now);
            TheWindBorderManager.tick(player, now);
        }
    }

    public static void triggerCorruption(ServerPlayer player) {
        if (player.level().dimension() != Level.OVERWORLD) return;
        ServerLevel level = (ServerLevel) player.level();
        int range = AbnormalitiesConfig.TW_CORRUPTION_RANGE.get() / 2;
        int chance = AbnormalitiesConfig.TW_CORRUPTION_CHANCE.get();
        int airChance = AbnormalitiesConfig.TW_CORRUPTION_AIR_CHANCE.get();
        BlockPos center = player.blockPosition();
        int corrupted = 0;
        LOGGER.info("[THE_WIND] Corruption scan: center={}, range={} (diameter {}), chance={}%, airChance={}%", center, range, range * 2, chance, airChance);

        int cx = center.getX() >> 4;
        int cz = center.getZ() >> 4;
        for (int x = cx - 2; x <= cx + 2; x++) {
            for (int z = cz - 2; z <= cz + 2; z++) {
                level.setChunkForced(x, z, true);
            }
        }

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

                    if (current.hasBlockEntity() && level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.BaseContainerBlockEntity container) {
                        for (int i = 0; i < container.getContainerSize(); i++) {
                            net.minecraft.world.item.ItemStack stack = container.getItem(i);
                            if (!stack.isEmpty()) {
                                level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack.copy()));
                            }
                        }
                    }

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
            scheduleCorruptionDecay(level, center, range, false);
        }
    }

    public static void triggerDestructiveCorruption(ServerPlayer player) {
        if (player.level().dimension() != Level.OVERWORLD) return;
        ServerLevel level = (ServerLevel) player.level();
        int range = AbnormalitiesConfig.TW_DESTRUCTIVE_RANGE.get() / 2;
        int chance = AbnormalitiesConfig.TW_DESTRUCTIVE_CHANCE.get();
        BlockPos center = player.blockPosition();
        int corrupted = 0;
        LOGGER.info("[THE_WIND] Destructive corruption: center={}, range={} (diameter {}), chance={}%", center, range, range * 2, chance);

        int cx = center.getX() >> 4;
        int cz = center.getZ() >> 4;
        for (int x = cx - 3; x <= cx + 3; x++) {
            for (int z = cz - 3; z <= cz + 3; z++) {
                level.setChunkForced(x, z, true);
            }
        }

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

                    if (current.hasBlockEntity() && level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.BaseContainerBlockEntity container) {
                        for (int i = 0; i < container.getContainerSize(); i++) {
                            net.minecraft.world.item.ItemStack stack = container.getItem(i);
                            if (!stack.isEmpty()) {
                                level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack.copy()));
                            }
                        }
                    }

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
            scheduleCorruptionDecay(level, center, range, true);
        }
    }

    public static void forceCorruption(ServerPlayer player) {
        if (player.level().dimension() != Level.OVERWORLD) return;
        triggerCorruption(player);
    }

    public static void forceDestructive(ServerPlayer player) {
        if (player.level().dimension() != Level.OVERWORLD) return;
        triggerDestructiveCorruption(player);
    }

    public static void forcePillar(ServerPlayer player) {
        if (player.level().dimension() != Level.OVERWORLD) return;
        TheWindPillarManager.spawnPillar(player);
    }

    public static void forceRandom(ServerPlayer player) {
        if (player.level().dimension() != Level.OVERWORLD) return;
        int roll = player.level().random.nextInt(3);
        if (roll == 0) forceCorruption(player);
        else if (roll == 1) forceDestructive(player);
        else forcePillar(player);
    }

    public static void forceTheWind(ServerPlayer player) {
        if (player.level().dimension() != Level.OVERWORLD) return;
        spawnTheWind(player);
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
        level.getServer().tell(new net.minecraft.server.TickTask(level.getServer().getTickCount() + 400, () -> {
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
