package com.abnormalities.horror;

import com.abnormalities.AbnormalitiesMod;
import com.abnormalities.config.AbnormalitiesConfig;
import com.abnormalities.network.FakeAchievementPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FakeAchievementManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|FakeAchievement");
    private static final Random RNG = new Random();
    private static long nextCheck = 0;

    private static final List<String> NAMES = List.of(
        "nur",
        "It sees you",
        "10",
        "One more step",
        "Do not blink",
        "The silence",
        "You are not alone",
        "The door",
        "It remembers",
        "Wrong floor",
        "Stay still",
        "The watcher",
        "he's behind you",
        "Too late",
        "Don't look",
        "No longer"
    );

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var srv = ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        if (!AbnormalitiesConfig.FAKE_ACH_ENABLED.get()) return;
        long now = srv.getTickCount();
        if (now < nextCheck) return;
        nextCheck = now + AbnormalitiesConfig.FAKE_ACH_COOLDOWN.get();
        var players = srv.getPlayerList().getPlayers();
        if (players.isEmpty()) return;
        ServerPlayer target = players.get(RNG.nextInt(players.size()));
        give(target);
    }

    public static void give(ServerPlayer target) {
        if (target.connection == null) return;
        String name = NAMES.get(RNG.nextInt(NAMES.size()));
        LOGGER.info("[FakeAchievement] {} -> {}", target.getName().getString(), name);
        AbnormalitiesMod.CHANNEL.send(PacketDistributor.PLAYER.with(() -> target), new FakeAchievementPacket(name));
        target.connection.send(new net.minecraft.network.protocol.game.ClientboundSystemChatPacket(
                net.minecraft.network.chat.Component.literal(
                    target.getName().getString() + " has made the advancement [" + name + "]"), false));
    }

    public static void giveNamed(ServerPlayer target, String name) {
        if (target.connection == null) return;
        AbnormalitiesMod.CHANNEL.send(PacketDistributor.PLAYER.with(() -> target), new FakeAchievementPacket(name));
    }
}