package com.abnormalities.horror;

import com.abnormalities.config.AbnormalitiesConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SlowedMusicManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|SlowedMusic");
    private static final Random RNG = new Random();
    private static long nextFire = 0;
    private static final long MIN_COOLDOWN = 60000;
    private static final long COOLDOWN_VARIANCE = 60000;
    private static final float VOLUME = 3.0f;
    private static final float PITCH_MIN = 0.3f;
    private static final float PITCH_RANGE = 0.2f;

    private static final List<SoundEvent> DISCS = List.of(
        SoundEvents.MUSIC_DISC_13,
        SoundEvents.MUSIC_DISC_13,
        SoundEvents.MUSIC_DISC_13,
        SoundEvents.MUSIC_DISC_OTHERSIDE,
        SoundEvents.MUSIC_DISC_STAL,
        SoundEvents.MUSIC_DISC_MELLOHI,
        SoundEvents.MUSIC_DISC_CAT
    );

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var srv = ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        var overworld = srv.getLevel(Level.OVERWORLD);
        if (overworld == null) return;
        long currentDay = overworld.getDayTime() / 24000L;
        if (currentDay < AbnormalitiesConfig.GRACE_PERIOD_DAYS.get()) return;

        long now = srv.getTickCount();
        if (nextFire == 0) {
            nextFire = now + MIN_COOLDOWN + RNG.nextInt((int) (COOLDOWN_VARIANCE + 1));
            return;
        }
        if (now < nextFire) return;

        var players = overworld.players();
        if (players.isEmpty()) return;
        ServerPlayer target = players.get(RNG.nextInt(players.size()));
        if (target.isSleeping()) return;

        playSlowedDisc(overworld, target);
        nextFire = now + MIN_COOLDOWN + RNG.nextInt((int) (COOLDOWN_VARIANCE + 1));
    }

    private static void playSlowedDisc(ServerLevel level, ServerPlayer player) {
        SoundEvent disc = DISCS.get(RNG.nextInt(DISCS.size()));
        float pitch = PITCH_MIN + RNG.nextFloat() * PITCH_RANGE;
        level.playSound(null, player.getX(), player.getY(), player.getZ(), disc, SoundSource.MASTER, VOLUME, pitch);
        LOGGER.info("[SlowedMusic] {} disc={} pitch={}",
            player.getName().getString(), disc.getLocation().getPath(), String.format("%.2f", pitch));
    }

    public static void forcePlay() {
        var srv = ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        var overworld = srv.getLevel(Level.OVERWORLD);
        if (overworld == null) return;
        var players = overworld.players();
        if (players.isEmpty()) return;
        ServerPlayer target = players.get(RNG.nextInt(players.size()));
        playSlowedDisc(overworld, target);
        nextFire = srv.getTickCount() + MIN_COOLDOWN + RNG.nextInt((int) (COOLDOWN_VARIANCE + 1));
    }
}