package com.abnormalities.horror;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;
import java.util.function.BooleanSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abnormalities.config.AbnormalitiesConfig;

public class SisterCooldownManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|SisterCooldown");

    private static final Map<UUID, Deque<String>> DEDUP = new HashMap<>();
    private static final Map<UUID, Long> LAST_ACTIVITY = new HashMap<>();
    private static final Map<UUID, Long> LAST_CHAT = new HashMap<>();
    private static final Map<UUID, Boolean> CHATTING = new HashMap<>();
    private static final Map<UUID, PendingMessage> PENDING = new HashMap<>();

    private static final int DEDUP_SIZE = 5;
    private static final int CHATTING_WINDOW = 60;

    private static class PendingMessage {
        final ServerPlayer player;
        final String message;
        final long targetTick;
        final boolean isChat;
        final BooleanSupplier stillValid;
        boolean cancelled;

        PendingMessage(ServerPlayer player, String message, long targetTick, boolean isChat, BooleanSupplier stillValid) {
            this.player = player;
            this.message = message;
            this.targetTick = targetTick;
            this.isChat = isChat;
            this.stillValid = stillValid;
            this.cancelled = false;
        }
    }

    public static boolean canActivityComment(UUID uuid) {
        return !isPending(uuid) && !isChatting(uuid) && activityCooldownExpired(uuid);
    }

    public static boolean canChatReply(UUID uuid) {
        return !isPending(uuid) && !isChatting(uuid) && chatCooldownExpired(uuid);
    }

    public static void scheduleActivity(ServerPlayer player, String msg, int delay, BooleanSupplier stillValid) {
        UUID uuid = player.getUUID();
        if (isPending(uuid)) {
            PendingMessage existing = PENDING.get(uuid);
            if (existing.isChat) return;
            existing.cancelled = true;
        }
        var srv = ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        long now = srv.getTickCount();
        PENDING.put(uuid, new PendingMessage(player, msg, now + delay, false, stillValid));
    }

    public static void scheduleChat(ServerPlayer player, String msg, int delay) {
        UUID uuid = player.getUUID();
        if (isPending(uuid)) {
            PENDING.get(uuid).cancelled = true;
        }
        var srv = ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        long now = srv.getTickCount();
        PENDING.put(uuid, new PendingMessage(player, msg, now + delay, true, () -> true));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var srv = ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        long now = srv.getTickCount();

        Iterator<Map.Entry<UUID, PendingMessage>> it = PENDING.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            PendingMessage msg = entry.getValue();
            UUID uuid = entry.getKey();

            if (msg.cancelled) {
                it.remove();
                continue;
            }

            if (now >= msg.targetTick) {
                it.remove();
                if (msg.player.connection == null) continue;
                if (!msg.player.isAlive()) continue;

                boolean valid = msg.stillValid == null || msg.stillValid.getAsBoolean();
                if (!valid) {
                    LOGGER.debug("[SisterCooldown] dropped stale message for {}: {}", msg.player.getName().getString(), msg.message);
                    continue;
                }

                msg.player.connection.send(new ClientboundSystemChatPacket(
                    Component.literal("<Sister> " + msg.message).withStyle(ChatFormatting.WHITE), false));

                markSentInternal(uuid, msg.message);
                if (msg.isChat) {
                    LAST_CHAT.put(uuid, now);
                } else {
                    LAST_ACTIVITY.put(uuid, now);
                }

                CHATTING.put(uuid, true);
                long chatExpiry = now + CHATTING_WINDOW;
                final UUID fuuid = uuid;
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new Object() {
                    @SubscribeEvent
                    public void onTick(TickEvent.ServerTickEvent e) {
                        if (e.phase != TickEvent.Phase.END) return;
                        var s = ServerLifecycleHooks.getCurrentServer();
                        if (s == null) return;
                        if (s.getTickCount() >= chatExpiry) {
                            CHATTING.remove(fuuid);
                            net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(this);
                        }
                    }
                });
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        DEDUP.remove(uuid);
        LAST_ACTIVITY.remove(uuid);
        LAST_CHAT.remove(uuid);
        CHATTING.remove(uuid);
        PendingMessage pending = PENDING.remove(uuid);
        if (pending != null) pending.cancelled = true;
    }

    private static boolean isPending(UUID uuid) {
        return PENDING.containsKey(uuid);
    }

    private static boolean isChatting(UUID uuid) {
        return CHATTING.getOrDefault(uuid, false);
    }

    private static boolean activityCooldownExpired(UUID uuid) {
        Long last = LAST_ACTIVITY.get(uuid);
        if (last == null) return true;
        var srv = ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return true;
        return srv.getTickCount() - last >= AbnormalitiesConfig.SISTER_ACTIVITY_COOLDOWN.get();
    }

    private static boolean chatCooldownExpired(UUID uuid) {
        Long last = LAST_CHAT.get(uuid);
        if (last == null) return true;
        var srv = ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return true;
        return srv.getTickCount() - last >= AbnormalitiesConfig.SISTER_CHAT_COOLDOWN.get();
    }

    private static void markSentInternal(UUID uuid, String msg) {
        Deque<String> pool = DEDUP.computeIfAbsent(uuid, k -> new ArrayDeque<>());
        pool.addLast(msg);
        while (pool.size() > DEDUP_SIZE) {
            pool.removeFirst();
        }
    }

    public static boolean isDuplicate(UUID uuid, String msg) {
        Deque<String> pool = DEDUP.get(uuid);
        return pool != null && pool.contains(msg);
    }
}
