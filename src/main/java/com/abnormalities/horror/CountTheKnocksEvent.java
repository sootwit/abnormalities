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

public class CountTheKnocksEvent extends AbstractHorrorEvent {
    private static final Map<UUID, Integer> TARGET = new HashMap<>();
    private static final Map<UUID, Integer> STATE = new HashMap<>();
    private static final Map<UUID, Integer> TICKS = new HashMap<>();
    private static final Map<UUID, Integer> KNOX = new HashMap<>();
    private static final Map<UUID, Vec3> START_POS = new HashMap<>();

    private static final int TICKS_PER_KNOCK = 80;
    private static final int ANSWER_WINDOW = 1200;
    private static final int MIN_KNOX = 4;
    private static final int MAX_KNOX = 10;

    private static final int S_KNOCKING = 0;
    private static final int S_ANSWERING = 1;
    private static final int S_DONE = 2;

    public CountTheKnocksEvent() {
        super("count_the_knocks", 180, 1.3);
    }

    @Override
    public boolean allowsOngoing() { return true; }

    @Override
    public void execute(ServerPlayer player) {
        UUID uuid = player.getUUID();
        int target = MIN_KNOX + player.getRandom().nextInt(MAX_KNOX - MIN_KNOX + 1);
        TARGET.put(uuid, target);
        STATE.put(uuid, S_KNOCKING);
        TICKS.put(uuid, 0);
        KNOX.put(uuid, 0);
        START_POS.put(uuid, player.position());
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 99999, 0, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 99999, 254, false, false, false));
        WhisperManager.sendWhisper(player, "count the knocks...");
    }

    @Override
    public void onPlayerTick(ServerPlayer player) {
        UUID uuid = player.getUUID();
        int state = STATE.getOrDefault(uuid, -1);
        if (state < 0) return;

        int tick = TICKS.merge(uuid, 1, Integer::sum);

        if (state == S_KNOCKING) {
            Vec3 startPos = START_POS.get(uuid);
            if (startPos != null && player.position().distanceTo(startPos) > 3.0D) {
                STATE.put(uuid, S_DONE);
                cleanup(player);
                return;
            }
            int knox = KNOX.getOrDefault(uuid, 0);
            int target = TARGET.getOrDefault(uuid, 0);
            int expectedKnox = tick / TICKS_PER_KNOCK;
            while (expectedKnox > knox && knox < target) {
                playKnock(player, knox, target);
                knox++;
                KNOX.put(uuid, knox);
            }
            if (knox >= target) {
                STATE.put(uuid, S_ANSWERING);
                TICKS.put(uuid, 0);
                WhisperManager.sendWhisper(player, "...how many?");
            }
        } else if (state == S_ANSWERING) {
            if (tick > ANSWER_WINDOW) {
                triggerWrongStatic(player, uuid);
                return;
            }
            if (tick > 0 && tick % 200 == 0) {
                int remaining = (ANSWER_WINDOW - tick) / 20;
                player.displayClientMessage(
                    Component.literal(remaining + "s").withStyle(ChatFormatting.RED), true);
            }
        }
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
        if (STATE.getOrDefault(uuid, -1) != S_ANSWERING) return;

        event.setCanceled(true);
        String msg = event.getMessage().getString().strip();
        int guess;
        try {
            guess = Integer.parseInt(msg);
        } catch (NumberFormatException e) {
            triggerWrongStatic(player, uuid);
            return;
        }

        int target = TARGET.getOrDefault(uuid, 0);
        if (guess == target) {
            STATE.put(uuid, S_DONE);
            player.removeEffect(MobEffects.BLINDNESS);
            player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            HorrorEventPool.clearOngoing(player);
        } else {
            triggerWrongStatic(player, uuid);
        }
    }

    private static void triggerWrongStatic(ServerPlayer player, UUID uuid) {
        if (STATE.put(uuid, S_DONE) == S_DONE) return;
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
        START_POS.remove(uuid);
    }

    @Override
    public void onCleanup(ServerPlayer player) {
        UUID uuid = player.getUUID();
        TARGET.remove(uuid);
        STATE.remove(uuid);
        TICKS.remove(uuid);
        KNOX.remove(uuid);
        START_POS.remove(uuid);
    }
}
