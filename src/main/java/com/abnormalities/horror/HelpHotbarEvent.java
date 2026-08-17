package com.abnormalities.horror;

import com.abnormalities.WhisperManager;
import com.abnormalities.registry.ModSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class HelpHotbarEvent extends AbstractHorrorEvent {
    private static final Map<UUID, HelpState> ACTIVE = new HashMap<>();

    private static class HelpState {
        int ticks;
        boolean mobSpawned;
    }

    public HelpHotbarEvent() {
        super("hotbar_help", 60, 0.7);
    }

    @Override
    public boolean allowsOngoing() { return true; }

    @Override
    public void execute(ServerPlayer player) {
        UUID uuid = player.getUUID();
        HelpState st = new HelpState();
        st.ticks = 0;
        st.mobSpawned = false;
        ACTIVE.put(uuid, st);
        WhisperManager.sendActionBar(player, "HELP", ChatFormatting.RED);
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0, false, false, false));
    }

    @Override
    public void onPlayerTick(ServerPlayer player) {
        UUID uuid = player.getUUID();
        HelpState st = ACTIVE.get(uuid);
        if (st == null) return;
        st.ticks++;

        if (st.ticks > 180) {
            cleanup(player);
            return;
        }

        if (st.ticks <= 60) {
            if (st.ticks % 30 == 0) {
                WhisperManager.sendActionBar(player, "HELP", ChatFormatting.RED);
            }
            if (st.ticks % 20 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0, false, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, false, false, false));
            }
        } else {
            if (!st.mobSpawned) {
                st.mobSpawned = true;
                WhisperManager.sendActionBar(player, "HELP", ChatFormatting.RED);
                spawnMobs(player);
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 140, 1, false, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 140, 1, false, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 140, 1, false, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 140, 0, false, false, false));
            }
            if (st.ticks % 20 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 40, 1, false, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, false, false));
            }
            if (st.ticks % 30 == 0) {
                playEscalatingSound(player, st.ticks);
            }
        }
    }

    private void spawnMobs(ServerPlayer player) {
        var rng = player.getRandom();
        int count = 2 + rng.nextInt(2);
        for (int i = 0; i < count; i++) {
            double angle = rng.nextDouble() * Math.PI * 2;
            double dist = 6.0 + rng.nextDouble() * 8.0;
            double x = player.getX() + Math.cos(angle) * dist;
            double z = player.getZ() + Math.sin(angle) * dist;
            int y = player.level().getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, new BlockPos((int) x, (int) player.getY(), (int) z)).getY();
            Mob mob;
            switch (rng.nextInt(3)) {
                case 0 -> mob = EntityType.ZOMBIE.create(player.level());
                case 1 -> mob = EntityType.SKELETON.create(player.level());
                default -> mob = EntityType.CREEPER.create(player.level());
            }
            if (mob != null) {
                mob.moveTo(x, y, z, rng.nextFloat() * 360.0F, 0.0F);
                mob.finalizeSpawn((ServerLevel) player.level(), player.level().getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
                player.level().addFreshEntity(mob);
            }
        }
    }

    private void playEscalatingSound(ServerPlayer player, int tick) {
        float progress = (float) (tick - 60) / 120.0F;
        float vol = 0.3F + progress * 1.7F;
        float pitch = 1.0F - progress * 0.5F;
        Vec3 pos = player.position();
        var rng = player.getRandom();
        double ox = (rng.nextDouble() - 0.5) * 6.0;
        double oz = (rng.nextDouble() - 0.5) * 6.0;
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
            Holder.direct(ModSounds.NUR_SOUND.get()), SoundSource.MASTER,
            pos.x + ox, pos.y + 1.0, pos.z + oz,
            vol, pitch, 0));
        if (progress > 0.5F) {
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                Holder.direct(ModSounds.HEARTBEAT_SOUND.get()), SoundSource.MASTER,
                pos.x, pos.y + 0.5, pos.z,
                vol * 0.8F, pitch, 0));
        }
    }

    private static void cleanup(ServerPlayer player) {
        UUID uuid = player.getUUID();
        ACTIVE.remove(uuid);
        player.removeEffect(MobEffects.CONFUSION);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(MobEffects.DIG_SLOWDOWN);
        player.removeEffect(MobEffects.BLINDNESS);
        HorrorEventPool.clearOngoing(player);
    }

    @Override
    public void onCleanup(ServerPlayer player) {
        UUID uuid = player.getUUID();
        ACTIVE.remove(uuid);
        player.removeEffect(MobEffects.CONFUSION);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(MobEffects.DIG_SLOWDOWN);
        player.removeEffect(MobEffects.BLINDNESS);
    }
}
