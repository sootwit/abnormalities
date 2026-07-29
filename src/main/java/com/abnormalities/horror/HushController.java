package com.abnormalities.horror;

import com.abnormalities.config.AbnormalitiesConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;

public class HushController {
    private static final Map<UUID, HushState> ACTIVE = new HashMap<>();
    private static final Map<UUID, Integer> COOLDOWNS = new HashMap<>();
    private static final Map<UUID, List<Integer>> FROZEN_MOBS = new HashMap<>();

    private static class HushState {
        int ticksLeft;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var srv = ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        var overworld = srv.getLevel(Level.OVERWORLD);
        if (overworld == null) return;
        if (!AbnormalitiesConfig.HUSH_ENABLED.get()) return;
        long currentDay = overworld.getDayTime() / 24000L;
        if (currentDay < AbnormalitiesConfig.GRACE_PERIOD_DAYS.get()) return;

        COOLDOWNS.values().removeIf(cd -> cd <= 0);
        COOLDOWNS.replaceAll((u, cd) -> cd - 1);

        tickActive(overworld);

        if (overworld.getGameTime() % 80 != 0) return;
        if (overworld.random.nextInt(AbnormalitiesConfig.HUSH_SPAWN_WEIGHT.get()) != 0) return;

        for (ServerPlayer sp : overworld.players()) {
            UUID uuid = sp.getUUID();
            if (ACTIVE.containsKey(uuid)) continue;
            if (COOLDOWNS.containsKey(uuid)) continue;
            if (sp.isSleeping()) continue;
            startHush(sp, overworld);
            break;
        }
    }

    private static void startHush(ServerPlayer player, ServerLevel level) {
        UUID uuid = player.getUUID();
        var st = new HushState();
        int dur = AbnormalitiesConfig.HUSH_DURATION.get();
        st.ticksLeft = dur;
        ACTIVE.put(uuid, st);

        int range = AbnormalitiesConfig.HUSH_RANGE.get();
        var mobs = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(range));
        List<Integer> frozenIds = new ArrayList<>();
        for (Mob mob : mobs) {
            if (mob.isNoAi()) continue;
            mob.getPersistentData().putBoolean("abnormalities:hush_was_noai", mob.isNoAi());
            mob.setNoAi(true);
            frozenIds.add(mob.getId());
        }
        FROZEN_MOBS.put(uuid, frozenIds);
    }

    private static void tickActive(ServerLevel level) {
        Iterator<Map.Entry<UUID, HushState>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            UUID uuid = entry.getKey();
            HushState st = entry.getValue();
            Player player = level.getPlayerByUUID(uuid);
            if (player == null || !player.isAlive()) {
                unfreezeMobs(uuid, level);
                it.remove();
                COOLDOWNS.put(uuid, AbnormalitiesConfig.HUSH_COOLDOWN.get());
                continue;
            }
            st.ticksLeft--;
            if (st.ticksLeft <= 0) {
                unfreezeMobs(uuid, level);
                it.remove();
                COOLDOWNS.put(uuid, AbnormalitiesConfig.HUSH_COOLDOWN.get());
                continue;
            }
            int range = AbnormalitiesConfig.HUSH_RANGE.get();
            var mobs = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(range));
            for (Mob mob : mobs) {
                if (!mob.isNoAi()) {
                    mob.getPersistentData().putBoolean("abnormalities:hush_was_noai", false);
                    mob.setNoAi(true);
                    var ids = FROZEN_MOBS.get(uuid);
                    if (ids != null && !ids.contains(mob.getId())) ids.add(mob.getId());
                }
                Vec3 lookTarget = player.getEyePosition();
                mob.lookAt(player, 360.0F, 360.0F);
            }
        }
    }

    private static void unfreezeMobs(UUID uuid, ServerLevel level) {
        List<Integer> ids = FROZEN_MOBS.remove(uuid);
        if (ids == null) return;
        for (int id : ids) {
            var e = level.getEntity(id);
            if (e instanceof Mob mob) {
                boolean wasNoai = mob.getPersistentData().getBoolean("abnormalities:hush_was_noai");
                mob.setNoAi(wasNoai);
                mob.getPersistentData().remove("abnormalities:hush_was_noai");
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        ACTIVE.remove(uuid);
        COOLDOWNS.remove(uuid);
        var srv = ServerLifecycleHooks.getCurrentServer();
        if (srv != null) {
            for (var level : srv.getAllLevels()) {
                unfreezeMobs(uuid, (ServerLevel) level);
            }
        }
    }

    public static boolean isActive(UUID uuid) {
        return ACTIVE.containsKey(uuid);
    }
}
