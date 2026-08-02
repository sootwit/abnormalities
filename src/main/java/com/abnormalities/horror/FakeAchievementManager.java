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

public class FakeAchievementManager {
    private static final Random RNG = new Random();
    private static long nextCheck = 0;

    private static final List<String> NAMES = List.of(
        "nur",
        "it sees you",
        "3 a.m.",
        "one more step",
        "do not blink",
        "the silence",
        "you are alone",
        "the door",
        "it remembers",
        "wrong floor",
        "stay still",
        "the watcher",
        "he's behind you",
        "too late",
        "don't look"
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
        AbnormalitiesMod.CHANNEL.send(PacketDistributor.PLAYER.with(() -> target), new FakeAchievementPacket(name));
    }

    public static void giveNamed(ServerPlayer target, String name) {
        if (target.connection == null) return;
        AbnormalitiesMod.CHANNEL.send(PacketDistributor.PLAYER.with(() -> target), new FakeAchievementPacket(name));
    }
}