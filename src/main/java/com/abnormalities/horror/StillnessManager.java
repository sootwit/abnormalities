package com.abnormalities.horror;

import com.abnormalities.config.AbnormalitiesConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;

public class StillnessManager {
    private static final Map<UUID, Integer> STILL_TICKS = new HashMap<>();
    private static final Map<UUID, HoldState> ACTIVE = new HashMap<>();
    private static final Map<UUID, List<Integer>> FROZEN = new HashMap<>();

    private static class HoldState {
        int ticksLeft;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var srv = ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        var overworld = srv.getLevel(Level.OVERWORLD);
        if (overworld == null) return;
        if (!AbnormalitiesConfig.H01D_ENABLED.get()) return;
        long currentDay = overworld.getDayTime() / 24000L;
        if (currentDay < AbnormalitiesConfig.GRACE_PERIOD_DAYS.get()) return;

        tickHolds(overworld);

        if (overworld.getGameTime() % 20 != 0) return;
        for (var sp : new java.util.ArrayList<>(overworld.getServer().getPlayerList().getPlayers())) {
            if (sp.isSleeping()) {
                STILL_TICKS.remove(sp.getUUID());
                continue;
            }
            double moved = sp.getDeltaMovement().horizontalDistanceSqr();
            boolean isMoving = moved > 0.0001;
            if (isMoving) {
                STILL_TICKS.remove(sp.getUUID());
                continue;
            }
            int ticks = STILL_TICKS.getOrDefault(sp.getUUID(), 0) + 1;
            STILL_TICKS.put(sp.getUUID(), ticks);
            if (ticks >= AbnormalitiesConfig.H01D_THRESHOLD.get()) {
                STILL_TICKS.put(sp.getUUID(), 0);
                triggerStillness(sp, overworld);
            }
        }
    }

    private static void triggerStillness(ServerPlayer sp, ServerLevel level) {
        SisterController.onHoldWarning(sp);
        ACTIVE.put(sp.getUUID(), new HoldState());
        ACTIVE.get(sp.getUUID()).ticksLeft = 80;
        List<Integer> frozenIds = new ArrayList<>();
        int range = 40;
        for (Mob mob : level.getEntitiesOfClass(Mob.class, sp.getBoundingBox().inflate(range))) {
            if (mob.isNoAi()) continue;
            if (mob.getPersistentData().getBoolean("abnormalities:h01d_ignore")) continue;
            mob.getPersistentData().putBoolean("abnormalities:h01d_was_noai", mob.isNoAi());
            mob.setNoAi(true);
            frozenIds.add(mob.getId());
            forceFace(mob, sp);
        }
        FROZEN.put(sp.getUUID(), frozenIds);
        int roll = level.random.nextInt(4);
        if (roll == 0) {
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                net.minecraft.core.Holder.direct(com.abnormalities.registry.ModSounds.WHISPER_SOUND.get()),
                net.minecraft.sounds.SoundSource.MASTER, sp.getX(), sp.getY(), sp.getZ(), 3.0f, 0.5f, 0));
        } else if (roll == 1) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double dist = 8.0D + level.random.nextDouble() * 6.0D;
            double sx = sp.getX() + Math.cos(angle) * dist;
            double sz = sp.getZ() + Math.sin(angle) * dist;
            int sy = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) sx, (int) sz);
            var nur = com.abnormalities.registry.ModEntities.NUR.get().create(level);
            if (nur != null) {
                nur.moveTo(sx + 0.5, sy + 1, sz + 0.5, 0, 0);
                nur.currentState = com.abnormalities.entity.NurEntity.State.STALKING;
                nur.currentTarget = sp;
                nur.getPersistentData().putBoolean("abnormalities:h01d_ignore", true);
                level.addFreshEntity(nur);
            }
        } else {
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSystemChatPacket(
                net.minecraft.network.chat.Component.literal("...").withStyle(net.minecraft.ChatFormatting.GRAY, net.minecraft.ChatFormatting.ITALIC), false));
        }
    }

    private static void forceFace(Mob mob, ServerPlayer player) {
        Vec3 look = player.getEyePosition();
        double dx = look.x - mob.getX();
        double dy = look.y - mob.getEyeY();
        double dz = look.z - mob.getZ();
        double h = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float)(Math.atan2(dz, dx) * 180.0F / Math.PI) - 90.0F;
        float pitch = (float)(-Math.atan2(dy, h) * 180.0F / Math.PI);
        mob.setYRot(yaw);
        mob.setXRot(pitch);
        mob.setYHeadRot(yaw);
        mob.setYBodyRot(yaw);
        mob.yHeadRot = yaw;
        mob.yBodyRot = yaw;
    }

    private static void tickHolds(ServerLevel level) {
        Iterator<Map.Entry<UUID, HoldState>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            UUID uuid = entry.getKey();
            ServerPlayer sp = level.getServer().getPlayerList().getPlayer(uuid);
            if (sp == null || !sp.isAlive()) {
                release(uuid, level);
                it.remove();
                continue;
            }
            HoldState st = entry.getValue();
            st.ticksLeft--;
            if (st.ticksLeft <= 0) {
                release(uuid, level);
                it.remove();
                continue;
            }
            int range = 40;
            for (Mob mob : level.getEntitiesOfClass(Mob.class, sp.getBoundingBox().inflate(range))) {
                if (mob.getPersistentData().getBoolean("abnormalities:h01d_ignore")) continue;
                if (!mob.isNoAi()) {
                    mob.getPersistentData().putBoolean("abnormalities:h01d_was_noai", false);
                    mob.setNoAi(true);
                    var ids = FROZEN.get(uuid);
                    if (ids != null && !ids.contains(mob.getId())) ids.add(mob.getId());
                }
                forceFace(mob, sp);
            }
        }
    }

    private static void release(UUID uuid, ServerLevel level) {
        List<Integer> ids = FROZEN.remove(uuid);
        if (ids == null) return;
        for (int id : ids) {
            var e = level.getEntity(id);
            if (e instanceof Mob mob) {
                boolean wasNoai = mob.getPersistentData().getBoolean("abnormalities:h01d_was_noai");
                mob.setNoAi(wasNoai);
                mob.getPersistentData().remove("abnormalities:h01d_was_noai");
            }
        }
    }

    public static void forceTrigger(ServerPlayer player) {
        triggerStillness(player, (ServerLevel) player.level());
    }

    @SubscribeEvent
    public static void onPlayerLogout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        ACTIVE.remove(uuid);
        STILL_TICKS.remove(uuid);
        var srv = ServerLifecycleHooks.getCurrentServer();
        if (srv != null) {
            for (var level : srv.getAllLevels()) {
                release(uuid, (ServerLevel) level);
            }
        }
    }
}
