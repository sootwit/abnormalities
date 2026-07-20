package com.abnormalities.entity.skinwalker;

import com.abnormalities.config.AbnormalitiesConfig;
import com.abnormalities.entity.NurEntity;
import com.abnormalities.registry.ModEntities;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

public class NurSkinwalkerApproachGoal extends Goal {
    private final Mob mob;
    private Player targetPlayer;
    private int proximityTimer;
    private int pathRecalcTimer;

    public NurSkinwalkerApproachGoal(Mob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (mob.level().isClientSide) return false;
        double range = AbnormalitiesConfig.SW_DETECTION_RANGE.get();
        targetPlayer = mob.level().getNearestPlayer(mob, range);
        return targetPlayer != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (targetPlayer == null || targetPlayer.isRemoved() || !targetPlayer.isAlive()) return false;
        return mob.distanceTo(targetPlayer) < AbnormalitiesConfig.SW_DETECTION_RANGE.get() * 1.5;
    }

    @Override
    public void start() {
        proximityTimer = 0;
        pathRecalcTimer = 0;
    }

    @Override
    public void tick() {
        if (targetPlayer == null) return;
        double dist = mob.distanceTo(targetPlayer);
        double speed = mob.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        int transformTime = AbnormalitiesConfig.SW_TRANSFORM_TIME.get();
        Level level = mob.level();
        if (dist < 2.0D) {
            proximityTimer++;
            mob.getLookControl().setLookAt(targetPlayer.getX(), targetPlayer.getEyeY(), targetPlayer.getZ(), 30, 30);
            mob.getNavigation().stop();
            if (proximityTimer >= transformTime) {
                transformIntoNur();
            }
        } else {
            proximityTimer = Math.max(0, proximityTimer - 2);
            pathRecalcTimer--;
            if (pathRecalcTimer <= 0) {
                mob.getNavigation().moveTo(targetPlayer, speed);
                pathRecalcTimer = 20 + mob.getRandom().nextInt(20);
            }
            if (mob.isInWater() && mob.getY() < targetPlayer.getY()) {
                mob.setDeltaMovement(mob.getDeltaMovement().add(0, 0.05, 0));
            }
        }
    }

    private void transformIntoNur() {
        Level level = mob.level();
        if (level.isClientSide) return;
        NurEntity nur = ModEntities.NUR.get().create(level);
        if (nur == null) return;
        nur.moveTo(mob.getX(), mob.getY(), mob.getZ(), mob.getYRot(), mob.getXRot());
        level.addFreshEntity(nur);
        nur.startChasing(targetPlayer);
        level.playSound(null, targetPlayer.getX(), targetPlayer.getY(), targetPlayer.getZ(),
                SoundEvents.AMBIENT_CAVE.get(), SoundSource.MASTER, 6.0F, 0.3F);
        mob.discard();
    }

    @Override
    public void stop() {
        targetPlayer = null;
        proximityTimer = 0;
        mob.getNavigation().stop();
    }
}
