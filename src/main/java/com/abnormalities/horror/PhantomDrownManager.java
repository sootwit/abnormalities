package com.abnormalities.horror;

import com.abnormalities.config.AbnormalitiesConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;

public class PhantomDrownManager {
    private static final Map<UUID, Integer> ACTIVE = new HashMap<>();

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var srv = ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        var overworld = srv.getLevel(Level.OVERWORLD);
        if (overworld == null) return;
        if (!AbnormalitiesConfig.BR34TH_ENABLED.get()) return;
        long currentDay = overworld.getDayTime() / 24000L;
        if (currentDay < AbnormalitiesConfig.GRACE_PERIOD_DAYS.get()) return;

        Iterator<Map.Entry<UUID, Integer>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            ServerPlayer sp = overworld.getServer().getPlayerList().getPlayer(e.getKey());
            if (sp == null) {
                it.remove();
                continue;
            }
            int t = e.getValue() - 1;
            if (t <= 0) {
                sp.setAirSupply(sp.getMaxAirSupply());
                it.remove();
            } else {
                e.setValue(t);
            }
        }

        if (overworld.getGameTime() % 400 != 0) return;
        for (var sp : new java.util.ArrayList<>(overworld.getServer().getPlayerList().getPlayers())) {
            if (ACTIVE.containsKey(sp.getUUID())) continue;
            if (sp.isSleeping()) continue;
            if (sp.isInWater() || sp.isInLava()) continue;
            if (overworld.random.nextInt(AbnormalitiesConfig.BR34TH_CHANCE.get()) != 0) continue;
            ACTIVE.put(sp.getUUID(), 40);
            sp.setAirSupply(0);
            sp.hurt(sp.damageSources().drown(), 1.0F);
        }
    }

    public static void forceDrown(ServerPlayer player) {
        ACTIVE.put(player.getUUID(), 40);
        player.setAirSupply(0);
        player.hurt(player.damageSources().drown(), 1.0F);
    }
}
