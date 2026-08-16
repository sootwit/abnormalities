package com.abnormalities.horror;

import com.abnormalities.WhisperManager;
import com.abnormalities.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CountTheKnocksEvent extends AbstractHorrorEvent {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|CountTheKnocks");
    private static final Map<UUID, KnockState> ACTIVE = new HashMap<>();

    private static class KnockState {
        int target;
        int state;
        int ticks;
        int knox;
        Vec3 startPos;
        static final int S_KNOCKING = 0;
        static final int S_ANSWERING = 1;
        static final int S_DONE = 2;
    }

    private static final int TICKS_PER_KNOCK = 80;
    private static final int ANSWER_WINDOW = 1200;
    private static final int MIN_KNOX = 4;
    private static final int MAX_KNOX = 10;

    public CountTheKnocksEvent() {
        super("count_the_knocks", 180, 1.3);
    }

    @Override
    public boolean allowsOngoing() { return true; }

    @Override
    public void execute(ServerPlayer player) {
        UUID uuid = player.getUUID();
        KnockState st = new KnockState();
        st.target = MIN_KNOX + player.getRandom().nextInt(MAX_KNOX - MIN_KNOX + 1);
        st.state = KnockState.S_KNOCKING;
        st.ticks = 0;
        st.knox = 0;
        st.startPos = player.position();
        ACTIVE.put(uuid, st);
        LOGGER.info("[CountTheKnocks] {} triggered, target={} knocks", player.getName().getString(), st.target);
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 99999, 0, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 99999, 254, false, false, false));
        WhisperManager.sendWhisper(player, "count the knocks...");
    }

    @Override
    public void onPlayerTick(ServerPlayer player) {
        UUID uuid = player.getUUID();
        KnockState st = ACTIVE.get(uuid);
        if (st == null) return;

        st.ticks++;

        if (st.state == KnockState.S_KNOCKING) {
            int expectedKnox = st.ticks / TICKS_PER_KNOCK;
            while (expectedKnox > st.knox && st.knox < st.target) {
                playKnock(player, st.knox, st.target);
                st.knox++;
            }
            if (st.knox >= st.target) {
                st.state = KnockState.S_ANSWERING;
                st.ticks = 0;
                askHowMany(player);
            }
        } else if (st.state == KnockState.S_ANSWERING) {
            if (st.ticks > ANSWER_WINDOW) {
                triggerWrongStatic(player, uuid);
                return;
            }
            if (st.ticks > 0 && st.ticks % 200 == 0) {
                int remaining = (ANSWER_WINDOW - st.ticks) / 20;
                player.displayClientMessage(
                    Component.literal(remaining + "s").withStyle(ChatFormatting.RED), true);
                askHowMany(player);
            }
        }
    }

    private static void askHowMany(ServerPlayer player) {
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSystemChatPacket(
            Component.literal("...how many?").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), false));
        player.displayClientMessage(
            Component.literal("HOW MANY...?").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
    }

    private static void playKnock(ServerPlayer player, int index, int total) {
        float progress = (float) index / Math.max(total - 1, 1);
        float vol = 0.4f + progress * 1.8f;
        float pitch = 0.7f + progress * 0.5f;
        float spread = Math.max(6.0f - index * 0.5f, 1.0f);
        var rng = player.getRandom();
        var kpos = player.position().add(
            rng.nextGaussian() * spread,
            rng.nextGaussian() * 2,
            rng.nextGaussian() * spread
        );
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
            Holder.direct(SoundEvents.STONE_BREAK), SoundSource.MASTER,
            kpos.x, kpos.y, kpos.z, vol, pitch * 0.8f, 0));
    }

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        UUID uuid = player.getUUID();
        KnockState st = ACTIVE.get(uuid);
        if (st == null || st.state != KnockState.S_ANSWERING) return;

        event.setCanceled(true);
        String msg = event.getMessage().getString().strip();
        int guess;
        try {
            guess = Integer.parseInt(msg);
        } catch (NumberFormatException e) {
            triggerWrongStatic(player, uuid);
            return;
        }

        if (guess == st.target) {
            LOGGER.info("[CountTheKnocks] {} answered correctly ({})", player.getName().getString(), guess);
            st.state = KnockState.S_DONE;
            player.removeEffect(MobEffects.BLINDNESS);
            player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            WhisperManager.sendWhisper(player, "...yes. it stops.");
            HorrorEventPool.clearOngoing(player);
        } else {
            triggerWrongStatic(player, uuid);
        }
    }

    private static void triggerWrongStatic(ServerPlayer player, UUID uuid) {
        KnockState st = ACTIVE.get(uuid);
        if (st != null && st.state == KnockState.S_DONE) return;
        ACTIVE.remove(uuid);
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
            SoundEvents.AMBIENT_CAVE, SoundSource.MASTER,
            player.getX(), player.getY() + 1, player.getZ(),
            2.0f, 0.3f, 0));
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
            Holder.direct(ModSounds.NUR_SOUND.get()), SoundSource.MASTER,
            player.getX(), player.getY() + 1, player.getZ(),
            1.0f, 1.0f, 0));
        player.getServer().tell(new net.minecraft.server.TickTask(
            player.getServer().getTickCount() + 10,
            () -> {
                cleanup(player);
                player.connection.disconnect(Component.literal("WRONG"));
            }
        ));
    }

    private static void cleanup(ServerPlayer player) {
        UUID uuid = player.getUUID();
        player.removeEffect(MobEffects.BLINDNESS);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        HorrorEventPool.clearOngoing(player);
        ACTIVE.remove(uuid);
    }

    @Override
    public void onCleanup(ServerPlayer player) {
        UUID uuid = player.getUUID();
        ACTIVE.remove(uuid);
        player.removeEffect(net.minecraft.world.effect.MobEffects.BLINDNESS);
        player.removeEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
    }
}
