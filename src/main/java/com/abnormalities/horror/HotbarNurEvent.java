package com.abnormalities.horror;

import com.abnormalities.WhisperManager;
import com.abnormalities.entity.NurEntity;
import com.abnormalities.registry.ModEntities;
import com.abnormalities.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class HotbarNurEvent extends AbstractHorrorEvent {
    private static final Map<UUID, Integer> TIMER = new HashMap<>();
    private static final Map<UUID, List<BlockPos>> BLOCKS = new HashMap<>();
    private static final Map<UUID, Entity> NURS = new HashMap<>();

    private static final int WARNING_END = 60;
    private static final int EVENT_END = 140;
    private static boolean debug_mode = false;

    public HotbarNurEvent() {
        super("hotbar_nur", 70, 0.6);
    }

    @Override
    public boolean allowsOngoing() { return true; }

    @Override
    public void execute(ServerPlayer player) {
        UUID uuid = player.getUUID();
        TIMER.put(uuid, 0);
        BLOCKS.put(uuid, new ArrayList<>());
        WhisperManager.sendActionBar(player, "nur");
    }

    @Override
    public void onPlayerTick(ServerPlayer player) {
        UUID uuid = player.getUUID();
        int t = TIMER.merge(uuid, 1, Integer::sum);

        if (t > EVENT_END) {
            onCleanup(player);
            HorrorEventPool.clearOngoing(player);
            return;
        }

        if (t <= WARNING_END) {
            tickWarning(player, t);
        } else {
            tickEffect(player, t);
        }
    }

    private void tickWarning(ServerPlayer player, int t) {
        if (t % 20 == 0) {
            WhisperManager.sendActionBar(player, "nur");
        }
        float vol = 0.3f + (t / (float) WARNING_END) * 0.5f;
        if (t % 20 == 0) {
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                Holder.direct(ModSounds.NUR_SOUND.get()), SoundSource.MASTER,
                player.getX(), player.getY() + 1, player.getZ(),
                vol, 0.8f, 0));
        }
        if (t >= 40) {
            int amp = Math.min((t - 40) / 10, 2);
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, amp, false, false, false));
        }
    }

    private void tickEffect(ServerPlayer player, int t) {
        UUID uuid = player.getUUID();

        if (t == WARNING_END + 1) {
            spawnDummyNur(player);
            placeCorruption(player);
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                Holder.direct(ModSounds.NUR_SOUND.get()), SoundSource.MASTER,
                player.getX(), player.getY() + 1, player.getZ(),
                1.5f, 0.6f, 0));
        }

        if (t == WARNING_END + 40) {
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                SoundEvents.AMBIENT_CAVE, SoundSource.MASTER,
                player.getX(), player.getY() + 1, player.getZ(),
                2.0f, 0.3f, 0));
        }

        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 30, 1, false, false, false));

        if (t % 30 == 0 && t < EVENT_END - 10) {
            float vol = 0.8f + ((t - WARNING_END) / (float) (EVENT_END - WARNING_END)) * 0.7f;
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                Holder.direct(ModSounds.NUR_SOUND.get()), SoundSource.MASTER,
                player.getX(), player.getY() + 1, player.getZ(),
                vol, 0.7f, 0));
        }
    }

    private void spawnDummyNur(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        double angle = level.random.nextDouble() * Math.PI * 2;
        double dist = 10.0 + level.random.nextDouble() * 5.0;
        double sx = player.getX() + Math.cos(angle) * dist;
        double sz = player.getZ() + Math.sin(angle) * dist;
        int sy = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) sx, (int) sz);
        BlockPos spawnPos = BlockPos.containing(sx, sy, sz);
        if (!level.getBlockState(spawnPos.below()).canOcclude()) return;
        if (!level.getBlockState(spawnPos).canBeReplaced()) return;

        // TODO: maybe make this configurable? spawn chance, distance, etc.
        // for now just hardcoded because it works
        NurEntity nur = ModEntities.NUR.get().create(level);
        if (nur == null) return;
        nur.moveTo(sx + 0.5, sy + 1, sz + 0.5, 0, 0);
        nur.currentState = NurEntity.State.STALKING_DUMMY;
        level.addFreshEntity(nur);
        NURS.put(player.getUUID(), nur);
    }

    private void placeCorruption(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        BlockState sculk = Blocks.SCULK_VEIN.defaultBlockState();
        List<BlockPos> placed = new ArrayList<>();
        int count = 4 + level.random.nextInt(4);
        for (int i = 0; i < count; i++) {
            double ox = (level.random.nextDouble() - 0.5) * 6;
            double oz = (level.random.nextDouble() - 0.5) * 6;
            BlockPos pos = BlockPos.containing(player.getX() + ox, player.getY(), player.getZ() + oz);
            if (level.getBlockState(pos).canBeReplaced() && level.getBlockState(pos.below()).canOcclude()) {
                level.setBlockAndUpdate(pos, sculk);
                placed.add(pos);
            }
        }
        BLOCKS.put(player.getUUID(), placed);
    }

    @Override
    public void onCleanup(ServerPlayer player) {
        UUID uuid = player.getUUID();
        TIMER.remove(uuid);
        player.removeEffect(MobEffects.DARKNESS);

        Entity nur = NURS.remove(uuid);
        if (nur != null && nur.isAlive()) {
            nur.discard();
        }

        List<BlockPos> placed = BLOCKS.remove(uuid);
        if (placed != null) {
            ServerLevel level = (ServerLevel) player.level();
            for (BlockPos pos : placed) {
                if (level.getBlockState(pos).is(Blocks.SCULK_VEIN)) {
                    level.removeBlock(pos, false);
                }
            }
        }
    }
}
