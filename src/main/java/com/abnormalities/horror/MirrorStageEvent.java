package com.abnormalities.horror;

import com.abnormalities.ActionLogger;
import com.abnormalities.WhisperManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;

public class MirrorStageEvent extends AbstractHorrorEvent {
    private static final Map<UUID, List<String>> CHAT_LOG = new HashMap<>();
    private static final Map<UUID, Integer> ACTIVE = new HashMap<>();

    public MirrorStageEvent() {
        super("mirror_stage", 120, 0.9);
    }

    @Override
    public boolean allowsOngoing() { return true; }

    @Override
    public void execute(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 0, false, false, false));
        ACTIVE.put(player.getUUID(), 0);
        List<String> msgs = CHAT_LOG.getOrDefault(player.getUUID(), List.of());
        if (!msgs.isEmpty()) {
            String echo = msgs.get(player.getRandom().nextInt(msgs.size()));
            WhisperManager.sendWhisper(player, WhisperManager.chatEcho(echo));
        }
        WhisperManager.sendWhisper(player, "who are you talking to...");
        scheduleEcho(player, 1);
    }

    private void scheduleEcho(ServerPlayer player, int index) {
        if (index >= 4) {
            WhisperManager.sendWhisper(player, "...mirror fades.");
            ACTIVE.remove(player.getUUID());
            HorrorEventPool.clearOngoing(player);
            return;
        }
        player.server.tell(new net.minecraft.server.TickTask(
            player.server.getTickCount() + 40,
            () -> {
                if (player.connection == null || !ACTIVE.containsKey(player.getUUID())) return;
                List<String> msgs = CHAT_LOG.getOrDefault(player.getUUID(), List.of());
                if (!msgs.isEmpty() && player.getRandom().nextBoolean()) {
                    String echo = msgs.get(player.getRandom().nextInt(msgs.size()));
                    WhisperManager.sendWhisper(player, WhisperManager.chatEcho(echo));
                } else {
                    WhisperManager.sendWhisper(player, WhisperManager.randomFragment(com.abnormalities.ReputationManager.getRep(player)));
                }
                scheduleEcho(player, index + 1);
            }
        ));
    }

    @Override
    public void onCleanup(ServerPlayer player) {
        ACTIVE.remove(player.getUUID());
        player.removeEffect(MobEffects.BLINDNESS);
    }

    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        String msg = event.getMessage().getString();
        CHAT_LOG.computeIfAbsent(player.getUUID(), k -> new ArrayList<>()).add(msg);
        List<String> log = CHAT_LOG.get(player.getUUID());
        if (log.size() > 20) log.remove(0);
        ActionLogger.log(player, "chat", msg);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        CHAT_LOG.remove(event.getEntity().getUUID());
    }
}
