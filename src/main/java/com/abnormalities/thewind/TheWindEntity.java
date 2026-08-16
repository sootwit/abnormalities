package com.abnormalities.thewind;

import com.abnormalities.config.AbnormalitiesConfig;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class TheWindEntity extends Mob {
    private UUID targetUUID;
    private int ticksExisted;
    private int nextTeleportTick;
    private boolean currentlyInFront;
    private boolean visible;
    private int appearCount = 0;

    public TheWindEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        this.setNoAi(true);
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.setSilent(true);
        this.setPersistenceRequired();
        this.ticksExisted = 0;
        this.visible = false;
        this.nextTeleportTick = 0;
        this.currentlyInFront = false;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 9999.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 0.0D);
    }

    public void setTarget(Player player) {
        this.targetUUID = player.getUUID();
    }

    public UUID getTargetUUID() {
        return targetUUID;
    }

    public boolean isVisible() {
        return visible;
    }

    @Override
    public void tick() {
        super.tick();
        ticksExisted++;

        if (level().isClientSide) return;
        if (ticksExisted < 60) return;

        if (targetUUID == null) {
            discard();
            return;
        }

        Player target = level().getPlayerByUUID(targetUUID);
        if (target == null || !target.isAlive()) {
            discard();
            return;
        }

        if (nextTeleportTick <= 0) {
            currentlyInFront = level().random.nextBoolean();
            nextTeleportTick = 100 + level().random.nextInt(101);
        }

        if (ticksExisted >= nextTeleportTick) {
            currentlyInFront = level().random.nextBoolean();
            nextTeleportTick = ticksExisted + 100 + level().random.nextInt(101);
            appearCount++;
            if (currentlyInFront) {
                level().playSound(null, target.blockPosition(),
                        SoundEvents.AMBIENT_CAVE.get(), SoundSource.AMBIENT, 3.0F, 0.3F);
            }
        }

        Vec3 eyePos = target.getEyePosition();
        Vec3 look = target.getLookAngle();
        Vec3 direction = currentlyInFront ? look : look.scale(-1.0D);
        Vec3 pos = eyePos.add(direction.scale(3.0D));
        this.moveTo(pos.x, pos.y, pos.z, target.getYRot(), 0);

        Vec3 toEntity = this.position().subtract(target.getEyePosition()).normalize();
        double dot = target.getLookAngle().dot(toEntity);
        visible = dot > 0.7;

        if (ticksExisted > 600 && !visible) {
            if (level().random.nextInt(2000) == 0) {
                discard();
            }
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Target")) {
            this.targetUUID = tag.getUUID("Target");
        }
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (targetUUID != null) {
            tag.putUUID("Target", targetUUID);
        }
    }
}