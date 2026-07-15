package com.abnormalities.entity.skinwalker;

import com.abnormalities.entity.NurEntity;
import com.abnormalities.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class SheepNurEntity extends Sheep {
    public SheepNurEntity(EntityType<? extends Sheep> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.23D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.getAvailableGoals().clear();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new NurSkinwalkerApproachGoal(this));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide && level() instanceof ServerLevel serverLevel) {
            int chance = com.abnormalities.config.AbnormalitiesConfig.SW_KILL_SPAWN_CHANCE.get();
            if (serverLevel.random.nextInt(100) < chance) {
                double dx = getX(), dy = getY(), dz = getZ();
                serverLevel.getServer().tell(new net.minecraft.server.TickTask(
                        serverLevel.getServer().getTickCount() + 40,
                        () -> {
                            NurEntity nur = ModEntities.NUR.get().create(serverLevel);
                            if (nur != null) {
                                nur.moveTo(dx, dy, dz, 0, 0);
                                Player nearest = serverLevel.getNearestPlayer(nur, 64.0);
                                if (nearest != null) nur.startChasing(nearest);
                                serverLevel.addFreshEntity(nur);
                            }
                        }
                ));
            }
        }
    }
}
