package com.abnormalities.entity.skinwalker;

import com.abnormalities.ReputationManager;
import com.abnormalities.config.AbnormalitiesConfig;
import com.abnormalities.entity.NurEntity;
import com.abnormalities.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class NurSkinwalkerApproachGoal extends Goal {
    private final Mob mob;
    private Player targetPlayer;
    private int proximityTimer;
    private int pathRecalcTimer;
    private Vec3 wanderTarget;
    private double lastPlayerX;
    private double lastPlayerZ;
    private boolean directPursuit;

    public NurSkinwalkerApproachGoal(Mob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (mob.level().isClientSide) return false;
        if (!mob.getPersistentData().getBoolean("abnormalities:skinwalker")) return false;
        targetPlayer = mob.level().getNearestPlayer(mob, AbnormalitiesConfig.SW_DETECTION_RANGE.get());
        return targetPlayer != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (!mob.isAlive()) return false;
        if (!mob.getPersistentData().getBoolean("abnormalities:skinwalker")) return false;
        if (targetPlayer == null || targetPlayer.isRemoved() || !targetPlayer.isAlive()
                || targetPlayer.level() != mob.level()) {
            targetPlayer = mob.level().getNearestPlayer(mob, AbnormalitiesConfig.SW_DETECTION_RANGE.get());
        }
        return targetPlayer != null;
    }

    @Override
    public void start() {
        proximityTimer = 0;
        pathRecalcTimer = 0;
        wanderTarget = null;
        directPursuit = false;
        if (targetPlayer != null) {
            lastPlayerX = targetPlayer.getX();
            lastPlayerZ = targetPlayer.getZ();
        }
    }

    @Override
    public void tick() {
        if (targetPlayer == null) return;
        double dist = mob.distanceTo(targetPlayer);
        double speed = AbnormalitiesConfig.SW_APPROACH_SPEED.get();
        int transformTime = AbnormalitiesConfig.SW_TRANSFORM_TIME.get();
        if (dist < 2.0D) {
            proximityTimer++;
            mob.getLookControl().setLookAt(targetPlayer.getX(), targetPlayer.getEyeY(), targetPlayer.getZ(), 30, 30);
            mob.getNavigation().stop();
            if (proximityTimer >= transformTime) {
                transformIntoNur();
            }
        } else {
            proximityTimer = Math.max(0, proximityTimer - 2);
            boolean playerMoved = Math.abs(targetPlayer.getX() - lastPlayerX) > 1.5 ||
                    Math.abs(targetPlayer.getZ() - lastPlayerZ) > 1.5;
            if (playerMoved) {
                directPursuit = false;
                wanderTarget = null;
                lastPlayerX = targetPlayer.getX();
                lastPlayerZ = targetPlayer.getZ();
            }
            if (!directPursuit && wanderTarget == null) {
                double angle = mob.getRandom().nextDouble() * Math.PI * 2;
                double radius = 4.0 + mob.getRandom().nextDouble() * 4.0;
                double wx = targetPlayer.getX() + Math.cos(angle) * radius;
                double wz = targetPlayer.getZ() + Math.sin(angle) * radius;
                int wy = mob.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                        (int) wx, (int) wz);
                wanderTarget = new Vec3(wx, wy + 1, wz);
                pathRecalcTimer = 0;
            }
            pathRecalcTimer--;
            if (pathRecalcTimer <= 0) {
                if (directPursuit) {
                    mob.getNavigation().moveTo(targetPlayer, speed);
                } else if (wanderTarget != null) {
                    mob.getNavigation().moveTo(wanderTarget.x, wanderTarget.y, wanderTarget.z, speed);
                    if (mob.distanceToSqr(wanderTarget) < 4.0) {
                        wanderTarget = null;
                        if (!playerMoved) {
                            directPursuit = true;
                        }
                    }
                }
                pathRecalcTimer = 10 + mob.getRandom().nextInt(10);
            }
            if (dist > 50 && mob.getNavigation().isDone()) {
                Vec3 diff = new Vec3(targetPlayer.getX() - mob.getX(), 0, targetPlayer.getZ() - mob.getZ());
                double len = diff.length();
                if (len > 0.01) {
                    mob.setDeltaMovement(diff.x / len * speed, mob.getDeltaMovement().y, diff.z / len * speed);
                }
            }
            if (mob.isInWater()) {
                if (mob.getY() < targetPlayer.getY()) {
                    mob.setDeltaMovement(mob.getDeltaMovement().add(0, 0.08, 0));
                }
            }
        }
    }

    private void transformIntoNur() {
        Level level = mob.level();
        if (level.isClientSide) return;
        if (targetPlayer != null) ReputationManager.addRep(targetPlayer, -75);
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
        wanderTarget = null;
        directPursuit = false;
        mob.getNavigation().stop();
    }
}
