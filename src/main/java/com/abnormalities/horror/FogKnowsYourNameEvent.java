package com.abnormalities.horror;

import com.abnormalities.ReputationManager;
import com.abnormalities.WhisperManager;
import com.abnormalities.registry.ModSounds;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.*;

public class FogKnowsYourNameEvent extends AbstractHorrorEvent {
    private static final Map<UUID, Integer> STAGE = new HashMap<>();
    private static final Map<UUID, Integer> TIMER = new HashMap<>();

    public FogKnowsYourNameEvent() {
        super("fog_knows_your_name", 200, 1.2);
    }

    @Override
    public boolean allowsOngoing() { return true; }

    @Override
    public void execute(ServerPlayer player) {
        int rep = ReputationManager.getRep(player);
        WhisperManager.sendWhisper(player, "the fog is coming...");
        STAGE.put(player.getUUID(), 0);
        TIMER.put(player.getUUID(), 0);
    }

    @Override
    public void onPlayerTick(ServerPlayer player) {
        int stage = STAGE.getOrDefault(player.getUUID(), -1);
        if (stage < 0) return;
        int timer = TIMER.getOrDefault(player.getUUID(), 0) + 1;
        TIMER.put(player.getUUID(), timer);
        int rep = ReputationManager.getRep(player);

        switch (stage) {
            case 0 -> {
                if (timer % 40 == 0 && timer <= 200) {
                    int amp = Math.min(timer / 40, 3);
                    player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, amp, false, false, false));
                    WhisperManager.sendWhisper(player, WhisperManager.randomFragment(rep));
                }
                if (timer > 200) {
                    STAGE.put(player.getUUID(), 1);
                    TIMER.put(player.getUUID(), 0);
                }
            }
            case 1 -> {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 4, false, false, false));
                if (timer % 30 == 0) {
                    String w = timer % 60 == 0 ?
                        WhisperManager.usernameWhisper(player.getName().getString()) :
                        WhisperManager.randomFragment(rep);
                    WhisperManager.sendWhisper(player, w);
                }
                if (timer % 60 == 0 && timer / 60 < 5) {
                    double ox = (player.getRandom().nextDouble() - 0.5) * 4;
                    double oz = (player.getRandom().nextDouble() - 0.5) * 4;
                    player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                        Holder.direct(ModSounds.HEARTBEAT_SOUND.get()), SoundSource.MASTER,
                        player.getX() + ox, player.getY() + 0.5, player.getZ() + oz,
                        0.6f, 1.0f, 0));
                }
                if (timer == 180) {
                    player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                        Holder.direct(ModSounds.TINNITUS_SOUND.get()), SoundSource.MASTER,
                        player.getX(), player.getY() + 1, player.getZ(),
                        0.5f, 1.0f, 0));
                }
                if (timer > 300) {
                    STAGE.put(player.getUUID(), 2);
                    TIMER.put(player.getUUID(), 0);
                }
            }
            case 2 -> {
                if (timer % 20 == 0) {
                    player.removeEffect(MobEffects.BLINDNESS);
                    WhisperManager.sendWhisper(player, "...fog lifts...");
                }
                if (timer > 100) {
                    STAGE.remove(player.getUUID());
                    TIMER.remove(player.getUUID());
                    HorrorEventPool.clearOngoing(player);
                }
            }
        }
    }

    @Override
    public void onCleanup(ServerPlayer player) {
        STAGE.remove(player.getUUID());
        TIMER.remove(player.getUUID());
        player.removeEffect(MobEffects.BLINDNESS);
    }
}
