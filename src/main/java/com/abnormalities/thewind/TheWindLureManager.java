package com.abnormalities.thewind;

import com.abnormalities.config.AbnormalitiesConfig;
import com.abnormalities.registry.ModEntities;
import com.abnormalities.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class TheWindLureManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|TheWind|Lure");
    private static final int LURE_X = 100000;
    private static final int LURE_Z = 100000;
    private static final Map<UUID, LureState> ACTIVE_LURES = new HashMap<>();

    private static final BlockState FLOOR = Blocks.SMOOTH_STONE.defaultBlockState();
    private static final BlockState WALL = Blocks.SMOOTH_STONE.defaultBlockState();
    private static final BlockState CEIL = Blocks.SMOOTH_STONE.defaultBlockState();
    private static final BlockState LIGHT = Blocks.SEA_LANTERN.defaultBlockState();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState PRESSURE_PLATE = Blocks.STONE_PRESSURE_PLATE.defaultBlockState();
    private static final BlockState CARPET = Blocks.PURPLE_CARPET.defaultBlockState();
    private static final BlockState FENCE = Blocks.SMOOTH_STONE_SLAB.defaultBlockState();

    private static class LureState {
        BlockPos returnPos;
        int remainingTicks;
        int totalTicks;
        int roomsGenerated;
        BlockPos roomCenter;
        BlockPos chestPos;
        boolean teleported = false;
        List<BlockPos> doorways = new ArrayList<>();
        Set<Long> generatedChunks = new HashSet<>();

        LureState(BlockPos returnPos, int duration) {
            this.returnPos = returnPos;
            this.remainingTicks = duration * 20;
            this.totalTicks = duration * 20;
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!AbnormalitiesConfig.TW_ENABLED.get() || !AbnormalitiesConfig.TW_LURE_ENABLED.get()) return;
        var srv = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;

        Iterator<Map.Entry<UUID, LureState>> it = ACTIVE_LURES.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            ServerPlayer player = srv.getPlayerList().getPlayer(entry.getKey());
            LureState state = entry.getValue();
            if (player == null || !player.isAlive()) { it.remove(); continue; }

            if (!state.teleported) continue;

            state.remainingTicks--;
            if (state.remainingTicks <= 0) {
                returnPlayer(player, state);
                it.remove();
                continue;
            }

            int interval = AbnormalitiesConfig.TW_LURE_ROOM_INTERVAL.get();
            if (interval > 0 && state.remainingTicks % interval == 0 && state.roomsGenerated < AbnormalitiesConfig.TW_LURE_MAX_ROOMS.get()) {
                generateNextRoom(player, state);
            }
        }
    }

    public static boolean isInLure(ServerPlayer player) {
        return ACTIVE_LURES.containsKey(player.getUUID());
    }

    public static void triggerLure(ServerPlayer player) {
        if (!AbnormalitiesConfig.TW_LURE_ENABLED.get() || ACTIVE_LURES.containsKey(player.getUUID())) return;
        ServerLevel level = (ServerLevel) player.level();
        int min = AbnormalitiesConfig.TW_LURE_MIN_DURATION.get();
        int max = AbnormalitiesConfig.TW_LURE_MAX_DURATION.get();
        int duration = min + level.random.nextInt(Math.max(1, max - min + 1));

        double angle = level.random.nextDouble() * Math.PI * 2;
        int dist = 8 + level.random.nextInt(15);
        int cx = (int) (player.getX() + Math.cos(angle) * dist);
        int cz = (int) (player.getZ() + Math.sin(angle) * dist);
        int cy = level.getHeight(Heightmap.Types.MOTION_BLOCKING, cx, cz);
        BlockPos chestPos = new BlockPos(cx, cy, cz);

        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 2);
        BlockEntity be = level.getBlockEntity(chestPos);
        if (be instanceof ChestBlockEntity chest) {
            chest.setCustomName(Component.literal("the wind was here").withStyle(ChatFormatting.DARK_PURPLE));
        }

        LureState state = new LureState(player.blockPosition(), duration);
        state.chestPos = chestPos;
        ACTIVE_LURES.put(player.getUUID(), state);

        LOGGER.info("[THE_WIND|Lure] Lure chest placed for {} at {} (duration={}s)", player.getName().getString(), chestPos, duration);
    }

    public static void onChestOpen(ServerPlayer player, BlockPos pos) {
        LureState state = ACTIVE_LURES.get(player.getUUID());
        if (state == null || state.chestPos == null || !state.chestPos.equals(pos)) return;
        if (state.teleported) return;
        state.teleported = true;

        ServerLevel level = (ServerLevel) player.level();
        int sx = LURE_X + level.random.nextInt(50) * 16;
        int sz = LURE_Z + level.random.nextInt(50) * 16;
        int sy = 1;

        BlockPos center = new BlockPos(sx, sy, sz);
        generateRoom(level, center, 0, state);
        state.roomCenter = center;

        for (int cx = (sx >> 4) - 2; cx <= (sx >> 4) + 2; cx++) {
            for (int cz = (sz >> 4) - 2; cz <= (sz >> 4) + 2; cz++) {
                level.setChunkForced(cx, cz, true);
            }
        }

        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false));
        level.getServer().tell(new net.minecraft.server.TickTask(level.getServer().getTickCount() + 20, () -> {
            player.teleportTo(level, sx + 0.5, sy + 1, sz + 0.5, player.getYRot(), player.getXRot());
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false));
            player.displayClientMessage(Component.literal("you open the chest. you are somewhere else now.").withStyle(ChatFormatting.DARK_PURPLE), false);
            level.playSound(null, sx, sy, sz, ModSounds.WHISPER_SOUND.get(), net.minecraft.sounds.SoundSource.AMBIENT, 3.0f, 0.5f);
        }));

        LOGGER.info("[THE_WIND|Lure] Player {} teleported to lure dimension at {}", player.getName().getString(), center);
    }

    private static void generateRoom(ServerLevel level, BlockPos center, int variant, LureState state) {
        switch (variant % 6) {
            case 0 -> buildSmallRoom(level, center, state);
            case 1 -> buildCorridor(level, center, state);
            case 2 -> buildDeadEnd(level, center, state);
            case 3 -> buildMazeRoom(level, center, state);
            case 4 -> buildTrapRoom(level, center, state);
            case 5 -> buildShrineRoom(level, center, state);
        }
        state.roomsGenerated++;
        LOGGER.info("[THE_WIND|Lure] Room #{} (variant {}) at {} (doorways={})", state.roomsGenerated, variant % 6, center, state.doorways.size());
    }

    private static void buildSmallRoom(ServerLevel level, BlockPos center, LureState state) {
        fillRoom(level, center, 3, 3, 4);
        addDoorways(level, center, 3, 3, 4, state);
        placeLights(level, center, 3, 3, 4);
    }

    private static void buildCorridor(ServerLevel level, BlockPos center, LureState state) {
        fillRoom(level, center, 1, 5, 4);
        addDoorways(level, center, 1, 5, 4, state);
        placeLights(level, center, 1, 5, 4);
    }

    private static void buildDeadEnd(ServerLevel level, BlockPos center, LureState state) {
        fillRoom(level, center, 3, 3, 4);
        placeLights(level, center, 3, 3, 4);
    }

    private static void buildMazeRoom(ServerLevel level, BlockPos center, LureState state) {
        fillRoom(level, center, 5, 5, 4);
        Random rng = new Random(level.getSeed() ^ center.asLong());
        for (int x = -3; x <= 3; x += 2) {
            for (int z = -3; z <= 3; z += 2) {
                if (rng.nextInt(3) == 0) {
                    for (int dy = 1; dy <= 3; dy++) {
                        level.setBlock(center.offset(x, dy, z), WALL, 2);
                    }
                }
            }
        }
        addDoorways(level, center, 5, 5, 4, state);
        placeLights(level, center, 5, 5, 4);
    }

    private static void buildTrapRoom(ServerLevel level, BlockPos center, LureState state) {
        fillRoom(level, center, 3, 3, 4);
        level.setBlock(center.offset(0, 0, 0), PRESSURE_PLATE, 2);
        level.setBlock(center.offset(1, 0, 0), PRESSURE_PLATE, 2);
        level.setBlock(center.offset(-1, 0, 0), PRESSURE_PLATE, 2);
        level.setBlock(center.offset(0, 0, 1), PRESSURE_PLATE, 2);
        level.setBlock(center.offset(0, 0, -1), PRESSURE_PLATE, 2);
        addDoorways(level, center, 3, 3, 4, state);
        placeLights(level, center, 3, 3, 4);
    }

    private static void buildShrineRoom(ServerLevel level, BlockPos center, LureState state) {
        fillRoom(level, center, 3, 3, 5);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlock(center.offset(dx, 1, dz), CARPET, 2);
            }
        }
        level.setBlock(center.above(1), LIGHT, 2);
        addDoorways(level, center, 3, 3, 5, state);
        placeLights(level, center, 3, 3, 5);
    }

    private static void fillRoom(ServerLevel level, BlockPos center, int halfW, int halfD, int height) {
        for (int x = -halfW; x <= halfW; x++) {
            for (int z = -halfD; z <= halfD; z++) {
                for (int y = 0; y <= height; y++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (y == 0) level.setBlock(pos, FLOOR, 2);
                    else if (y == height) level.setBlock(pos, CEIL, 2);
                    else if (x == -halfW || x == halfW || z == -halfD || z == halfD) level.setBlock(pos, WALL, 2);
                    else level.setBlock(pos, AIR, 2);
                }
            }
        }
    }

    private static void addDoorways(ServerLevel level, BlockPos center, int halfW, int halfD, int height, LureState state) {
        Random rng = new Random(level.getSeed() ^ center.asLong() ^ 0xDEADBEEF);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (rng.nextInt(3) == 0) continue;
            int dx = dir.getStepX() * halfW;
            int dz = dir.getStepZ() * halfD;
            BlockPos door = center.offset(dx, 0, dz);
            for (int dy = 1; dy <= 3; dy++) {
                level.setBlock(door.above(dy), AIR, 2);
            }
            state.doorways.add(door);
        }
    }

    private static void placeLights(ServerLevel level, BlockPos center, int halfW, int halfD, int height) {
        for (int[] off : new int[][]{{-halfW, -halfD}, {halfW, -halfD}, {-halfW, halfD}, {halfW, halfD}}) {
            level.setBlock(center.offset(off[0], height - 1, off[1]), LIGHT, 2);
        }
    }

    private static void generateNextRoom(ServerPlayer player, LureState state) {
        ServerLevel level = (ServerLevel) player.level();
        if (state.doorways.isEmpty()) return;

        BlockPos doorway = state.doorways.remove(level.random.nextInt(state.doorways.size()));
        Direction facing = getFacing(doorway, state.roomCenter);
        int[] sizes = {3, 1, 3, 5, 3, 3};
        int halfW = sizes[level.random.nextInt(sizes.length)];
        int halfD = sizes[level.random.nextInt(sizes.length)];
        int height = 4 + level.random.nextInt(2);
        BlockPos newCenter = doorway.offset(facing.getStepX() * (halfW + 1), 0, facing.getStepZ() * (halfD + 1));

        int variant = state.roomsGenerated;
        generateRoom(level, newCenter, variant, state);
        state.roomCenter = newCenter;

        if (level.random.nextInt(Math.max(1, (int) (5.0 / AbnormalitiesConfig.TW_LURE_ABNORMAL_MULT.get()))) == 0) {
            spawnAbnormality(level, newCenter);
        }
        if (level.random.nextInt(8) == 0) {
            BlockPos chestPos = newCenter.above();
            level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 2);
            BlockEntity be = level.getBlockEntity(chestPos);
            if (be instanceof ChestBlockEntity chest) {
                chest.setCustomName(Component.literal("the wind was here").withStyle(ChatFormatting.DARK_PURPLE));
            }
        }
    }

    private static void spawnAbnormality(ServerLevel level, BlockPos pos) {
        int roll = level.random.nextInt(4);
        Entity entity = null;
        if (roll == 0) entity = ModEntities.NUR.get().create(level);
        else if (roll == 1) entity = ModEntities.K3W.get().create(level);
        else if (roll == 2) entity = ModEntities.IT.get().create(level);
        else if (roll == 3) entity = ModEntities.HIM.get().create(level);
        if (entity != null) {
            entity.moveTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, level.random.nextFloat() * 360, 0);
            level.addFreshEntity(entity);
        }
    }

    private static Direction getFacing(BlockPos from, BlockPos to) {
        int dx = from.getX() - to.getX();
        int dz = from.getZ() - to.getZ();
        return Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST) : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
    }

    private static void returnPlayer(ServerPlayer player, LureState state) {
        ServerLevel level = (ServerLevel) player.level();
        player.teleportTo(level, state.returnPos.getX() + 0.5, state.returnPos.getY() + 1, state.returnPos.getZ() + 0.5, player.getYRot(), player.getXRot());
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false));
        int elapsed = (state.totalTicks - state.remainingTicks) / 20;
        player.displayClientMessage(Component.literal("you were gone for " + elapsed + " seconds. the chest was never there.").withStyle(ChatFormatting.DARK_GRAY), false);
        LOGGER.info("[THE_WIND|Lure] Returning {} after {}s (rooms: {})", player.getName().getString(), elapsed, state.roomsGenerated);
    }

    @SubscribeEvent
    public static void onChestInteract(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (event.getLevel().getBlockState(event.getPos()).getBlock() == Blocks.CHEST) {
            onChestOpen(sp, event.getPos());
        }
    }

    public static void forceLure(ServerPlayer player) {
        triggerLure(player);
    }
}
