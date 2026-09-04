package com.abnormalities.horror;

import com.abnormalities.ReputationManager;
import com.abnormalities.config.AbnormalitiesConfig;
import com.abnormalities.entity.NurEntity;
import com.abnormalities.network.K3wOverlayPacket;
import com.abnormalities.registry.ModEntities;
import com.abnormalities.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ApparitionManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|Apparition");
    private static final Random RNG = new Random();
    private static final Map<UUID, ApparitionState> ACTIVE = new HashMap<>();

    private static class ApparitionState {
        final Entity entity;
        final UUID ownerPlayer;
        final long spawnTick;
        boolean hasBeenSeen;

        ApparitionState(Entity entity, UUID owner, long tick) {
            this.entity = entity;
            this.ownerPlayer = owner;
            this.spawnTick = tick;
            this.hasBeenSeen = false;
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!AbnormalitiesConfig.APPARITION_ENABLED.get()) return;
        var srv = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        ServerLevel overworld = srv.getLevel(Level.OVERWORLD);
        if (overworld == null) return;
        long now = overworld.getGameTime();

        Iterator<Map.Entry<UUID, ApparitionState>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            ApparitionState state = entry.getValue();
            if (state.entity.isRemoved() || now - state.spawnTick > AbnormalitiesConfig.APPARITION_MAX_LIFETIME.get()) {
                if (!state.entity.isRemoved()) state.entity.discard();
                it.remove();
                continue;
            }

            boolean inFov = false;
            ServerPlayer owner = srv.getPlayerList().getPlayer(entry.getKey());
            if (owner != null && owner.level().dimension() == Level.OVERWORLD) {
                Vec3 toEntity = state.entity.position().subtract(owner.getEyePosition());
                double dist = toEntity.length();
                if (dist > 0.5 && dist < 64) {
                    Vec3 look = owner.getViewVector(1.0F);
                    double dot = look.dot(toEntity.normalize());
                    if (dot > 0.3) {
                        inFov = true;
                    }
                }
            }

            for (ServerPlayer p : srv.getPlayerList().getPlayers()) {
                if (p.getUUID().equals(entry.getKey())) continue;
                if (p.level().dimension() != Level.OVERWORLD) continue;
                Vec3 toEntity = state.entity.position().subtract(p.getEyePosition());
                double dist = toEntity.length();
                if (dist < 0.5 || dist > 64) continue;
                Vec3 look = p.getViewVector(1.0F);
                double dot = look.dot(toEntity.normalize());
                if (dot > 0.3) {
                    inFov = true;
                    break;
                }
            }

            if (inFov && !state.hasBeenSeen) {
                state.hasBeenSeen = true;
                LOGGER.info("[Apparition] {} saw apparition, warning triggered", owner != null ? owner.getName().getString() : "?");
                if (owner != null) {
                    owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                        ModSounds.WARNING.get(), SoundSource.HOSTILE, 2.0f, 0.6f);
                    com.abnormalities.AbnormalitiesMod.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> owner),
                        new K3wOverlayPacket());
                }
            }

            if (inFov) {
                state.entity.discard();
                it.remove();
            }
        }

        for (ServerPlayer player : new ArrayList<>(overworld.getServer().getPlayerList().getPlayers())) {
            if (player.level().dimension() != Level.OVERWORLD) continue;
            if (player.tickCount % 200 != 0) continue;
            if (ACTIVE.containsKey(player.getUUID())) continue;

            int rep = ReputationManager.getRep(player);
            int minRep = AbnormalitiesConfig.APPARITION_MIN_REP.get();
            int maxRep = AbnormalitiesConfig.APPARITION_MAX_REP.get();
            if (rep < minRep || rep > maxRep) continue;
            if (overworld.random.nextInt(AbnormalitiesConfig.APPARITION_WEIGHT.get()) != 0) continue;

            spawnApparition(overworld, player);
        }
    }

    private static void spawnApparition(ServerLevel level, ServerPlayer player) {
        float yaw = player.getYRot();
        double spawnAngle = Math.toRadians(yaw + 90 + (RNG.nextDouble() - 0.5) * 60);
        double dist = 8.0 + RNG.nextDouble() * (AbnormalitiesConfig.APPARITION_SPAWN_RANGE.get() - 8.0);
        double sx = player.getX() + Math.cos(spawnAngle) * dist;
        double sz = player.getZ() + Math.sin(spawnAngle) * dist;
        int sy = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) sx, (int) sz);
        LOGGER.info("[Apparition] {} apparition spawned at ({}, {}, {})", player.getName().getString(), (int)sx, sy, (int)sz);

        Entity entity = pickEntity(level);
        if (entity == null) return;
        if (entity instanceof Mob mob) {
            mob.setPersistenceRequired();
            mob.setNoAi(true);
        }
        entity.moveTo(sx + 0.5, sy + 1, sz + 0.5, level.random.nextFloat() * 360.0F, 0);
        entity.noCulling = true;
        if (entity instanceof NurEntity nur) {
            nur.currentState = NurEntity.State.DUMMY;
            nur.currentTarget = player;
        }
        level.addFreshEntity(entity);

        Vec3 toEntity = entity.position().subtract(player.getEyePosition());
        double hDist = Math.sqrt(toEntity.x * toEntity.x + toEntity.z * toEntity.z);
        float lookYaw = (float) Math.toDegrees(Math.atan2(-toEntity.x, toEntity.z));
        float lookPitch = (float) Math.toDegrees(-Math.atan2(toEntity.y, hDist));
        player.setYRot(lookYaw);
        player.setXRot(lookPitch);
        player.yHeadRot = lookYaw;
        player.yBodyRot = lookYaw;
        player.setDeltaMovement(0, player.getDeltaMovement().y, 0);

        ACTIVE.put(player.getUUID(), new ApparitionState(entity, player.getUUID(), level.getGameTime()));
    }

    private static Entity pickEntity(ServerLevel level) {
        return ModEntities.NUR.get().create(level);
    }

    public static void forceSpawn(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        if (level.dimension() != Level.OVERWORLD) return;
        spawnApparition(level, player);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() == null) return;
        ACTIVE.remove(event.getEntity().getUUID());
    }
}
