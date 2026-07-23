package com.abnormalities.entity;

import com.abnormalities.config.AbnormalitiesConfig;
import com.abnormalities.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;
import java.util.UUID;

public class NurEntity extends Mob {
    private static final EntityDataAccessor<Boolean> DATA_CHASING = SynchedEntityData.defineId(NurEntity.class, EntityDataSerializers.BOOLEAN);
    public boolean isChasing() {
        return this.entityData.get(DATA_CHASING);
    }
    public enum State { STALKING, CHASING }
    public State currentState = State.STALKING;
    public int soundTick = -1;
    public int silenceTimer = 0;
    public Player currentTarget = null;
    public UUID chasedPlayerId;
    private boolean hasPlayedSecondSound = false;
    private int attackCooldown = 0;
    private int soundLoopTick = 0;

    public NurEntity(EntityType<? extends NurEntity> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.setPersistenceRequired();
        this.noCulling = true;
    }
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_CHASING, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 9999.0D)
                .add(Attributes.MOVEMENT_SPEED, 1.8D)
                .add(Attributes.ATTACK_DAMAGE, 999.0D)
                .add(Attributes.FOLLOW_RANGE, 128.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(source, amount);
        }
        Entity direct = source.getDirectEntity();
        boolean fromProjectile = source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE);
        boolean fromExplosion = source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION);
        boolean fromNonLiving = direct != null && !(direct instanceof LivingEntity);
        if (fromProjectile || fromExplosion || fromNonLiving) {
            return super.hurt(source, amount);
        }
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (currentTarget == null || currentTarget.isRemoved() || !currentTarget.isAlive()) {
            currentTarget = findNearestPlayer();
            if (currentTarget == null) {
                this.entityData.set(DATA_CHASING, false);
                if (currentState == State.CHASING) {
                    if (chasedPlayerId != null) NurHorrorCycle.stop(chasedPlayerId, this.getUUID());
                    chasedPlayerId = null;
                    currentState = State.STALKING;
                }
                if (tickCount > 200) {
                    discard();
                }
                return;
            }
            soundLoopTick = 0;
            soundTick = -1;
        }
        if (silenceTimer > 0) silenceTimer--;
        if (attackCooldown > 0) attackCooldown--;
        if (isChasing()) {
            replaceFluidsUnderneath();
        }
        switch (currentState) {
            case STALKING -> tickStalking();
            case CHASING -> tickChasing();
        }
    }

    private void replaceFluidsUnderneath() {
        if (!AbnormalitiesConfig.NUR_WATER.get() && !AbnormalitiesConfig.NUR_LAVA.get() && !AbnormalitiesConfig.NUR_LIQUID.get()) return;
        BlockPos pos = this.blockPosition();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos check = pos.offset(dx, 0, dz);
                BlockState state = level().getBlockState(check);
                if (state.is(Blocks.WATER) && AbnormalitiesConfig.NUR_WATER.get()) {
                    level().setBlockAndUpdate(check, Blocks.STONE.defaultBlockState());
                } else if (state.is(Blocks.LAVA) && AbnormalitiesConfig.NUR_LAVA.get()) {
                    level().setBlockAndUpdate(check, Blocks.STONE.defaultBlockState());
                } else if (AbnormalitiesConfig.NUR_LIQUID.get()) {
                    var fluid = state.getFluidState();
                    if (fluid.isSource() && !fluid.is(net.minecraft.tags.FluidTags.WATER) && !fluid.is(net.minecraft.tags.FluidTags.LAVA)) {
                        level().setBlockAndUpdate(check, Blocks.STONE.defaultBlockState());
                    }
                }
            }
        }
    }

    private void tickStalking() {
        this.getNavigation().stop();
        this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
        if (soundTick == -1) {
            Player nearPlayer = findNearestPlayer();
            if (nearPlayer != null) {
                level().playSound(null, nearPlayer.getX(), nearPlayer.getY(), nearPlayer.getZ(),
                        net.minecraft.sounds.SoundEvents.AMBIENT_CAVE.get(), SoundSource.MASTER, 6.0f, 0.3f);
            }
            soundTick = 0;
        }
    }

    private void tickChasing() {
        if (currentTarget == null || currentTarget.isRemoved() || !currentTarget.isAlive()) {
            discard();
            return;
        }
        if (soundTick >= 0 && soundTick < 60) {
            soundTick++;
        } else if (soundTick == 60 && !hasPlayedSecondSound) {
            if (currentTarget != null) {
                level().playSound(null, currentTarget.getX(), currentTarget.getY(), currentTarget.getZ(),
                        ModSounds.NUR_SOUND.get(), SoundSource.MASTER, 6.0f, 0.5f);
            }
            hasPlayedSecondSound = true;
            soundTick = 61;
            soundLoopTick = 80;
        }
        if (soundLoopTick > 0) {
            soundLoopTick--;
            if (soundLoopTick <= 0 && currentTarget != null) {
                level().playSound(null, currentTarget.getX(), currentTarget.getY(), currentTarget.getZ(),
                        ModSounds.NUR_SOUND.get(), SoundSource.MASTER, 6.0f, 0.5f);
                soundLoopTick = 100;
            }
        }
        this.getNavigation().stop();
        double dist = this.distanceTo(currentTarget);
        if (dist > 2.5D) {
            pushTowardTarget();
            tryReachTarget();
            this.getLookControl().setLookAt(currentTarget, 30, 30);
        }
        if (dist < 3.0D && attackCooldown <= 0) {
            currentTarget.hurt(this.damageSources().mobAttack(this), Float.MAX_VALUE);
            attackCooldown = 20;
            silenceTimer = 40;
        }
    }

    private void pushTowardTarget() {
        if (currentTarget == null) return;
        double dx = currentTarget.getX() - this.getX();
        double dz = currentTarget.getZ() - this.getZ();
        double dy = currentTarget.getY() - this.getY();
        double horizDist = Math.sqrt(dx * dx + dz * dz);
        double speed = 1.8D;
        double mx = 0, mz = 0;
        if (horizDist > 0.1) {
            mx = dx / horizDist * speed;
            mz = dz / horizDist * speed;
        }
        double my = this.getDeltaMovement().y;
        if (dy < -1) {
            my = -0.8D;
        }
        if (this.onGround() && horizDist > 1.5) {
            BlockPos ahead = this.blockPosition().offset((int)Math.signum(dx), 1, (int)Math.signum(dz));
            if (!level().getBlockState(ahead).isAir() || !level().getBlockState(ahead.below()).isAir()) {
                this.jumpFromGround();
            }
            if (AbnormalitiesConfig.NUR_BREAK_BLOCKS.get()) {
                BlockPos aheadGround = this.blockPosition().offset((int)Math.signum(dx), 0, (int)Math.signum(dz));
                BlockState aheadState = level().getBlockState(aheadGround);
                if (!aheadState.isAir() && !aheadState.is(Blocks.BEDROCK) && !aheadState.is(Blocks.COBBLESTONE)) {
                    level().destroyBlock(aheadGround, AbnormalitiesConfig.NUR_BREAK_DROPS.get());
                }
            }
        }
        this.setDeltaMovement(mx, my, mz);
        this.hasImpulse = true;
    }

    private void tryReachTarget() {
        if (currentTarget == null) return;
        BlockPos targetPos = currentTarget.blockPosition();
        BlockPos nurPos = this.blockPosition();
        int dy = targetPos.getY() - nurPos.getY();

        if (dy < -1) {
            double dx = targetPos.getX() + 0.5 - this.getX();
            double dz = targetPos.getZ() + 0.5 - this.getZ();
            double hDist = Math.sqrt(dx * dx + dz * dz);
            if (hDist < 2.0 && dy < -3) {
                int safeY = level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, this.blockPosition().getX(), this.blockPosition().getZ());
                this.teleportTo(this.getX(), safeY, this.getZ());
                return;
            }
            if (AbnormalitiesConfig.NUR_BREAK_BLOCKS.get()) {
                for (int bx = -2; bx <= 2; bx++) {
                    for (int bz = -2; bz <= 2; bz++) {
                        BlockPos below = nurPos.offset(bx, -1, bz);
                        BlockState belowState = level().getBlockState(below);
                        if (!belowState.isAir() && !belowState.is(Blocks.BEDROCK)) {
                            level().destroyBlock(below, false);
                        }
                    }
                }
            }
            this.setDeltaMovement(this.getDeltaMovement().x, -1.0D, this.getDeltaMovement().z);
            return;
        }

        if (AbnormalitiesConfig.NUR_BREAK_BLOCKS.get()) {
            int stepX = (int) Math.signum(targetPos.getX() + 0.5 - this.getX());
            int stepZ = (int) Math.signum(targetPos.getZ() + 0.5 - this.getZ());
            for (int bx = -2; bx <= 2; bx++) {
                for (int bz = -2; bz <= 2; bz++) {
                    for (int by = 0; by < 3; by++) {
                        BlockPos check = nurPos.offset(bx + (stepX != 0 ? stepX : 0), by, bz + (stepZ != 0 ? stepZ : 0));
                        BlockState state = level().getBlockState(check);
                        if (!state.isAir() && !state.is(Blocks.BEDROCK) && !state.is(Blocks.COBBLESTONE)) {
                            level().destroyBlock(check, AbnormalitiesConfig.NUR_BREAK_DROPS.get());
                        }
                        if (state.is(Blocks.COBBLESTONE) && level().getGameTime() % 60 == 0) {
                            level().destroyBlock(check, AbnormalitiesConfig.NUR_BREAK_DROPS.get());
                        }
                    }
                }
            }
        }

        if (AbnormalitiesConfig.NUR_TOWER.get() && dy > 0) {
            BlockPos above = nurPos.above();
            BlockState aboveState = level().getBlockState(above);
            if (!aboveState.isAir() && !aboveState.is(Blocks.BEDROCK) && !aboveState.getFluidState().isSource()) {
                if (AbnormalitiesConfig.NUR_BREAK_BLOCKS.get()) {
                    level().destroyBlock(above, AbnormalitiesConfig.NUR_BREAK_DROPS.get());
                }
            } else if (aboveState.isAir()) {
                level().setBlockAndUpdate(nurPos, Blocks.COBBLESTONE.defaultBlockState());
                this.moveTo(nurPos.getX() + 0.5, nurPos.getY() + 1, nurPos.getZ() + 0.5);
                nurPos = this.blockPosition();
            }
        }

        if (AbnormalitiesConfig.NUR_BRIDGE.get()) {
            double dx = targetPos.getX() + 0.5 - this.getX();
            double dz = targetPos.getZ() + 0.5 - this.getZ();
            double horizDist = Math.sqrt(dx * dx + dz * dz);
            if (horizDist > 1.5) {
                int stepX = (int) Math.signum(dx);
                int stepZ = (int) Math.signum(dz);
                BlockPos bridgeTarget;
                if (Math.abs(dx) > Math.abs(dz)) {
                    bridgeTarget = nurPos.offset(stepX, 0, 0);
                } else {
                    bridgeTarget = nurPos.offset(0, 0, stepZ);
                }
                boolean placed = false;
                for (int checkY = 0; checkY >= -4; checkY--) {
                    BlockPos check = bridgeTarget.offset(0, checkY, 0);
                    if (level().getBlockState(check).canOcclude()) {
                        BlockPos placeAt = bridgeTarget.offset(0, checkY + 1, 0);
                        if (level().getBlockState(placeAt).isAir()) {
                            level().setBlockAndUpdate(placeAt, Blocks.COBBLESTONE.defaultBlockState());
                        }
                        placed = true;
                        break;
                    }
                }
                if (!placed) {
                    level().setBlockAndUpdate(bridgeTarget.below(), Blocks.COBBLESTONE.defaultBlockState());
                }
            }
        }
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        if (target instanceof Player p) {
            p.hurt(this.damageSources().mobAttack(this), Float.MAX_VALUE);
            return true;
        }
        return false;
    }

    public void startChasing(Player player) {
        if (currentState == State.CHASING) return;
        currentState = State.CHASING;
        currentTarget = player;
        soundTick = 0;
        hasPlayedSecondSound = false;
        attackCooldown = 0;
        soundLoopTick = 0;
        this.entityData.set(DATA_CHASING, true);
    if (!level().isClientSide) {
        level().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.NUR_SOUND.get(), SoundSource.MASTER, 6.0f, 1.0f);
        this.chasedPlayerId = player.getUUID();
        NurHorrorCycle.start(this.chasedPlayerId, this.getUUID());
    }
    }

    private Player findNearestPlayer() {
        return level().getNearestPlayer(this, 64.0D);
    }

    @Override
    public net.minecraft.world.scores.PlayerTeam getTeam() {
        return null;
    }

    @Override
    public boolean isAlliedTo(net.minecraft.world.entity.Entity other) {
        return false;
    }

    @Override
    public void remove(net.minecraft.world.entity.Entity.RemovalReason reason) {
    if (level() != null && !level().isClientSide && currentState == State.CHASING && chasedPlayerId != null) {
        NurHorrorCycle.stop(chasedPlayerId, this.getUUID());
    }
        super.remove(reason);
        if (level() != null && !level().isClientSide) {
            this.entityData.set(DATA_CHASING, false);
        }
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("NurState", currentState.name());
        tag.putBoolean("Chasing", isChasing());
        if (currentTarget != null) tag.putUUID("TargetUUID", currentTarget.getUUID());
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("NurState")) {
            try { currentState = State.valueOf(tag.getString("NurState")); } catch (Exception ignored) {}
        }
        if (tag.contains("TargetUUID") && level().getServer() != null) {
            currentTarget = level().getServer().getPlayerList().getPlayer(tag.getUUID("TargetUUID"));
        }
        if (tag.getBoolean("Chasing") && currentState == State.CHASING && currentTarget != null) {
            this.entityData.set(DATA_CHASING, true);
            this.chasedPlayerId = currentTarget.getUUID();
            if (!level().isClientSide) NurHorrorCycle.start(this.chasedPlayerId, this.getUUID());
        }
    }
}
