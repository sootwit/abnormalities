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
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class TheWindLureManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|TheWind|Lure");
    private static final int LURE_X = 100000;
    private static final int LURE_Z = 100000;
    private static final Map<UUID, LureState> ACTIVE_LURES = new HashMap<>();

    private static class LureState {
        BlockPos returnPos;
        int remainingTicks;
        int roomsGenerated;
        BlockPos roomCenter;
        List<BlockPos> doorways = new ArrayList<>();

        LureState(BlockPos returnPos, int duration) {
            this.returnPos = returnPos;
            this.remainingTicks = duration * 20;
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

        LureState state = new LureState(player.blockPosition(), duration);
        ACTIVE_LURES.put(player.getUUID(), state);
        LOGGER.info("[THE_WIND|Lure] Lure triggered for {} (duration={}s)", player.getName().getString(), duration);

        int sx = LURE_X + level.random.nextInt(50) * 16;
        int sz = LURE_Z + level.random.nextInt(50) * 16;
        int sy = level.getHeight(Heightmap.Types.MOTION_BLOCKING, sx, sz);

        BlockPos center = new BlockPos(sx, sy, sz);
        buildRoom(level, center, 5, 5, 5, state);
        state.roomCenter = center;
        state.roomsGenerated = 1;
        LOGGER.info("[THE_WIND|Lure] Initial room generated at {}", center);

        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false));
        level.getServer().tell(new net.minecraft.server.TickTask(level.getServer().getTickCount() + 20, () -> {
            player.teleportTo(level, sx + 0.5, sy + 1, sz + 0.5, player.getYRot(), player.getXRot());
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false));
            player.displayClientMessage(Component.literal("you open the chest. you are somewhere else now.").withStyle(ChatFormatting.DARK_PURPLE), false);
            level.playSound(null, sx, sy, sz, ModSounds.WHISPER_SOUND.get(), SoundSource.AMBIENT, 3.0f, 0.5f);
        }));
    }

    private static void buildRoom(ServerLevel level, BlockPos center, int halfW, int halfD, int height, LureState state) {
        for (int x = -halfW; x <= halfW; x++) {
            for (int z = -halfD; z <= halfD; z++) {
                for (int y = 0; y <= height; y++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (y == 0 || y == height || x == -halfW || x == halfW || z == -halfD || z == halfD) {
                        level.setBlock(pos, Blocks.SMOOTH_STONE.defaultBlockState(), 2);
                    } else {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 1; dy <= 3; dy++) {
                level.setBlock(center.offset(halfW + 1, dy, dx), Blocks.AIR.defaultBlockState(), 2);
            }
        }
        state.doorways.add(center.offset(halfW + 1, 0, 0));
        placeLanterns(level, center, halfW, halfD, height);
    }

    private static void generateNextRoom(ServerPlayer player, LureState state) {
        ServerLevel level = (ServerLevel) player.level();
        if (state.doorways.isEmpty()) return;

        BlockPos doorway = state.doorways.remove(level.random.nextInt(state.doorways.size()));
        Direction facing = getFacing(doorway, state.roomCenter);
        BlockPos newCenter = doorway.offset(facing.getStepX() * 7, 0, facing.getStepZ() * 7);

        int halfW = 3 + level.random.nextInt(4);
        int halfD = 3 + level.random.nextInt(4);
        int height = 4 + level.random.nextInt(2);

        for (int x = -halfW; x <= halfW; x++) {
            for (int z = -halfD; z <= halfD; z++) {
                for (int y = 0; y <= height; y++) {
                    BlockPos pos = newCenter.offset(x, y, z);
                    if (y == 0 || y == height || x == -halfW || x == halfW || z == -halfD || z == halfD) {
                        level.setBlock(pos, Blocks.SMOOTH_STONE.defaultBlockState(), 2);
                    } else {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        BlockPos innerDoor = doorway.offset(facing.getStepX() * halfW, 0, facing.getStepZ() * halfD);
        for (int dy = 1; dy <= 3; dy++) {
            level.setBlock(innerDoor.above(dy), Blocks.AIR.defaultBlockState(), 2);
        }

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (dir == facing.getOpposite()) continue;
            if (level.random.nextInt(3) == 0) {
                BlockPos sd = newCenter.offset(dir.getStepX() * halfW, 0, dir.getStepZ() * halfD);
                for (int dy = 1; dy <= 3; dy++) {
                    level.setBlock(sd.above(dy), Blocks.AIR.defaultBlockState(), 2);
                }
                state.doorways.add(sd);
            }
        }

        placeLanterns(level, newCenter, halfW, halfD, height);
        state.roomsGenerated++;
        state.roomCenter = newCenter;
        LOGGER.info("[THE_WIND|Lure] Room #{} generated at {} (doorways={})", state.roomsGenerated, newCenter, state.doorways.size());

        if (level.random.nextInt(5) == 0) {
            spawnAbnormality(level, newCenter);
        }
        if (level.random.nextInt(8) == 0) {
            level.setBlock(newCenter.above(), Blocks.CHEST.defaultBlockState(), 2);
            BlockEntity be = level.getBlockEntity(newCenter.above());
            if (be instanceof ChestBlockEntity chest) {
                chest.setItem(0, new net.minecraft.world.item.ItemStack(Items.PAPER, 1));
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

    private static void placeLanterns(ServerLevel level, BlockPos center, int halfW, int halfD, int height) {
        for (int[] off : new int[][]{{-halfW+1, -halfD+1}, {halfW-1, -halfD+1}, {-halfW+1, halfD-1}, {halfW-1, halfD-1}}) {
            level.setBlock(center.offset(off[0], height - 1, off[1]), Blocks.SEA_LANTERN.defaultBlockState(), 2);
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
        int elapsed = (AbnormalitiesConfig.TW_LURE_MAX_DURATION.get() * 20 - state.remainingTicks) / 20;
        LOGGER.info("[THE_WIND|Lure] Returning {} after {}s (rooms generated: {})", player.getName().getString(), elapsed, state.roomsGenerated);
        player.displayClientMessage(Component.literal("you were gone for " + elapsed + " seconds. the chest was never there.").withStyle(ChatFormatting.DARK_GRAY), false);
    }

    public static void forceLure(ServerPlayer player) {
        triggerLure(player);
    }
}
