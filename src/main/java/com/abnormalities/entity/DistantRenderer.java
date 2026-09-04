package com.abnormalities.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class DistantRenderer extends MobRenderer<DistantEntity, HumanoidModel<DistantEntity>> {
    private static final ResourceLocation TEX = new ResourceLocation("abnormalities", "textures/entity/distant.png");

    public DistantRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(DistantEntity entity) {
        return TEX;
    }

    @Override
    protected void scale(DistantEntity entity, PoseStack poseStack, float partialTicks) {
        poseStack.scale(1.0F, 1.0F, 1.0F);
    }
}