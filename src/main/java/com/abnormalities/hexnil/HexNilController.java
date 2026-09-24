package com.abnormalities.hexnil;

import com.abnormalities.config.AbnormalitiesConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HexNilController {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|0x0000");
    private static long lastPillar = 0;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!AbnormalitiesConfig.HN_ENABLED.get()) return;
        if (com.abnormalities.horror.PeakDayManager.isWindDisabled()) return;
        var srv = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        ServerLevel overworld = srv.getLevel(Level.OVERWORLD);
        if (overworld == null) return;
        long now = overworld.getGameTime();

        for (ServerPlayer player : new java.util.ArrayList<>(srv.getPlayerList().getPlayers())) {
            if (player.tickCount % 200 != 0) continue;
            boolean inOverworld = player.level().dimension() == Level.OVERWORLD;
            boolean inSign = com.abnormalities.config.AbnormalitiesConfig.SIGN_ENABLED.get()
                    && player.level().dimension() == com.abnormalities.sign.SignDimension.LEVEL_KEY;
            if (!inOverworld && !inSign) continue;

            long graceTicks = (long) AbnormalitiesConfig.GRACE_PERIOD_DAYS.get() * 24000L;
            if (now < graceTicks) continue;

            double graceMult = AbnormalitiesConfig.HN_GRACE_MULT.get();

            if (AbnormalitiesConfig.HN_PILLARS_ENABLED.get()) {
                long cooldown = (long) (AbnormalitiesConfig.HN_PILLARS_COOLDOWN.get() * graceMult);
                int pillarChance = inSign ? 750 : 3000;
                if (now - lastPillar >= cooldown && overworld.random.nextInt(pillarChance) == 0) {
                    lastPillar = now;
                    HexNilPillarManager.spawnPillar(player);
                }
            }

            HexNilChunkManager.tick(player, now);
        }
    }

    public static void forcePillar(ServerPlayer player) {
        if (player.level().dimension() != Level.OVERWORLD
                && player.level().dimension() != com.abnormalities.sign.SignDimension.LEVEL_KEY) return;
        HexNilPillarManager.spawnPillar(player);
    }

    public static void forceRandom(ServerPlayer player) {
        if (player.level().dimension() != Level.OVERWORLD
                && player.level().dimension() != com.abnormalities.sign.SignDimension.LEVEL_KEY) return;
        forcePillar(player);
    }
}
