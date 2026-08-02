package com.abnormalities.entity;

import com.abnormalities.config.AbnormalitiesConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;

public class HimEntity extends PathfinderMob implements RangedAttackMob {
    private static final EntityDataAccessor<Float> DATA_COLLAPSE = SynchedEntityData.defineId(HimEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_BOSS = SynchedEntityData.defineId(HimEntity.class, EntityDataSerializers.BOOLEAN);

    private int bridgeCooldown = 0;
    private int towerCooldown = 0;
    private boolean lineSent = false;
    private boolean achSent = false;
    private boolean punished = false;
    private boolean summoned = false;
    private java.util.UUID lastVictimHit = null;
    private int lastVictimHitTick = -999;
    private ServerBossEvent bossBar = null;
    private boolean trackedActive = false;
    private int ambienceTick = 0;

    public HimEntity(EntityType<? extends HimEntity> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.noCulling = true;
        this.setPersistenceRequired();
    }

    public boolean isBoss() {
        return this.entityData.get(DATA_BOSS);
    }

    public void markBoss() {
        this.entityData.set(DATA_BOSS, true);
        if (!this.level().isClientSide) {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(120.0D);
            this.setHealth(120.0F);
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(8.0D);
            this.bossBar = new ServerBossEvent(Component.literal("Him"), net.minecraft.world.BossEvent.BossBarColor.WHITE, net.minecraft.world.BossEvent.BossBarOverlay.PROGRESS);
            this.bossBar.setCreateWorldFog(true);
            this.bossBar.setDarkenScreen(true);
            if (!this.trackedActive) {
                this.trackedActive = true;
                HimTracker.incActiveBosses();
            }
        }
    }

    public float collapseProgress() {
        return this.entityData.get(DATA_COLLAPSE);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_COLLAPSE, 0.0F);
        this.entityData.define(DATA_BOSS, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.ARMOR, 6.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false) {
            @Override
            protected double getAttackReachSqr(LivingEntity attacker) {
                return 4.0D;
            }
        });
        this.goalSelector.addGoal(3, new RangedAttackGoal(this, 1.0D, 30, 18.0F) {
            @Override
            public boolean canUse() {
                return parentBoss() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return parentBoss() && super.canContinueToUse();
            }

            private boolean parentBoss() {
                return isBoss();
            }
        });
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 24.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        if (target == null) return;
        for (int i = 0; i < (isBoss() ? 2 : 1); i++) {
            Arrow arrow = new Arrow(this.level(), this);
            arrow.setPos(this.getX(), this.getEyeY() - 0.1D, this.getZ());
            double dx = target.getX() - this.getX();
            double dy = target.getY(0.5D) - arrow.getY();
            double dz = target.getZ() - this.getZ();
            arrow.shoot(dx, dy, dz, 1.9F, 1.0F);
            this.level().addFreshEntity(arrow);
        }
        this.swing(InteractionHand.MAIN_HAND);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity le && !this.level().isClientSide) {
            this.lastVictimHit = le.getUUID();
            this.lastVictimHitTick = this.tickCount;
        }
        return hit;
    }

    private void bossCall() {
        LivingEntity target = this.getTarget();
        if (!(target instanceof ServerPlayer sp)) return;
        int roll = this.random.nextInt(5);
        switch (roll) {
            case 0 -> com.abnormalities.registry.ModEvents.forceNurSpawn(sp);
            case 1 -> com.abnormalities.registry.ModEvents.forceItSpawn(sp);
            case 2, 3 -> {
                for (int i = 0; i < 3; i++) {
                    com.abnormalities.registry.ModEvents.forceHimSpawn(sp, false);
                }
            }
            default -> com.abnormalities.horror.HorrorEventPool.getRegistered().stream()
                    .filter(e -> e.getName().equals("the tally"))
                    .findFirst()
                    .ifPresent(e -> com.abnormalities.horror.HorrorEventPool.fireEvent((ServerPlayer) target, e));
        }
    }

    private void punish(ServerPlayer target) {
        if (target.connection == null) return;
        var mode = AbnormalitiesConfig.HIM_PUNISH.get();
        if (mode == AbnormalitiesConfig.PunishMode.CRASH) {
            com.abnormalities.AbnormalitiesMod.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> target),
                    new com.abnormalities.network.CrashPacket());
        } else if (mode == AbnormalitiesConfig.PunishMode.KICK) {
            com.abnormalities.horror.SisterController.onKickWarning(target);
            target.connection.disconnect(Component.literal("him got you."));
        }
    }

    private void broadcastLine() {
        var srv = this.level().getServer();
        if (srv == null) return;
        String line = "<Him> " + (isBoss() ? "I DO NOT FALL TWICE" : HimDialogue.randomLine());
        var players = new ArrayList<>(srv.getPlayerList().getPlayers());
        for (var p : players) {
            p.connection.send(new net.minecraft.network.protocol.game.ClientboundSystemChatPacket(
                    Component.literal(line), false));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        if (this.bossBar != null) {
            var srv = this.level().getServer();
            if (srv != null) {
                var players = new ArrayList<>(srv.getPlayerList().getPlayers());
                for (var p : players) {
                    if (!this.bossBar.getPlayers().contains(p)) this.bossBar.addPlayer(p);
                }
            }
            this.bossBar.setProgress(this.getHealth() / this.getMaxHealth());
            this.ambienceTick++;
            if (this.ambienceTick >= 100) {
                this.ambienceTick = 0;
                var srv2 = this.level().getServer();
                if (srv2 != null) {
                    for (var p : new ArrayList<>(srv2.getPlayerList().getPlayers())) {
                        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                            net.minecraft.core.Holder.direct(com.abnormalities.registry.ModSounds.VR9P_AMBIENCE.get()),
                            SoundSource.MASTER, this.getX(), this.getY(), this.getZ(), 3.0f, 1.0f, 0));
                    }
                }
            }
        }
        LivingEntity tgt = this.getTarget();
        if (tgt instanceof ServerPlayer tv && !tv.isAlive() && !this.punished
                && tv.getUUID().equals(this.lastVictimHit) && this.tickCount - this.lastVictimHitTick < 60) {
            this.punished = true;
            this.punish(tv);
            this.getNavigation().stop();
        }
        if (this.isBoss() && !this.summoned) {
            this.summoned = true;
            this.bossCall();
        }
        if (this.entityData.get(DATA_COLLAPSE) > 0.0F) {
            this.setDeltaMovement(0, 0, 0);
            this.setNoAi(true);
            float p = this.entityData.get(DATA_COLLAPSE) + 0.05F;
            if (p >= 1.0F) {
                if (!this.lineSent) {
                    this.lineSent = true;
                    this.broadcastLine();
                }
                this.removeBossBar();
                this.discard();
                return;
            }
            this.entityData.set(DATA_COLLAPSE, p);
            return;
        }
        if (this.bridgeCooldown > 0) this.bridgeCooldown--;
        if (this.towerCooldown > 0) this.towerCooldown--;
        if (this.getTarget() != null) {
            this.playerBridge();
        }
        if (!this.achSent) {
            this.achSent = true;
            var srv = this.level().getServer();
            if (srv != null) {
                var players = new ArrayList<>(srv.getPlayerList().getPlayers());
                for (var p : players) {
                    if (p.distanceToSqr(this) < 40.0D * 40.0D) {
                        com.abnormalities.horror.FakeAchievementManager.giveNamed(p, "Him");
                    }
                }
            }
        }
    }

    private void playerBridge() {
        LivingEntity target = this.getTarget();
        if (target == null) return;
        Vec3 delta = new Vec3(target.getX() - this.getX(),
                target.getY() - this.getY(),
                target.getZ() - this.getZ());
        double horiz = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        BlockPos pos = this.blockPosition();
        int stepX = (int) Math.signum(delta.x);
        int stepZ = (int) Math.signum(delta.z);

        if (delta.y > 1.0D && towerCooldown <= 0 && this.onGround()) {
            BlockState above = this.level().getBlockState(pos.above());
            BlockState below = this.level().getBlockState(pos);
            if (above.isAir() && below.canOcclude()) {
                this.jumpFromGround();
                this.level().setBlockAndUpdate(pos, Blocks.COBBLESTONE.defaultBlockState());
                this.moveTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
                towerCooldown = isBoss() ? 4 : 6;
                this.hasImpulse = true;
            }
        }

        if (horiz > 1.8D && bridgeCooldown <= 0) {
            BlockPos next = pos.offset(stepX, 0, stepZ);
            BlockState nextState = this.level().getBlockState(next);
            if (nextState.isAir()) {
                BlockPos underNext = next.below();
                BlockState underNextState = this.level().getBlockState(underNext);
                if (!underNextState.canOcclude() && !underNextState.is(Blocks.WATER)) {
                    for (int checkY = 0; checkY >= -4; checkY--) {
                        BlockPos chk = next.offset(0, checkY, 0);
                        if (this.level().getBlockState(chk).canOcclude()) {
                            BlockPos placeAt = chk.above();
                            if (this.level().getBlockState(placeAt).isAir()) {
                                this.level().setBlockAndUpdate(placeAt, Blocks.COBBLESTONE.defaultBlockState());
                                bridgeCooldown = isBoss() ? 8 : 14;
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    private void removeBossBar() {
        if (this.bossBar != null) {
            for (var p : new ArrayList<>(this.bossBar.getPlayers())) {
                this.bossBar.removePlayer(p);
            }
            this.bossBar = null;
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        this.removeBossBar();
        if (this.trackedActive) {
            this.trackedActive = false;
            HimTracker.decActiveBosses();
        }
        super.remove(reason);
    }

    public void beginCollapse() {
        if (this.entityData.get(DATA_COLLAPSE) > 0.0F) return;
        this.entityData.set(DATA_COLLAPSE, 0.01F);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.entityData.get(DATA_COLLAPSE) > 0.0F) return false;
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        if (this.entityData.get(DATA_COLLAPSE) > 0.0F) return;
        this.setHealth(1.0F);
        this.beginCollapse();
        this.setNoAi(true);
        this.getNavigation().stop();
        if (!this.level().isClientSide && source.getEntity() instanceof ServerPlayer sp && !this.lineSent) {
            if (isBoss()) {
                HimTracker.bossKilled(sp);
            } else {
                HimTracker.himKilled(sp);
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Boss", this.entityData.get(DATA_BOSS));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Boss") && tag.getBoolean("Boss")) this.markBoss();
    }
}