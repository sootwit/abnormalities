package com.abnormalities.horror;

import com.abnormalities.ReputationManager;
import com.abnormalities.WhisperManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;

public class LowRepGateEvent extends AbstractHorrorEvent {
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
        WhisperManager.sendWhisper(player, "it knows how low you've fallen.");
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 400, 2, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 400, 1, false, false, false));
        WhisperManager.sendWhisper(player, "the anomalies no longer fear you.");
    }

    @Override
    public void onPlayerTick(ServerPlayer player) {
        if (player.tickCount % 40 == 0 && !player.level().isClientSide) {
            WhisperManager.sendWhisper(player, WhisperManager.randomFragment(ReputationManager.getRep(player)));
        }
    }

    @Override
    public void onCleanup(ServerPlayer player) {
        player.removeEffect(MobEffects.BLINDNESS);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
    }
}
