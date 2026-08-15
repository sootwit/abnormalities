package com.abnormalities.horror;

import com.abnormalities.config.AbnormalitiesConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnimalNoiseManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|AnimalNoise");
    private static final Random RNG = new Random();
    private static long nextFire = 0;
    private static final long MIN_COOLDOWN = 30000;
    private static final long COOLDOWN_VARIANCE = 60000;
    private static final float VOLUME = 2.0f;
    private static final float PITCH_MIN = 0.5f;
    private static final float PITCH_RANGE = 0.2f;
    private static final double MIN_DIST = 5.0D;
    private static final double MAX_DIST = 15.0D;

    private static final List<SoundEvent> ANIMAL_SOUNDS = List.of(
        SoundEvents.CAT_AMBIENT,
        SoundEvents.CAT_HISS,
        SoundEvents.WOLF_HOWL,
        SoundEvents.WOLF_AMBIENT,
        SoundEvents.COW_AMBIENT,
        SoundEvents.PIG_AMBIENT,
        SoundEvents.SHEEP_AMBIENT,
        SoundEvents.CHICKEN_AMBIENT,
        SoundEvents.FOX_AMBIENT,
        SoundEvents.HORSE_AMBIENT
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

        playAnimalNoise(overworld, target);
        nextFire = now + MIN_COOLDOWN + RNG.nextInt((int) (COOLDOWN_VARIANCE + 1));
    }

    private static void playAnimalNoise(ServerLevel level, ServerPlayer player) {
        SoundEvent snd = ANIMAL_SOUNDS.get(RNG.nextInt(ANIMAL_SOUNDS.size()));
        double angle = RNG.nextDouble() * Math.PI * 2.0D;
        double dist = MIN_DIST + RNG.nextDouble() * (MAX_DIST - MIN_DIST);
        double x = player.getX() + Math.cos(angle) * dist;
        double z = player.getZ() + Math.sin(angle) * dist;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) x, (int) z);
        float pitch = PITCH_MIN + RNG.nextFloat() * PITCH_RANGE;
        level.playSound(null, x, y, z, snd, SoundSource.MASTER, VOLUME, pitch);
        LOGGER.info("[AnimalNoise] {} sound={} at ({},{},{}) dist={} pitch={}",
            player.getName().getString(), snd.getLocation().getPath(), (int) x, y, (int) z,
            String.format("%.1f", dist), String.format("%.2f", pitch));
    }

    public static void forcePlay(ServerPlayer target) {
        MinecraftServer srv = ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        ServerLevel level = srv.getLevel(Level.OVERWORLD);
        if (level == null) return;
        ServerPlayer chosen = target;
        if (chosen == null || chosen.level() != level) {
            var players = level.players();
            if (players.isEmpty()) return;
            chosen = players.get(RNG.nextInt(players.size()));
        }
        playAnimalNoise(level, chosen);
        nextFire = srv.getTickCount() + MIN_COOLDOWN + RNG.nextInt((int) (COOLDOWN_VARIANCE + 1));
    }

    public static void forcePlay() {
        forcePlay(null);
    }
}