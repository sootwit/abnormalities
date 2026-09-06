package com.abnormalities.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class DistantEntity extends Mob {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|Distant");
    private static final EntityDataAccessor<Boolean> DATA_FLASHBACK = SynchedEntityData.defineId(DistantEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_AIRBORNE = SynchedEntityData.defineId(DistantEntity.class, EntityDataSerializers.BOOLEAN);
    private static final int STARE_TICKS = 15;
    private static final int GRACE_MAX = 10;

    private UUID targetPlayer = null;
    private int lookTicks = 0;
    private int graceTicks = 0;
    private int flashbackStage = 0;
    private int flashbackTicks = 0;
    private boolean flashbackTriggered = false;
    private int despawnRange = 16;
    private int waterTicks = 0;

    public DistantEntity(EntityType<? extends DistantEntity> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.setPersistenceRequired();
        this.noCulling = true;
        this.despawnRange = 14 + this.random.nextInt(5);
        this.setNoGravity(false);
    }

    public void setAirborne(boolean airborne) {
        this.entityData.set(DATA_AIRBORNE, airborne);
        this.setNoGravity(airborne);
    }

    public boolean isAirborne() {
        return this.entityData.get(DATA_AIRBORNE);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 9999.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_FLASHBACK, false);
        this.entityData.define(DATA_AIRBORNE, false);
    }

    @Override
    protected void registerGoals() {}

    public void setTargetPlayer(Player player) {
        this.targetPlayer = player.getUUID();
        LOGGER.info("[Distant] {} spawned for {} at ({}, {}, {}) range={}", this.isAirborne() ? "airborne" : "ground", player.getName().getString(), (int)this.getX(), (int)this.getY(), (int)this.getZ(), this.despawnRange);
    }

    public boolean isFlashback() {
        return this.entityData.get(DATA_FLASHBACK);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;
        this.setAirSupply(this.getMaxAirSupply());
        if (this.isInWater()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.05D, 0.0D));
            this.waterTicks++;
            if (this.waterTicks > 200) {
                LOGGER.info("[Distant] discarded, stuck in water");
                this.discard();
                return;
            }
        } else {
            this.waterTicks = 0;
        }
        if (!com.abnormalities.config.AbnormalitiesConfig.DISTANT_ENABLED.get()) {
            LOGGER.info("[Distant] discarded, config disabled");
            this.discard();
            return;
        }

        if (this.isAirborne() && !this.isInWater()) {
            double bob = Math.sin(this.tickCount * 0.1D) * 0.05D;
            this.setDeltaMovement(0.0D, bob, 0.0D);
        }

        ServerPlayer player = this.targetPlayer != null
                ? this.level().getServer() != null ? this.level().getServer().getPlayerList().getPlayer(this.targetPlayer) : null
                : null;
        if (player == null) {
            Player nearest = this.level().getNearestPlayer(this, 64.0D);
            if (nearest instanceof ServerPlayer sp) {
                player = sp;
                this.targetPlayer = sp.getUUID();
            }
        }
        if (player == null) {
            LOGGER.info("[Distant] discarded, no player found");
            this.discard();
            return;
        }

        this.facePlayer(player);

        if (this.flashbackTriggered && !com.abnormalities.horror.DistantManager.isFlashbackActive(player)) {
            this.flashbackTriggered = false;
            this.entityData.set(DATA_FLASHBACK, false);
            LOGGER.info("[Distant] flashback flag reset, manager inactive");
        }

        if (player.level().dimension() != this.level().dimension()) {
            LOGGER.info("[Distant] discarded, player left dimension");
            this.discard();
            return;
        }

        double dist = this.distanceTo(player);
        if (dist > 128.0D) {
            LOGGER.info("[Distant] discarded, player too far ({} blocks)", (int)dist);
            this.discard();
            return;
        }

        if (!this.flashbackTriggered && !this.isAirborne() && dist <= this.despawnRange) {
            LOGGER.info("[Distant] discarded, player within {} blocks", this.despawnRange);
            com.abnormalities.horror.DistantManager.onProximityDespawn(player);
            this.discard();
            return;
        }

        boolean looking = dist <= 128.0D && isPlayerLookingAt(player);

        if (looking) {
            this.lookTicks++;
            this.graceTicks = 0;
            if (this.lookTicks == 40) {
                LOGGER.info("[Distant] {} staring, continuing to track", player.getName().getString());
            }
            if (this.lookTicks >= STARE_TICKS && !this.flashbackTriggered) {
                LOGGER.info("[Distant] {} held stare, triggering flashback", player.getName().getString());
                this.startFlashback(player);
                return;
            }
        } else {
            if (this.graceTicks < GRACE_MAX) {
                this.graceTicks++;
            } else {
                if (this.lookTicks > 0) {
                    LOGGER.debug("[Distant] look reset after {} ticks (grace exhausted)", this.lookTicks);
                }
                this.lookTicks = 0;
            }
        }
    }

    private void facePlayer(Player player) {
        double dx = player.getX() - this.getX();
        double dz = player.getZ() - this.getZ();
        double dy = player.getEyeY() - this.getEyeY();
        double horizDist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0F);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, horizDist)));
        this.setYRot(yaw);
        this.setXRot(pitch);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
    }

    public boolean isPlayerLookingAt(Player player) {
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getViewVector(1.0F);
        AABB box = this.getBoundingBox().inflate(0.3D);
        Vec3 entityCenter = box.getCenter();
        if (eyePos.distanceTo(entityCenter) > 128.0D) return false;
        double entityDist = rayToAABB(eyePos, lookVec, box);
        if (entityDist >= 0 && entityDist < 128.0D) {
            return isLineOfSightClear(player, eyePos, eyePos.add(lookVec.scale(entityDist)));
        }
        if (entityDist >= 128.0D) return false;
        double threshold = 1.5D;
        if (!isLineOfSightClear(player, eyePos, entityCenter)) return false;
        Vec3[] corners = {
                new Vec3(box.minX, box.minY, box.minZ), new Vec3(box.maxX, box.minY, box.minZ),
                new Vec3(box.minX, box.maxY, box.minZ), new Vec3(box.maxX, box.maxY, box.minZ),
                new Vec3(box.minX, box.minY, box.maxZ), new Vec3(box.maxX, box.minY, box.maxZ),
                new Vec3(box.minX, box.maxY, box.maxZ), new Vec3(box.maxX, box.maxY, box.maxZ)
        };
        for (Vec3 corner : corners) {
            Vec3 toCorner = corner.subtract(eyePos);
            double t = toCorner.dot(lookVec) / lookVec.dot(lookVec);
            if (t < 0) continue;
            Vec3 projection = eyePos.add(lookVec.scale(t));
            if (projection.distanceTo(corner) < threshold && isLineOfSightClear(player, eyePos, corner)) return true;
        }
        return false;
    }

    private static double rayToAABB(Vec3 origin, Vec3 dir, AABB box) {
        double tmin = Double.NEGATIVE_INFINITY;
        double tmax = Double.POSITIVE_INFINITY;
        double[] originArr = {origin.x, origin.y, origin.z};
        double[] dirArr = {dir.x, dir.y, dir.z};
        double[] boxMin = {box.minX, box.minY, box.minZ};
        double[] boxMax = {box.maxX, box.maxY, box.maxZ};
        for (int i = 0; i < 3; i++) {
            if (Math.abs(dirArr[i]) < 1.0E-10D) {
                if (originArr[i] < boxMin[i] || originArr[i] > boxMax[i]) return -1;
            } else {
                double inv = 1.0D / dirArr[i];
                double t1 = (boxMin[i] - originArr[i]) * inv;
                double t2 = (boxMax[i] - originArr[i]) * inv;
                if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
                tmin = Math.max(tmin, t1);
                tmax = Math.min(tmax, t2);
                if (tmin > tmax) return -1;
            }
        }
        if (tmax < 0) return -1;
        return tmin >= 0 ? tmin : tmax;
    }

    private static boolean isLineOfSightClear(Player player, Vec3 from, Vec3 to) {
        net.minecraft.world.level.ClipContext ctx = new net.minecraft.world.level.ClipContext(
                from, to, net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, player);
        return player.level().clip(ctx).getType() == net.minecraft.world.phys.HitResult.Type.MISS;
    }

    private void startFlashback(Player player) {
        if (this.flashbackTriggered) return;
        if (player instanceof ServerPlayer sp) {
            if (!com.abnormalities.horror.DistantManager.startFlashbackFor(sp, this)) {
                LOGGER.debug("[Distant] flashback rejected by manager (active, cooldown, or nur disabled)");
                this.lookTicks = 0;
                return;
            }
            this.flashbackTriggered = true;
            this.entityData.set(DATA_FLASHBACK, true);
            this.flashbackStage = 1;
            this.flashbackTicks = 0;
            com.abnormalities.AbnormalitiesMod.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp),
                new com.abnormalities.network.DistantFlashbackPacket(1));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.targetPlayer != null) tag.putUUID("Target", this.targetPlayer);
        tag.putInt("Range", this.despawnRange);
        tag.putBoolean("Triggered", this.flashbackTriggered);
        tag.putBoolean("Airborne", this.isAirborne());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Target")) this.targetPlayer = tag.getUUID("Target");
        int savedRange = tag.getInt("Range");
        this.despawnRange = savedRange > 0 ? savedRange : 14 + this.random.nextInt(5);
        this.flashbackTriggered = tag.getBoolean("Triggered");
        if (tag.getBoolean("Airborne")) this.setAirborne(true);
    }
}