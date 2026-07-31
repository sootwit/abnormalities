package com.abnormalities.horror;

import com.abnormalities.AbnormalitiesMod;
import com.abnormalities.config.AbnormalitiesConfig;
import com.abnormalities.network.St1llPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;

public class StillWorldManager {
    private static final Map<UUID, StillState> ACTIVE = new HashMap<>();

    private static class StillState {
        int ticksLeft;
        long frozenDayTime;
        UUID playerId;
        List<Integer> frozenMobs = new ArrayList<>();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var srv = ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        var overworld = srv.getLevel(Level.OVERWORLD);
        if (overworld == null) return;
        if (!AbnormalitiesConfig.ST1LL_ENABLED.get()) return;
        long currentDay = overworld.getDayTime() / 24000L;
        if (currentDay < AbnormalitiesConfig.GRACE_PERIOD_DAYS.get()) return;

        Iterator<Map.Entry<UUID, StillState>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            UUID uuid = e.getKey();
            StillState s = e.getValue();
            ServerPlayer player = overworld.getServer().getPlayerList().getPlayer(uuid);
            if (player == null || !player.isAlive()) {
                unfreeze(e.getValue(), overworld);
                it.remove();
                continue;
            }
            overworld.setDayTime(s.frozenDayTime);
            int radius = AbnormalitiesConfig.ST1LL_RADIUS.get();
            for (Mob mob : overworld.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(radius))) {
                if (mob.isNoAi()) continue;
                mob.setNoAi(true);
                mob.setDeltaMovement(0, 0, 0);
                if (!s.frozenMobs.contains(mob.getId())) s.frozenMobs.add(mob.getId());
            }
            s.ticksLeft--;
            if (s.ticksLeft <= 0) {
                unfreeze(s, overworld);
                it.remove();
            }
        }

        if (overworld.getGameTime() % 200 != 0) return;
        if (overworld.random.nextInt(AbnormalitiesConfig.ST1LL_CHANCE.get()) != 0) return;
        for (var sp : new java.util.ArrayList<>(overworld.getServer().getPlayerList().getPlayers())) {
            if (ACTIVE.containsKey(sp.getUUID())) continue;
            if (sp.isSleeping()) continue;
            if (overworld.random.nextInt(3) != 0) continue;
            startStill(sp, overworld);
            break;
        }
    }

    private static void startStill(ServerPlayer player, ServerLevel level) {
        StillState s = new StillState();
        s.ticksLeft = AbnormalitiesConfig.ST1LL_DURATION.get();
        s.frozenDayTime = level.getDayTime();
        s.playerId = player.getUUID();
        ACTIVE.put(player.getUUID(), s);
        AbnormalitiesMod.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new St1llPacket(true));
    }

    private static void unfreeze(StillState s, ServerLevel level) {
        for (int id : s.frozenMobs) {
            var e = level.getEntity(id);
            if (e instanceof Mob mob && mob.isNoAi()) {
                mob.setNoAi(false);
            }
        }
        s.frozenMobs.clear();
        if (s.playerId != null) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(s.playerId);
            if (player != null && player.connection != null) {
                AbnormalitiesMod.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new St1llPacket(false));
            }
        }
    }

    public static void forceStart(ServerPlayer player) {
        if (ACTIVE.containsKey(player.getUUID())) return;
        startStill(player, (ServerLevel) player.level());
    }
}
