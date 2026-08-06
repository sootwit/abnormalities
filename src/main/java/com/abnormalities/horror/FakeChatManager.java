package com.abnormalities.horror;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.List;
import java.util.Random;

public class FakeChatManager {
    private static final Random RNG = new Random();
    private static final long FAKE_CHAT_COOLDOWN = 18000;
    private static final long FAKE_JOIN_COOLDOWN = 24000;
    private static long nextChat = 0;
    private static long nextJoin = 0;

    private static final List<String> MESSAGES = List.of(
        "I see you.",
        "Can you see me?",
        "It was your fault.",
        "Help us.",
        "I am right behind you.",
        "null",
        "null.err",
        "000",
        "you should not have opened this world.",
        "it has been watching since the first day.",
        "do not tell anyone.",
        "we know where you sleep."
    );

    private static final List<String> FAKE_NAMES = List.of(
        "null",
        "void",
        "spectator",
        "theOtherOne",
        "notYou",
        "empty",
        "none"
    );

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var srv = ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        long now = srv.getTickCount();
        if (nextChat == 0) {
            nextChat = now + FAKE_CHAT_COOLDOWN;
            nextJoin = now + FAKE_JOIN_COOLDOWN;
            return;
        }
        if (now >= nextChat) {
            nextChat = now + FAKE_CHAT_COOLDOWN;
            sendFakeChat(srv);
        }
        if (now >= nextJoin) {
            nextJoin = now + FAKE_JOIN_COOLDOWN;
            sendFakeJoinLeave(srv);
        }
    }

    private static void sendFakeChat(MinecraftServer srv) {
        var players = srv.getPlayerList().getPlayers();
        if (players.isEmpty()) return;
        ServerPlayer target = players.get(RNG.nextInt(players.size()));
        String msg = MESSAGES.get(RNG.nextInt(MESSAGES.size()));
        sendSystem(target, msg);
    }

    private static void sendFakeJoinLeave(MinecraftServer srv) {
        var players = srv.getPlayerList().getPlayers();
        if (players.isEmpty()) return;
        String name = FAKE_NAMES.get(RNG.nextInt(FAKE_NAMES.size()));
        String msg = RNG.nextBoolean() ? name + " joined the game" : name + " left the game";
        for (ServerPlayer p : players) {
            sendSystem(p, msg);
        }
    }

    private static void sendSystem(ServerPlayer player, String text) {
        if (player.connection == null) return;
        player.connection.send(new ClientboundSystemChatPacket(Component.literal(text), false));
    }
}
