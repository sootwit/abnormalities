package com.abnormalities.entity;

import com.abnormalities.ReputationManager;
import com.abnormalities.config.AbnormalitiesConfig;
import com.abnormalities.network.CrashPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;

public class ItEntity extends Mob {
    private static final EntityDataAccessor<Boolean> DATA_FROZEN = SynchedEntityData.defineId(ItEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_SWING = SynchedEntityData.defineId(ItEntity.class, EntityDataSerializers.FLOAT);

    private Player target;
    private int lookAwayTicks = 0;
    private int stareTicks = 0;
    private int steals = 0;
    private float lastSwing = 0.0F;

    public ItEntity(EntityType<? extends ItEntity> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.setPersistenceRequired();
        this.noCulling = true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 9999.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 128.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_FROZEN, false);
        this.entityData.define(DATA_SWING, 0.0F);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    protected boolean canRide(net.minecraft.world.entity.Entity vehicle) {
        return false;
    }

    public boolean isFrozen() {
        return this.entityData.get(DATA_FROZEN);
    }

    public float getHeldSwing() {
        return this.entityData.get(DATA_SWING);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;

        if (target == null || target.isRemoved() || !target.isAlive()) {
            target = level().getNearestPlayer(this, 64.0D);
            if (target == null) {
                this.entityData.set(DATA_FROZEN, false);
                if (tickCount > 200) discard();
                return;
            }
            lookAwayTicks = 0;
            stareTicks = 0;
        }

        long tod = level().getDayTime() % 24000L;
        if (tod >= 2000L && tod < 13000L) {
            discard();
            return;
        }

        if (!this.entityData.get(DATA_FROZEN)) {
            lastSwing = Math.max(lastSwing, this.walkAnimation.position());
            this.entityData.set(DATA_SWING, lastSwing);
        }

        boolean looking = isPlayerLookingAt(target);
        double dist = this.distanceTo(target);

        if (looking) {
            this.entityData.set(DATA_FROZEN, true);
            this.getNavigation().stop();
            this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
            lookAwayTicks = 0;
            if (dist <= 5.5D) {
                stareTicks++;
                if (stareTicks >= stareTargetTicks()) {
                    winStare();
                    return;
                }
            } else {
                stareTicks = 0;
            }
        } else {
            this.entityData.set(DATA_FROZEN, false);
            stareTicks = 0;
            lookAwayTicks++;
            this.getLookControl().setLookAt(target, 10, 10);

            if (dist < 1.5D) {
                stealFrom(target, true);
            } else if (dist < 1.6D) {
                stealFrom(target, false);
            } else {
                float speed = baseApproachSpeed() + (maxApproachSpeed() - baseApproachSpeed()) * Math.min(1.0F, lookAwayTicks / 120.0F);
                this.getNavigation().moveTo(target, speed);
            }
        }
    }

    private boolean isPlayerLookingAt(Player player) {
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F).normalize();
        Vec3 to = this.getEyePosition(1.0F).subtract(eye).normalize();
        return look.dot(to) > 0.92D;
    }

    private float baseApproachSpeed() {
        return 0.12F;
    }

    private float maxApproachSpeed() {
        return 0.42F;
    }

    private int stareTargetTicks() {
        return Math.max(20, AbnormalitiesConfig.IT_STARE_SECONDS.get() * 20);
    }

    private void winStare() {
        if (target != null) {
            ReputationManager.addRep(target, 40);
            if (target instanceof ServerPlayer sp) {
                var srv = sp.getServer();
                if (srv != null) {
                    for (var p : new ArrayList<>(srv.getPlayerList().getPlayers())) {
                        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSystemChatPacket(
                                Component.literal("<it> you chose to see me."), false));
                    }
                }
            }
        }
        this.discard();
    }

    private void stealFrom(Player victim, boolean wasLooking) {
        if (victim == null || level().isClientSide) return;
        int n = wasLooking ? AbnormalitiesConfig.IT_STEAL_COUNT.get() * 2 : AbnormalitiesConfig.IT_STEAL_COUNT.get();
        int took = 0;
        for (int i = 0; i < n; i++) {
            if (takeRandomItem(victim)) took++;
        }
        if (took > 0) steals++;

        if (victim instanceof ServerPlayer sp) {
            level().playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                    SoundEvents.GLASS_HIT, SoundSource.MASTER, 3.0F, 0.4F);
        }

        Vec3 tp = randomTeleportPos(victim);
        if (tp != null) {
            this.moveTo(tp.x, tp.y, tp.z, this.random.nextFloat() * 360.0F, 0);
        }
        lookAwayTicks = 0;
        if (steals >= 3) {
            if (victim instanceof ServerPlayer sp && !level().isClientSide) {
                var mode = AbnormalitiesConfig.IT_PUNISH.get();
                if (mode == AbnormalitiesConfig.PunishMode.KICK) {
                    com.abnormalities.horror.SisterController.onKickWarning(sp);
                    sp.connection.disconnect(Component.literal("it took it."));
                } else if (mode == AbnormalitiesConfig.PunishMode.CRASH) {
                    com.abnormalities.AbnormalitiesMod.CHANNEL.send(
                            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp),
                            new CrashPacket());
                }
            }
            this.discard();
        }
    }

    private boolean takeRandomItem(Player victim) {
        var inv = victim.getInventory();
        for (int a = 1; a < inv.items.size(); a++) {
            if (!inv.getItem(a).isEmpty()) {
                inv.removeItem(a, inv.getItem(a).getCount());
                return true;
            }
        }
        return false;
    }

    private Vec3 randomTeleportPos(Player victim) {
        double angle = this.random.nextDouble() * Math.PI * 2;
        double dist = 16.0D + this.random.nextDouble() * 8.0D;
        double x = victim.getX() + Math.cos(angle) * dist;
        double z = victim.getZ() + Math.sin(angle) * dist;
        int y = level().getHeight(Heightmap.Types.MOTION_BLOCKING, (int) x, (int) z);
        return new Vec3(x, y + 1, z);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurt(source, amount);
        net.minecraft.world.entity.Entity direct = source.getDirectEntity();
        boolean fromProjectile = source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE);
        boolean fromExplosion = source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION);
        boolean fromNonLiving = direct != null && !(direct instanceof net.minecraft.world.entity.LivingEntity);
        if (fromProjectile || fromExplosion || fromNonLiving) return super.hurt(source, amount);
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Steals", steals);
        tag.putBoolean("Frozen", this.entityData.get(DATA_FROZEN));
        tag.putFloat("Swing", this.entityData.get(DATA_SWING));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        steals = tag.getInt("Steals");
        if (tag.contains("Frozen")) this.entityData.set(DATA_FROZEN, tag.getBoolean("Frozen"));
        if (tag.contains("Swing")) this.entityData.set(DATA_SWING, tag.getFloat("Swing"));
    }
}