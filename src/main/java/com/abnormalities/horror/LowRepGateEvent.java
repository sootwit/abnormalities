package com.abnormalities.horror;

import com.abnormalities.ReputationManager;
import com.abnormalities.WhisperManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;

import java.util.*;

public class LowRepGateEvent extends AbstractHorrorEvent {
    private static final Map<UUID, Integer> TICKS = new HashMap<>();
    private static final int DURATION = 400;

    public LowRepGateEvent() {
        super("anomaly_stops_hiding", 50, 2.5, 0, 300, 72000, true);
    }

    @Override
    public boolean canTrigger(ServerPlayer player, long currentTick) {
        return ReputationManager.getRep(player) <= 300
            && player.level().dimension() == Level.OVERWORLD;
    }

    @Override
    public void execute(ServerPlayer player) {
        TICKS.put(player.getUUID(), 0);
        WhisperManager.sendWhisper(player, "it knows how low you've fallen.");
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, DURATION, 2, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DURATION, 1, false, false, false));
        WhisperManager.sendWhisper(player, "the anomalies no longer fear you.");
    }

    @Override
    public void onPlayerTick(ServerPlayer player) {
        UUID uuid = player.getUUID();
        int tick = TICKS.getOrDefault(uuid, 0) + 1;
        TICKS.put(uuid, tick);
        if (tick >= DURATION) {
            TICKS.remove(uuid);
            HorrorEventPool.clearOngoing(player);
            return;
        }
        if (tick % 40 == 0) {
            WhisperManager.sendWhisper(player, WhisperManager.randomFragment(ReputationManager.getRep(player)));
        }
    }

    @Override
    public void onCleanup(ServerPlayer player) {
        TICKS.remove(player.getUUID());
        player.removeEffect(MobEffects.BLINDNESS);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
    }
}
