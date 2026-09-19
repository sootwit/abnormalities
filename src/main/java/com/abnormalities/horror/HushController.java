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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HushController {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|Hush");
    private static final Map<UUID, HushState> ACTIVE = new HashMap<>();
    private static final Map<UUID, Integer> COOLDOWNS = new HashMap<>();
    private static final Map<UUID, List<UUID>> FROZEN_MOBS = new HashMap<>();

    private static class HushState {
        int ticksLeft;
        final net.minecraft.resources.ResourceKey<Level> dimension;
        HushState(net.minecraft.resources.ResourceKey<Level> dim) { this.dimension = dim; }
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
        if (overworld.random.nextInt(com.abnormalities.entity.HimTracker.weighted(AbnormalitiesConfig.HUSH_SPAWN_WEIGHT.get())) != 0) return;

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
        var st = new HushState(player.level().dimension());
        int dur = AbnormalitiesConfig.HUSH_DURATION.get();
        st.ticksLeft = dur;
        ACTIVE.put(uuid, st);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.AMBIENT_CAVE.get(), net.minecraft.sounds.SoundSource.MASTER, 6.0f, 0.4f);
        level.playSound(null, player.getX(), player.getY() + 16, player.getZ(),
                net.minecraft.sounds.SoundEvents.AMBIENT_CAVE.get(), net.minecraft.sounds.SoundSource.MASTER, 4.0f, 0.2f);

        int range = AbnormalitiesConfig.HUSH_RANGE.get();
        var mobs = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(range));
        List<UUID> frozenIds = new ArrayList<>();
        for (Mob mob : mobs) {
            if (mob.isNoAi()) continue;
            mob.getPersistentData().putBoolean("abnormalities:hush_was_noai", mob.isNoAi());
            mob.setNoAi(true);
            frozenIds.add(mob.getUUID());
        }
        FROZEN_MOBS.put(uuid, frozenIds);
        LOGGER.info("[Hush] {} triggered, froze {} mobs, duration={}t", player.getName().getString(), frozenIds.size(), dur);
    }

    private static void tickActive(ServerLevel level) {
        Iterator<Map.Entry<UUID, HushState>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            UUID uuid = entry.getKey();
            HushState st = entry.getValue();
            Player player = level.getPlayerByUUID(uuid);
            if (player == null || !player.isAlive()) {
                unfreezeMobs(uuid);
                it.remove();
                COOLDOWNS.put(uuid, AbnormalitiesConfig.HUSH_COOLDOWN.get());
                continue;
            }
            if (player.level().dimension() != st.dimension) {
                LOGGER.info("[Hush] {} changed dimension mid-hush, releasing", player.getName().getString());
                unfreezeMobs(uuid);
                it.remove();
                COOLDOWNS.put(uuid, AbnormalitiesConfig.HUSH_COOLDOWN.get());
                continue;
            }
            st.ticksLeft--;
            if (st.ticksLeft <= 0) {
                unfreezeMobs(uuid);
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
                    if (ids != null && !ids.contains(mob.getUUID())) ids.add(mob.getUUID());
                }
                Vec3 lookTarget = player.getEyePosition();
                double dx = lookTarget.x - mob.getX();
                double dz = lookTarget.z - mob.getZ();
                double dy = lookTarget.y - mob.getEyeY();
                double dist = Math.sqrt(dx * dx + dz * dz);
                float yaw = (float)(Math.atan2(dz, dx) * 180.0F / Math.PI) - 90.0F;
                float pitch = (float)(-Math.atan2(dy, dist) * 180.0F / Math.PI);
                mob.setYRot(yaw);
                mob.setXRot(pitch);
                mob.yHeadRot = yaw;
                mob.yBodyRot = yaw;
            }
        }
    }

    private static void unfreezeMobs(UUID uuid) {
        List<UUID> ids = FROZEN_MOBS.remove(uuid);
        if (ids == null) return;
        var srv = ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        for (var lvl : srv.getAllLevels()) {
            for (UUID id : ids) {
                var e = lvl.getEntity(id);
                if (e instanceof Mob mob) {
                    boolean wasNoai = mob.getPersistentData().getBoolean("abnormalities:hush_was_noai");
                    mob.setNoAi(wasNoai);
                    mob.getPersistentData().remove("abnormalities:hush_was_noai");
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        ACTIVE.remove(uuid);
        COOLDOWNS.remove(uuid);
        unfreezeMobs(uuid);
    }

    public static boolean isActive(UUID uuid) {
        return ACTIVE.containsKey(uuid);
    }

    public static void forceStart(ServerPlayer player) {
        if (ACTIVE.containsKey(player.getUUID())) return;
        var level = (ServerLevel) player.level();
        startHush(player, level);
    }
}
