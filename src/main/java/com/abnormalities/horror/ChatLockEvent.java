package com.abnormalities.horror;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;

public class ChatLockEvent extends AbstractHorrorEvent {
    private static final Map<UUID, Integer> LOCK_TICKS = new HashMap<>();
    private static final Random RNG = new Random();
    private final boolean disabled;

    public ChatLockEvent(boolean disabled) {
        super(disabled ? "chatDisabled" : "chatEnabled", disabled ? 60 : 50, 1.0, 0, 900, 12000, false);
        this.disabled = disabled;
    }

    @Override
    public void execute(ServerPlayer player) {
        if (disabled) {
            setDisabled(player, 300 + RNG.nextInt(601));
        } else {
            setEnabled(player);
        }
    }

    public static void forceDisabled(ServerPlayer player) {
        setDisabled(player, -1);
    }

    public static void forceEnabled(ServerPlayer player) {
        setEnabled(player);
    }

    private static void setDisabled(ServerPlayer player, int ticks) {
        LOCK_TICKS.put(player.getUUID(), ticks);
        sendForgeError(player, 86);
    }

    private static void setEnabled(ServerPlayer player) {
        LOCK_TICKS.put(player.getUUID(), 0);
        sendForgeError(player, 87);
    }

    private static void sendForgeError(ServerPlayer player, int code) {
        String reason = code == 86 ? "Unauthorized access by a mod" : "Authorization declined for a mod";
        Component text = Component.literal("'Forge Error'").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(": " + code + " (" + reason + ")"));
        player.connection.send(new ClientboundSystemChatPacket(text, false));
    }

    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        if (LOCK_TICKS.getOrDefault(event.getPlayer().getUUID(), 0) == 0) return;
        event.setCanceled(true);
        event.getPlayer().displayClientMessage(Component.literal("Authentication failed"), true);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (LOCK_TICKS.isEmpty()) return;
        var it = LOCK_TICKS.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            int ticks = e.getValue();
            if (ticks < 0) continue;
            if (ticks <= 1) {
                it.remove();
                continue;
            }
            e.setValue(ticks - 1);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LOCK_TICKS.remove(event.getEntity().getUUID());
    }
}