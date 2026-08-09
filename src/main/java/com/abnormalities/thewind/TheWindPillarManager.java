package com.abnormalities.thewind;

import com.abnormalities.config.AbnormalitiesConfig;
import com.abnormalities.registry.ModBlocks;
import com.abnormalities.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;

public class TheWindPillarManager {
    private static final List<Block> HOUSE_BLOCKS = List.of(
            Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.BIRCH_PLANKS, Blocks.JUNGLE_PLANKS,
            Blocks.ACACIA_PLANKS, Blocks.DARK_OAK_PLANKS, Blocks.MANGROVE_PLANKS, Blocks.CHERRY_PLANKS,
            Blocks.BAMBOO_PLANKS, Blocks.CRIMSON_PLANKS, Blocks.WARPED_PLANKS,
            Blocks.OAK_LOG, Blocks.SPRUCE_LOG, Blocks.BIRCH_LOG, Blocks.JUNGLE_LOG,
            Blocks.ACACIA_LOG, Blocks.DARK_OAK_LOG, Blocks.MANGROVE_LOG, Blocks.CHERRY_LOG,
            Blocks.GLASS, Blocks.GLASS_PANE, Blocks.TINTED_GLASS,
            Blocks.OAK_DOOR, Blocks.SPRUCE_DOOR, Blocks.BIRCH_DOOR, Blocks.JUNGLE_DOOR,
            Blocks.ACACIA_DOOR, Blocks.DARK_OAK_DOOR, Blocks.IRON_DOOR,
            Blocks.CHEST, Blocks.BARREL, Blocks.ENDER_CHEST,
            Blocks.CRAFTING_TABLE, Blocks.FURNACE, Blocks.BLAST_FURNACE, Blocks.SMOKER,
            Blocks.ENCHANTING_TABLE, Blocks.BREWING_STAND, Blocks.ANVIL,
            Blocks.OAK_SIGN, Blocks.SPRUCE_SIGN, Blocks.BIRCH_SIGN, Blocks.JUNGLE_SIGN,
            Blocks.ACACIA_SIGN, Blocks.DARK_OAK_SIGN, Blocks.MANGROVE_SIGN, Blocks.CHERRY_SIGN
    );

    private static final Set<Block> INDESTRUCTIBLE = Set.of(Blocks.BEDROCK);

    private static final List<ActivePillar> ACTIVE_PILLARS = new ArrayList<>();

    private static class ActivePillar {
        ServerLevel level;
        BlockPos target;
        int width, depth, halfW, halfD;
        int currentY;
        int endY;
        int tickCounter;
        int speed;
        boolean finished;

        ActivePillar(ServerLevel level, BlockPos target, int width, int depth, int startY, int endY, int speed) {
            this.level = level;
            this.target = target;
            this.width = width;
            this.depth = depth;
            this.halfW = width / 2;
            this.halfD = depth / 2;
            this.currentY = startY;
            this.endY = endY;
            this.speed = speed;
            this.tickCounter = 0;
            this.finished = false;
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Iterator<ActivePillar> it = ACTIVE_PILLARS.iterator();
        while (it.hasNext()) {
            ActivePillar p = it.next();
            if (p.finished || p.level == null || !p.level.isLoaded(p.target)) {
                it.remove();
                continue;
            }
            p.tickCounter++;
            if (p.tickCounter >= p.speed) {
                p.tickCounter = 0;
                processLayer(p);
                p.currentY--;
                if (p.currentY < p.endY) {
                    collapseRemaining(p);
                    p.finished = true;
                    it.remove();
                }
            }
        }
    }

    private static void processLayer(ActivePillar p) {
        ServerLevel level = p.level;
        boolean hitIndestructible = false;

        for (int dx = -p.halfW; dx < p.halfW; dx++) {
            for (int dz = -p.halfD; dz < p.halfD; dz++) {
                BlockPos pos = p.target.offset(dx, p.currentY, dz);
                if (!level.isLoaded(pos)) continue;
                BlockState state = level.getBlockState(pos);
                if (state.is(ModBlocks.WIND_PILLAR_BLOCK.get())) continue;
                if (INDESTRUCTIBLE.contains(state.getBlock())) {
                    hitIndestructible = true;
                    continue;
                }
                if (AbnormalitiesConfig.TW_PILLARS_DUMP_INVENTORY.get() && state.hasBlockEntity()) {
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be instanceof BaseContainerBlockEntity container) {
                        dumpContainer(level, pos, container);
                    }
                }
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                level.setBlock(pos, ModBlocks.WIND_PILLAR_BLOCK.get().defaultBlockState(), 3);
                if (AbnormalitiesConfig.TW_PILLARS_BREAK_SOUNDS.get() && !state.isAir()) {
                    level.playSound(null, pos, state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 0.3f, 0.5f);
                }
            }
        }

        for (ServerPlayer pl : level.getServer().getPlayerList().getPlayers()) {
            double dist = pl.distanceToSqr(p.target.getX(), p.currentY, p.target.getZ());
            if (dist < AbnormalitiesConfig.TW_SHAKE_RANGE.get() * AbnormalitiesConfig.TW_SHAKE_RANGE.get()) {
                float intensity = (float) AbnormalitiesConfig.TW_SHAKE_BLOCK.get().doubleValue();
                com.abnormalities.thewind.TheWindShakeHandler.triggerShake(pl, intensity, 30);
            }
        }

        checkPlayerCollision(level, p.target, p.width, p.depth, p.halfW, p.halfD, p.currentY);

        if (hitIndestructible) {
            collapseRemaining(p);
            p.finished = true;
        }
    }

    private static void collapseRemaining(ActivePillar p) {
        ServerLevel level = p.level;
        int collapsed = 0;
        for (int y = p.currentY; y >= level.getMinBuildHeight(); y--) {
            for (int dx = -p.halfW; dx < p.halfW; dx++) {
                for (int dz = -p.halfD; dz < p.halfD; dz++) {
                    BlockPos pos = p.target.offset(dx, y, dz);
                    if (!level.isLoaded(pos)) continue;
                    BlockState state = level.getBlockState(pos);
                    if (state.is(ModBlocks.WIND_PILLAR_BLOCK.get()) || INDESTRUCTIBLE.contains(state.getBlock())) continue;
                    if (AbnormalitiesConfig.TW_PILLARS_DUMP_INVENTORY.get() && state.hasBlockEntity()) {
                        BlockEntity be = level.getBlockEntity(pos);
                        if (be instanceof BaseContainerBlockEntity container) {
                            dumpContainer(level, pos, container);
                        }
                    }
                    level.setBlock(pos, ModBlocks.WIND_PILLAR_BLOCK.get().defaultBlockState(), 3);
                    collapsed++;
                }
            }
        }
        if (collapsed > 0) {
            for (ServerPlayer pl : level.getServer().getPlayerList().getPlayers()) {
                double dist = pl.distanceToSqr(p.target.getX(), p.currentY, p.target.getZ());
                if (dist < AbnormalitiesConfig.TW_SHAKE_RANGE.get() * AbnormalitiesConfig.TW_SHAKE_RANGE.get()) {
                    float intensity = (float) AbnormalitiesConfig.TW_SHAKE_GROUND.get().doubleValue();
                    com.abnormalities.thewind.TheWindShakeHandler.triggerShake(pl, intensity, 100);
                }
            }
        }
    }

    public static void spawnPillar(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        int width = AbnormalitiesConfig.TW_PILLARS_WIDTH.get();
        int depth = AbnormalitiesConfig.TW_PILLARS_DEPTH.get();
        int length = AbnormalitiesConfig.TW_PILLARS_LENGTH.get();

        if (AbnormalitiesConfig.TW_PILLARS_RANDOM_SIZE.get()) {
            width = AbnormalitiesConfig.TW_PILLARS_RANDOM_MIN_W.get() + level.random.nextInt(AbnormalitiesConfig.TW_PILLARS_RANDOM_MAX_W.get() - AbnormalitiesConfig.TW_PILLARS_RANDOM_MIN_W.get() + 1);
            depth = AbnormalitiesConfig.TW_PILLARS_RANDOM_MIN_D.get() + level.random.nextInt(AbnormalitiesConfig.TW_PILLARS_RANDOM_MAX_D.get() - AbnormalitiesConfig.TW_PILLARS_RANDOM_MIN_D.get() + 1);
            length = AbnormalitiesConfig.TW_PILLARS_RANDOM_MIN_L.get() + level.random.nextInt(AbnormalitiesConfig.TW_PILLARS_RANDOM_MAX_L.get() - AbnormalitiesConfig.TW_PILLARS_RANDOM_MIN_L.get() + 1);
        }

        BlockPos target = findTarget(player, width, depth);
        int startY = level.getMaxBuildHeight();
        int endY = Math.max(target.getY() - length, level.getMinBuildHeight());
        int speed = AbnormalitiesConfig.TW_PILLARS_SPEED.get();

        level.playSound(null, target.getX(), startY, target.getZ(),
                ModSounds.PILLAR_ALARM.get(), SoundSource.AMBIENT, 8.0f, 0.5f);

        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            double dist = p.distanceToSqr(target.getX(), startY, target.getZ());
            if (dist < AbnormalitiesConfig.TW_SHAKE_RANGE.get() * AbnormalitiesConfig.TW_SHAKE_RANGE.get()) {
                com.abnormalities.thewind.TheWindShakeHandler.triggerShake(p, (float) AbnormalitiesConfig.TW_SHAKE_DIRECT.get().doubleValue(), 150);
            }
        }

        ACTIVE_PILLARS.add(new ActivePillar(level, target, width, depth, startY, endY, speed));
    }

    private static void checkPlayerCollision(ServerLevel level, BlockPos target, int width, int depth, int halfW, int halfD, int y) {
        int damageRadius = AbnormalitiesConfig.TW_PILLARS_DAMAGE_RADIUS.get();
        int damageAmount = AbnormalitiesConfig.TW_PILLARS_DAMAGE_AMOUNT.get();

        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            BlockPos playerPos = player.blockPosition();
            int relX = playerPos.getX() - target.getX();
            int relZ = playerPos.getZ() - target.getZ();
            int relY = playerPos.getY() - y;

            boolean inside = relX >= -halfW && relX < halfW && relZ >= -halfD && relZ < halfD && relY >= 0 && relY <= 1;
            boolean near = !inside && Math.abs(relX) <= halfW + damageRadius && Math.abs(relZ) <= halfD + damageRadius && relY >= -1 && relY <= 2;

            if (inside) {
                player.kill();
                stealItems(player);
                for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
                    double dist = p.distanceToSqr(player.getX(), player.getY(), player.getZ());
                    if (dist < AbnormalitiesConfig.TW_SHAKE_RANGE.get() * AbnormalitiesConfig.TW_SHAKE_RANGE.get()) {
                        com.abnormalities.thewind.TheWindShakeHandler.triggerShake(p, (float) AbnormalitiesConfig.TW_SHAKE_CRUSH.get().doubleValue(), 120);
                    }
                }
            } else if (near) {
                player.hurt(level.damageSources().generic(), damageAmount);
                net.minecraft.world.phys.Vec3 knock = player.position().subtract(target.getX() + 0.5, player.getY(), target.getZ() + 0.5).normalize().scale(1.5);
                player.push(knock.x, 0.5, knock.z);
            }
        }
    }

    private static void stealItems(ServerPlayer player) {
        int min = AbnormalitiesConfig.TW_PILLARS_STEAL_MIN.get();
        int max = AbnormalitiesConfig.TW_PILLARS_STEAL_MAX.get();
        int count = min + player.level().random.nextInt(Math.max(1, max - min + 1));
        List<Integer> candidates = new ArrayList<>();
        for (int i = 9; i < 36; i++) {
            if (!player.getInventory().getItem(i).isEmpty()) candidates.add(i);
        }
        Collections.shuffle(candidates, new java.util.Random());
        int stolen = 0;
        for (int slot : candidates) {
            if (stolen >= count) break;
            player.getInventory().setItem(slot, net.minecraft.world.item.ItemStack.EMPTY);
            stolen++;
        }
    }

    private static void dumpContainer(ServerLevel level, BlockPos pos, BaseContainerBlockEntity container) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                ItemEntity item = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack.copy());
                item.setDeltaMovement(level.random.nextGaussian() * 0.3, 0.2, level.random.nextGaussian() * 0.3);
                level.addFreshEntity(item);
                container.setItem(i, net.minecraft.world.item.ItemStack.EMPTY);
            }
        }
    }

    private static BlockPos findTarget(ServerPlayer player, int width, int depth) {
        int roll = player.level().random.nextInt(100);
        int houseChance = AbnormalitiesConfig.TW_PILLARS_HOUSE_CHANCE.get();
        int directChance = AbnormalitiesConfig.TW_PILLARS_DIRECT_CHANCE.get();

        if (roll < directChance) return player.blockPosition();
        if (roll < directChance + houseChance) {
            BlockPos house = findHouse(player);
            if (house != null) return house;
        }
        return randomRing(player, width, depth);
    }

    private static BlockPos findHouse(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        int radius = AbnormalitiesConfig.TW_PILLARS_HOUSE_RADIUS.get();
        int clusterSize = AbnormalitiesConfig.TW_PILLARS_HOUSE_CLUSTER.get();
        BlockPos center = player.blockPosition();
        Map<BlockPos, Integer> scores = new HashMap<>();

        for (int x = -radius; x <= radius; x += 3) {
            for (int y = -radius; y <= radius; y += 3) {
                for (int z = -radius; z <= radius; z += 3) {
                    BlockPos check = center.offset(x, y, z);
                    if (!level.isLoaded(check)) continue;
                    int score = 0;
                    for (int dx = -3; dx <= 3; dx++) {
                        for (int dy = -3; dy <= 3; dy++) {
                            for (int dz = -3; dz <= 3; dz++) {
                                BlockPos p = check.offset(dx, dy, dz);
                                if (level.isLoaded(p) && HOUSE_BLOCKS.contains(level.getBlockState(p).getBlock())) score++;
                            }
                        }
                    }
                    if (score >= clusterSize) scores.put(check, score);
                }
            }
        }
        if (scores.isEmpty()) return null;
        return scores.entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();
    }

    private static BlockPos randomRing(ServerPlayer player, int width, int depth) {
        ServerLevel level = (ServerLevel) player.level();
        int halfW = width / 2;
        int halfD = depth / 2;
        for (int attempt = 0; attempt < 20; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            int dist = 10 + level.random.nextInt(31);
            int tx = (int) (player.getX() + Math.cos(angle) * dist);
            int tz = (int) (player.getZ() + Math.sin(angle) * dist);
            int ty = level.getHeight(Heightmap.Types.MOTION_BLOCKING, tx, tz);
            if (Math.abs((int) player.getX() - tx) <= halfW + 5 && Math.abs((int) player.getZ() - tz) <= halfD + 5) continue;
            return new BlockPos(tx, ty, tz);
        }
        return player.blockPosition().offset(20, 0, 20);
    }
}
