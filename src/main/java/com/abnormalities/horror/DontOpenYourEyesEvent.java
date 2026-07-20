package com.abnormalities.horror;

import com.abnormalities.WhisperManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class DontOpenYourEyesEvent extends AbstractHorrorEvent {
    private static final Map<UUID, Vec3> PRESENCE = new HashMap<>();
    private static final Map<UUID, Integer> TICKS = new HashMap<>();
    private static final Map<UUID, Float> LOOK_DURATION = new HashMap<>();
    private static final double TRIGGER_DOT = 0.97;
    private static final int DURATION = 240;

    public DontOpenYourEyesEvent() {
        super("dont_open_your_eyes", 150, 1.0);
    }

    @Override
    public boolean allowsOngoing() { return true; }

    @Override
    public void execute(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        double dist = 8 + player.getRandom().nextDouble() * 12;
        Vec3 pos = player.getEyePosition(1).add(look.scale(dist))
            .add(player.getRandom().nextGaussian(), player.getRandom().nextGaussian() * 0.5, player.getRandom().nextGaussian());
        PRESENCE.put(player.getUUID(), pos);
        TICKS.put(player.getUUID(), 0);
        LOOK_DURATION.put(player.getUUID(), 0f);
        WhisperManager.sendWhisper(player, "don't open your eyes...");
    }

    @Override
    public void onPlayerTick(ServerPlayer player) {
        Vec3 pos = PRESENCE.get(player.getUUID());
        if (pos == null) return;
        int tick = TICKS.getOrDefault(player.getUUID(), 0) + 1;
        TICKS.put(player.getUUID(), tick);

        Vec3 eyePos = player.getEyePosition(1);
        Vec3 lookVec = player.getViewVector(1);
        Vec3 toPresence = pos.subtract(eyePos).normalize();
        double dot = lookVec.dot(toPresence);

        if (dot > TRIGGER_DOT) {
            float lookTime = LOOK_DURATION.getOrDefault(player.getUUID(), 0f);
            lookTime = Math.min(lookTime + 0.05f, 1.0f);
            LOOK_DURATION.put(player.getUUID(), lookTime);

            int amp = Math.min(2 + (int)(lookTime * 3), 5);
            int dur = Math.min(40 + (int)(lookTime * 60), 100);
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, dur, amp, false, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur, Math.min(1 + (int)(lookTime * 2), 3), false, false, false));

            if (lookTime >= 0.6f && tick % 20 == 0) {
                WhisperManager.sendWhisper(player, "...stop looking...");
            }
        } else {
            if (player.hasEffect(MobEffects.BLINDNESS) && tick % 10 == 0) {
                player.removeEffect(MobEffects.BLINDNESS);
                player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            }
            LOOK_DURATION.put(player.getUUID(), Math.max(0f, LOOK_DURATION.getOrDefault(player.getUUID(), 0f) - 0.03f));
        }

        if (tick == 100) {
            WhisperManager.sendWhisper(player, "...still there...");
        } else if (tick == 180) {
            WhisperManager.sendWhisper(player, "...waiting...");
        }

        if (tick >= DURATION) {
            player.removeEffect(MobEffects.BLINDNESS);
            player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            WhisperManager.sendWhisper(player, "...gone.");
            PRESENCE.remove(player.getUUID());
            TICKS.remove(player.getUUID());
            LOOK_DURATION.remove(player.getUUID());
            HorrorEventPool.clearOngoing(player);
        }
    }

    @Override
    public void onCleanup(ServerPlayer player) {
        PRESENCE.remove(player.getUUID());
        TICKS.remove(player.getUUID());
        LOOK_DURATION.remove(player.getUUID());
        player.removeEffect(MobEffects.BLINDNESS);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
    }
}
