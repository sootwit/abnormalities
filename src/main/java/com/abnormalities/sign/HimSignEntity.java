package com.abnormalities.sign;

import com.abnormalities.AbnormalitiesMod;
import com.abnormalities.entity.HimEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class HimSignEntity extends HimEntity {
    private int wanderTicks = 0;

    public HimSignEntity(EntityType<? extends HimSignEntity> type, Level level) {
        super(type, level);
        this.achSent = true;
        this.bridgeCooldown = Integer.MAX_VALUE;
        this.towerCooldown = Integer.MAX_VALUE;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 15.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.ARMOR, 4.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 24.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;
        if (this.getTarget() == null) {
            wanderTicks++;
            if (wanderTicks > 40) {
                Player nearest = this.level().getNearestPlayer(this, 48.0D);
                if (nearest != null) this.setTarget(nearest);
            }
        } else {
            wanderTicks = 0;
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        this.lastVictimHit = null;
        return hit;
    }

    @Override
    public void die(DamageSource source) {
        this.setNoAi(true);
        super.die(source);
    }

    public static class HimSignRenderer extends GeoEntityRenderer<HimSignEntity> {
        public HimSignRenderer(EntityRendererProvider.Context context) {
            super(context, new HimSignModel());
            this.shadowRadius = 0.5F;
        }
    }

    public static class HimSignModel extends GeoModel<HimSignEntity> {
        @Override
        public ResourceLocation getModelResource(HimSignEntity object) {
            return new ResourceLocation(AbnormalitiesMod.MODID, "geo/him.geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(HimSignEntity object) {
            return new ResourceLocation(AbnormalitiesMod.MODID, "textures/entity/himchase.png");
        }

        @Override
        public ResourceLocation getAnimationResource(HimSignEntity object) {
            return new ResourceLocation(AbnormalitiesMod.MODID, "animations/him.animation.json");
        }
    }
}
