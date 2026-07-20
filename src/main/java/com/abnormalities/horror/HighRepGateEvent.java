package com.abnormalities.horror;

import com.abnormalities.ReputationManager;
import com.abnormalities.WhisperManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class HighRepGateEvent extends AbstractHorrorEvent {
    public HighRepGateEvent() {
        super("anomaly_trusts_you", 50, -0.5, 2000, 2500, 72000, false);
    }

    @Override
    public boolean canTrigger(ServerPlayer player, long currentTick) {
        return ReputationManager.getRep(player) >= 2000
            && player.level().getGameTime() % 24000L < 13000L;
    }

    @Override
    public void execute(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 2, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 1200, 1, false, false, false));
        WhisperManager.sendWhisper(player, "it acknowledges you. you are safe.");
        WhisperManager.sendWhisper(player, "the anomalies respect your presence.");
    }
}
