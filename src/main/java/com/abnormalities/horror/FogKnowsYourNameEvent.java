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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FogKnowsYourNameEvent extends AbstractHorrorEvent {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|FogKnowsYourName");
    private static final Map<UUID, FogState> ACTIVE = new HashMap<>();
    private static int totalFires = 0;

    private static class FogState {
        int stage;
        int timer;
    }

    public FogKnowsYourNameEvent() {
        super("fog_knows_your_name", 200, 1.2);
    }

    @Override
    public boolean allowsOngoing() { return true; }

    @Override
    public void execute(ServerPlayer player) {
        int rep = ReputationManager.getRep(player);
        totalFires++;
        LOGGER.info("[FogKnowsYourName] {} triggered (rep={}, totalFires={})", player.getName().getString(), rep, totalFires);
        WhisperManager.sendWhisper(player, "the fog is coming...");
        FogState st = new FogState();
        st.stage = 0;
        st.timer = 0;
        ACTIVE.put(player.getUUID(), st);
    }

    @Override
    public void onPlayerTick(ServerPlayer player) {
        UUID uuid = player.getUUID();
        FogState st = ACTIVE.get(uuid);
        if (st == null) return;
        st.timer++;
        int rep = ReputationManager.getRep(player);

        switch (st.stage) {
            case 0 -> {
                if (st.timer % 40 == 0 && st.timer <= 200) {
                    int amp = Math.min(st.timer / 40, 3);
                    LOGGER.debug("[FogKnowsYourName] {} stage 0 blind amp={}", player.getName().getString(), amp);
                    player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, amp, false, false, false));
                    WhisperManager.sendWhisper(player, WhisperManager.randomFragment(rep));
                }
                if (st.timer > 200) {
                    LOGGER.debug("[FogKnowsYourName] {} stage 0->1", player.getName().getString());
                    st.stage = 1;
                    st.timer = 0;
                }
            }
            case 1 -> {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 4, false, false, false));
                if (st.timer % 30 == 0) {
                    String w = st.timer % 60 == 0 ?
                        WhisperManager.usernameWhisper(player.getName().getString()) :
                        WhisperManager.randomFragment(rep);
                    WhisperManager.sendWhisper(player, w);
                }
                if (st.timer % 60 == 0 && st.timer / 60 < 5) {
                    double ox = (player.getRandom().nextDouble() - 0.5) * 4;
                    double oz = (player.getRandom().nextDouble() - 0.5) * 4;
                    player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                        Holder.direct(ModSounds.HEARTBEAT_SOUND.get()), SoundSource.MASTER,
                        player.getX() + ox, player.getY() + 0.5, player.getZ() + oz,
                        0.6f, 1.0f, 0));
                }
                if (st.timer == 180) {
                    player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                        Holder.direct(ModSounds.TINNITUS_SOUND.get()), SoundSource.MASTER,
                        player.getX(), player.getY() + 1, player.getZ(),
                        0.5f, 1.0f, 0));
                }
                if (st.timer > 300) {
                    LOGGER.debug("[FogKnowsYourName] {} stage 1->2", player.getName().getString());
                    st.stage = 2;
                    st.timer = 0;
                }
            }
            case 2 -> {
                if (st.timer % 20 == 0) {
                    player.removeEffect(MobEffects.BLINDNESS);
                    WhisperManager.sendWhisper(player, "...fog lifts...");
                }
                if (st.timer > 100) {
                    LOGGER.debug("[FogKnowsYourName] {} event complete", player.getName().getString());
                    ACTIVE.remove(uuid);
                    HorrorEventPool.clearOngoing(player);
                }
            }
        }
    }

    @Override
    public void onCleanup(ServerPlayer player) {
        ACTIVE.remove(player.getUUID());
        player.removeEffect(MobEffects.BLINDNESS);
    }
}
