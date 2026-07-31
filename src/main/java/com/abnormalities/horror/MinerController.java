package com.abnormalities.horror;

import com.abnormalities.config.AbnormalitiesConfig;
import com.abnormalities.registry.ModEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;

public class MinerController {
    private static final Map<UUID, MinerSession> SESSIONS = new HashMap<>();

    private static class MinerSession {
        ServerLevel level;
        BlockPos start;
        BlockPos head;
        int ticksUntilNext = 60;
        int blocksMined = 0;
        int sinceTorch = 0;
        Direction heading;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var srv = ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        var overworld = srv.getLevel(Level.OVERWORLD);
        if (overworld == null) return;
        if (!AbnormalitiesConfig.M1NER_ENABLED.get()) return;
        long currentDay = overworld.getDayTime() / 24000L;
        if (currentDay < AbnormalitiesConfig.GRACE_PERIOD_DAYS.get()) return;

        Iterator<Map.Entry<UUID, MinerSession>> it = SESSIONS.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            MinerSession s = e.getValue();
            ServerPlayer player = s.level.getServer().getPlayerList().getPlayer(e.getKey());
            if (player == null || !player.isAlive() || !s.level.equals(player.level())) {
                it.remove();
                continue;
            }
            if (player.blockPosition().distSqr(s.head) < 144) {
                finishSession(s, e.getKey());
                it.remove();
                continue;
            }
            s.ticksUntilNext--;
            if (s.ticksUntilNext > 0) continue;
            s.ticksUntilNext = 40 + s.level.random.nextInt(60);
            if (!digStep(s)) {
                finishSession(s, e.getKey());
                it.remove();
            }
        }

        if (overworld.getGameTime() % 100 != 0) return;
        for (var sp : new java.util.ArrayList<>(overworld.getServer().getPlayerList().getPlayers())) {
            if (SESSIONS.containsKey(sp.getUUID())) continue;
            if (!sp.level().equals(overworld)) continue;
            if (sp.isSleeping()) continue;
            if (sp.blockPosition().getY() > 0) continue;
            if (overworld.getMaxLocalRawBrightness(sp.blockPosition()) > 7) continue;
            if (overworld.random.nextInt(AbnormalitiesConfig.M1NER_SPAWN_WEIGHT.get()) != 0) continue;
            startSession(sp, overworld);
        }
    }

    private static void startSession(ServerPlayer player, ServerLevel level) {
        SisterController.onMinerWarning(player);
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double dist = 30.0D + level.random.nextDouble() * 30.0D;
            int sx = player.blockPosition().getX() + (int) Math.round(Math.cos(angle) * dist);
            int sz = player.blockPosition().getZ() + (int) Math.round(Math.sin(angle) * dist);
            int sy = Math.max(level.getMinBuildHeight() + 2, player.blockPosition().getY() - 5 - level.random.nextInt(20));
            BlockPos start = new BlockPos(sx, sy, sz);
            BlockPos airSpot = scanForAir(level, start);
            if (airSpot == null) continue;
            MinerSession s = new MinerSession();
            s.level = level;
            s.start = airSpot;
            s.head = airSpot;
            double dx = player.getX() - airSpot.getX();
            double dz = player.getZ() - airSpot.getZ();
            if (Math.abs(dx) > Math.abs(dz)) s.heading = dx > 0 ? Direction.WEST : Direction.EAST;
            else s.heading = dz > 0 ? Direction.NORTH : Direction.SOUTH;
            SESSIONS.put(player.getUUID(), s);
            return;
        }
    }

    private static BlockPos scanForAir(ServerLevel level, BlockPos center) {
        for (int dy = 0; dy <= 24; dy++) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    BlockPos p = center.offset(dx, dy, dz);
                    if (p.getY() >= level.getMaxBuildHeight()) continue;
                    if (level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir()) return p;
                }
            }
        }
        return null;
    }

    private static boolean digStep(MinerSession s) {
        BlockPos next = s.head.relative(s.heading);
        if (s.blocksMined >= AbnormalitiesConfig.M1NER_MAX_TUNNEL.get()) return false;
        if (!s.level.getBlockState(next).isAir()) {
            if (s.level.getBlockState(next).is(Blocks.BEDROCK)) return false;
            if (s.level.getBlockEntity(next) != null) return false;
            s.level.destroyBlock(next, false);
            playDigSound(s, next);
        }
        BlockPos nextAbove = next.above();
        if (!s.level.getBlockState(nextAbove).isAir()) {
            if (s.level.getBlockState(nextAbove).is(Blocks.BEDROCK)) return false;
            if (s.level.getBlockEntity(nextAbove) != null) return false;
            s.level.destroyBlock(nextAbove, false);
            playDigSound(s, nextAbove);
        }
        s.head = next;
        s.blocksMined++;
        s.sinceTorch++;
        if (s.sinceTorch >= 4) {
            s.sinceTorch = 0;
            BlockPos torchPos = s.head.relative(s.heading);
            if (s.level.getBlockState(torchPos).isAir() && !s.level.getBlockState(torchPos.below()).isAir()) {
                BlockState torch = Blocks.TORCH.defaultBlockState();
                s.level.setBlock(torchPos, torch, 3);
            }
        }
        return true;
    }

    private static void playDigSound(MinerSession s, BlockPos pos) {
        s.level.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                s.level.random.nextBoolean() ? SoundEvents.STONE_BREAK : SoundEvents.DEEPSLATE_BREAK,
                SoundSource.BLOCKS, 5.0f, 0.7f + s.level.random.nextFloat() * 0.4f);
    }

    private static void finishSession(MinerSession s, UUID uuid) {
        if (s.level.random.nextInt(100) < AbnormalitiesConfig.M1NER_DUMMY_CHANCE.get()) {
            ServerPlayer player = s.level.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) {
                var nur = com.abnormalities.registry.ModEntities.NUR.get().create(s.level);
                if (nur != null) {
                    nur.moveTo(s.head.getX() + 0.5, s.head.getY() + 0.5, s.head.getZ() + 0.5, 0, 0);
                    nur.currentState = com.abnormalities.entity.NurEntity.State.DUMMY;
                    nur.currentTarget = player;
                    s.level.addFreshEntity(nur);
                }
            }
        }
    }

    public static void forceStart(ServerPlayer player) {
        if (SESSIONS.containsKey(player.getUUID())) return;
        startSession(player, (ServerLevel) player.level());
    }
}
