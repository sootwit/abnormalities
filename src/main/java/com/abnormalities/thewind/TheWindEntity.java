package com.abnormalities.thewind;

import com.abnormalities.config.AbnormalitiesConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class TheWindEntity extends Mob {
    private UUID targetUUID;
    private int ticksExisted;
    private boolean visible;

    public TheWindEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        this.setNoAi(true);
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.setSilent(true);
        this.setPersistenceRequired();
        this.ticksExisted = 0;
        this.visible = false;
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

        Vec3 targetPos = target.position();
        float yaw = target.getYRot();
        double behindX = targetPos.x - Math.sin(Math.toRadians(yaw)) * 3.0;
        double behindZ = targetPos.z + Math.cos(Math.toRadians(yaw)) * 3.0;
        double behindY = targetPos.y;

        this.moveTo(behindX, behindY, behindZ, yaw, 0);

        boolean hasSolidBack = hasSolidBlocksBehind(target);

        if (hasSolidBack) {
            visible = true;
        } else {
            visible = false;
        }

        if (ticksExisted > 600 && !visible) {
            if (level().random.nextInt(2000) == 0) {
                discard();
            }
        }
    }

    private boolean hasSolidBlocksBehind(Player player) {
        float yaw = player.getYRot();
        for (int dist = 1; dist <= 5; dist++) {
            double checkX = player.getX() - Math.sin(Math.toRadians(yaw)) * dist;
            double checkZ = player.getZ() + Math.cos(Math.toRadians(yaw)) * dist;
            BlockPos checkPos = new BlockPos((int) checkX, (int) player.getY(), (int) checkZ);
            BlockPos checkPos2 = new BlockPos((int) checkX, (int) (player.getY() + 1), (int) checkZ);
            BlockState state1 = level().getBlockState(checkPos);
            BlockState state2 = level().getBlockState(checkPos2);
            if (state1.canOcclude() || state2.canOcclude()) {
                return true;
            }
        }
        return false;
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
