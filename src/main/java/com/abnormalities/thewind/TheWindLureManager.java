package com.abnormalities.thewind;

import com.abnormalities.config.AbnormalitiesConfig;
import com.abnormalities.registry.ModEntities;
import com.abnormalities.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.core.particles.ParticleTypes;
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
    private static final BlockState PLATE = Blocks.STONE_PRESSURE_PLATE.defaultBlockState();
    private static final BlockState CARPET = Blocks.PURPLE_CARPET.defaultBlockState();
    private static final BlockState DOOR_LOWER = Blocks.SPRUCE_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
    private static final BlockState DOOR_UPPER = Blocks.SPRUCE_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);

    private static class LureState {
        BlockPos returnPos;
        int remainingTicks;
        int totalTicks;
        int roomsGenerated;
        BlockPos roomCenter;
        BlockPos chestPos;
        boolean teleported = false;
        List<BlockPos> doorways = new ArrayList<>();
        List<int[]> roomBounds = new ArrayList<>();
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
            if (player == null || !player.isAlive()) {
                if (state.teleported) {
                    ServerLevel lureLevel = srv.getLevel(net.minecraft.world.level.Level.OVERWORLD);
                    if (lureLevel != null) {
                        int cx = state.roomCenter.getX() >> 4;
                        int cz = state.roomCenter.getZ() >> 4;
                        for (int x = cx - 2; x <= cx + 2; x++) {
                            for (int z = cz - 2; z <= cz + 2; z++) {
                                lureLevel.setChunkForced(x, z, false);
                            }
                        }
                    }
                }
                it.remove();
                continue;
            }
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
        buildEntryRoom(level, center, state);
        state.roomCenter = center;
        state.roomBounds.add(new int[]{center.getX() - 3, center.getZ() - 3, center.getX() + 3, center.getZ() + 3});

        for (int cx = (sx >> 4) - 2; cx <= (sx >> 4) + 2; cx++) {
            for (int cz = (sz >> 4) - 2; cz <= (sz >> 4) + 2; cz++) {
                level.setChunkForced(cx, cz, true);
            }
        }

        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false));
        level.getServer().tell(new net.minecraft.server.TickTask(level.getServer().getTickCount() + 20, () -> {
            player.teleportTo(level, sx + 0.5, sy + 1, sz + 0.5, player.getYRot(), player.getXRot());
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false));
        }));
        LOGGER.info("[THE_WIND|Lure] Player {} teleported to lure at {}", player.getName().getString(), center);
    }

    private static void buildEntryRoom(ServerLevel level, BlockPos center, LureState state) {
        fillRoom(level, center, 3, 3, 4);
        addDoorways(level, center, 3, 3, 4, state, true);
        placeLights(level, center, 3, 3, 4);
    }

    private static void generateNextRoom(ServerPlayer player, LureState state) {
        ServerLevel level = (ServerLevel) player.level();
        if (state.doorways.isEmpty()) return;

        BlockPos doorway = state.doorways.remove(level.random.nextInt(state.doorways.size()));
        Direction facing = getFacing(state.roomCenter, doorway);

        int roll = level.random.nextInt(100);
        int halfW, halfD;
        int variant = state.roomsGenerated % 6;
        if (roll < 80) {
            halfW = 3 + (variant % 3);
            halfD = 3 + ((variant + 1) % 3);
        } else if (roll < 90) {
            halfW = 5;
            halfD = 5;
        } else {
            halfW = 3;
            halfD = 3;
        }

        BlockPos newCenter = doorway.offset(facing.getStepX() * (halfW + 1), 0, facing.getStepZ() * (halfD + 1));
        int nMinX = newCenter.getX() - halfW;
        int nMaxX = newCenter.getX() + halfW;
        int nMinZ = newCenter.getZ() - halfD;
        int nMaxZ = newCenter.getZ() + halfD;

        for (int[] b : state.roomBounds) {
            if (nMinX <= b[2] && nMaxX >= b[0] && nMinZ <= b[3] && nMaxZ >= b[1]) {
                LOGGER.info("[THE_WIND|Lure] Skipping room at {} - would overlap existing room", newCenter);
                return;
            }
        }

        if (roll < 80) {
            buildStandardRoom(level, doorway, facing, state, variant);
        } else if (roll < 90) {
            buildNurDarkRoom(level, doorway, facing, state);
        } else {
            buildK3wCloneRoom(level, doorway, facing, state);
        }

        state.roomBounds.add(new int[]{nMinX, nMinZ, nMaxX, nMaxZ});
        state.roomsGenerated++;

        for (int dy = 1; dy <= 3; dy++) {
            for (int a = -1; a <= 1; a++) {
                BlockPos clear;
                if (facing.getAxis() == Direction.Axis.X) {
                    clear = new BlockPos(doorway.getX(), newCenter.getY() + dy, doorway.getZ() + a);
                } else {
                    clear = new BlockPos(doorway.getX() + a, newCenter.getY() + dy, doorway.getZ());
                }
                level.setBlock(clear, Blocks.AIR.defaultBlockState(), 2);
            }
        }

        LOGGER.info("[THE_WIND|Lure] Room #{} at {} (doorways={})", state.roomsGenerated, state.roomCenter, state.doorways.size());
    }

    private static void buildStandardRoom(ServerLevel level, BlockPos doorway, Direction facing, LureState state, int variant) {
        int halfW = 3 + (variant % 3);
        int halfD = 3 + ((variant + 1) % 3);
        int height = 4 + (variant % 2);
        BlockPos center = doorway.offset(facing.getStepX() * (halfW + 1), 0, facing.getStepZ() * (halfD + 1));

        fillRoom(level, center, halfW, halfD, height);
        clearDoorwayPassage(level, center, doorway, facing, halfW, halfD, height);
        addDoorways(level, center, halfW, halfD, height, state, true);
        placeLights(level, center, halfW, halfD, height);

        if (level.random.nextInt(8) == 0) {
            placeChest(level, center.above());
        }
        if (level.random.nextInt(5) == 0) {
            spawnAbnormality(level, center);
        }
        state.roomCenter = center;
    }

    private static void buildNurDarkRoom(ServerLevel level, BlockPos doorway, Direction facing, LureState state) {
        int halfW = 5;
        int halfD = 5;
        int height = 4;
        BlockPos center = doorway.offset(facing.getStepX() * (halfW + 1), 0, facing.getStepZ() * (halfD + 1));

        fillRoom(level, center, halfW, halfD, height);

        for (int x = -halfW; x <= halfW; x++) {
            for (int z = -halfD; z <= halfD; z++) {
                for (int y = 1; y < height; y++) {
                    level.setBlock(center.offset(x, y, z), Blocks.BLACK_WOOL.defaultBlockState(), 2);
                }
            }
        }

        clearDoorwayPassage(level, center, doorway, facing, halfW, halfD, height);
        addDoorways(level, center, halfW, halfD, height, state, true);

        Entity nur = ModEntities.NUR.get().create(level);
        if (nur != null) {
            nur.moveTo(center.getX() + 0.5, 1, center.getZ() + 0.5, 0, 0);
            level.addFreshEntity(nur);
        }

        state.roomCenter = center;
        LOGGER.info("[THE_WIND|Lure] Nur dark room at {}", center);
    }

    private static void buildK3wCloneRoom(ServerLevel level, BlockPos doorway, Direction facing, LureState state) {
        int halfW = 3;
        int halfD = 3;
        int height = 4;
        BlockPos center = doorway.offset(facing.getStepX() * (halfW + 1), 0, facing.getStepZ() * (halfD + 1));

        fillRoom(level, center, halfW, halfD, height);
        placeLights(level, center, halfW, halfD, height);

        for (int i = 0; i < 3; i++) {
            Entity k3w = ModEntities.K3W.get().create(level);
            if (k3w != null) {
                double ox = (level.random.nextDouble() - 0.5) * 4;
                double oz = (level.random.nextDouble() - 0.5) * 4;
                k3w.moveTo(center.getX() + ox, 1, center.getZ() + oz, 0, 0);
                level.addFreshEntity(k3w);
            }
        }

        clearDoorwayPassage(level, center, doorway, facing, halfW, halfD, height);
        addDoorways(level, center, halfW, halfD, height, state, true);
        state.roomCenter = center;
        LOGGER.info("[THE_WIND|Lure] K3W clone room at {}", center);
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

    private static void addDoorways(ServerLevel level, BlockPos center, int halfW, int halfD, int height, LureState state, boolean withDoor) {
        Random rng = new Random(level.getSeed() ^ center.asLong() ^ 0xDEADBEEF);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (rng.nextInt(3) == 0) continue;
            int dx = dir.getStepX() * halfW;
            int dz = dir.getStepZ() * halfD;
            BlockPos door = center.offset(dx, 0, dz);
            for (int dy = 1; dy <= 3; dy++) {
                level.setBlock(door.above(dy), AIR, 2);
            }
            if (withDoor) {
                placeDoor(level, door, dir);
            }
            state.doorways.add(door);
        }
    }

    private static void placeDoor(ServerLevel level, BlockPos pos, Direction facing) {
        level.setBlock(pos.above(), DOOR_LOWER.setValue(DoorBlock.FACING, facing), 2);
        level.setBlock(pos.above(2), DOOR_UPPER.setValue(DoorBlock.FACING, facing), 2);
        level.setBlock(pos.below().offset(facing.getStepX(), 0, facing.getStepZ()), PLATE, 2);
    }

    private static void placeLights(ServerLevel level, BlockPos center, int halfW, int halfD, int height) {
        for (int[] off : new int[][]{{-halfW, -halfD}, {halfW, -halfD}, {-halfW, halfD}, {halfW, halfD}}) {
            level.setBlock(center.offset(off[0], height - 1, off[1]), LIGHT, 2);
        }
    }

    private static void clearDoorwayPassage(ServerLevel level, BlockPos roomCenter, BlockPos doorway, Direction facing, int halfW, int halfD, int height) {
        int dx = -facing.getStepX();
        int dz = -facing.getStepZ();
        int wallX = dx * halfW;
        int wallZ = dz * halfD;
        for (int dy = 1; dy <= 3; dy++) {
            for (int a = -1; a <= 1; a++) {
                int cx, cz;
                if (facing.getAxis() == Direction.Axis.X) {
                    cx = roomCenter.getX() + wallX;
                    cz = roomCenter.getZ() + a;
                } else {
                    cx = roomCenter.getX() + a;
                    cz = roomCenter.getZ() + wallZ;
                }
                level.setBlock(new BlockPos(cx, roomCenter.getY() + dy, cz), AIR, 2);
            }
        }
    }

    private static void placeChest(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 2);
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ChestBlockEntity chest) {
            chest.setCustomName(Component.literal("the wind was here").withStyle(ChatFormatting.DARK_PURPLE));
        }
    }

    private static void spawnAbnormality(ServerLevel level, BlockPos pos) {
        int roll = level.random.nextInt(3);
        Entity entity = null;
        if (roll == 0) entity = ModEntities.NUR.get().create(level);
        else if (roll == 1) entity = ModEntities.IT.get().create(level);
        else if (roll == 2) entity = ModEntities.HIM.get().create(level);
        if (entity != null) {
            entity.moveTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, level.random.nextFloat() * 360, 0);
            level.addFreshEntity(entity);
        }
    }

    private static Direction getFacing(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();
        return Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST) : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
    }

    private static void returnPlayer(ServerPlayer player, LureState state) {
        ServerLevel level = (ServerLevel) player.level();
        player.teleportTo(level, state.returnPos.getX() + 0.5, state.returnPos.getY() + 1, state.returnPos.getZ() + 0.5, player.getYRot(), player.getXRot());
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false));
        int elapsed = (state.totalTicks - state.remainingTicks) / 20;
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

    @SubscribeEvent
    public static void onPlayerLogout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LureState state = ACTIVE_LURES.remove(player.getUUID());
            if (state != null && state.teleported) {
                returnPlayer(player, state);
            }
        }
    }

    private static void unforceChunks(ServerLevel level, int cx, int cz) {
        for (int x = cx - 2; x <= cx + 2; x++) {
            for (int z = cz - 2; z <= cz + 2; z++) {
                level.setChunkForced(x, z, false);
            }
        }
    }
}
