package com.abnormalities.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class HimRenderer extends MobRenderer<HimEntity, HimModel> {
    private static final ResourceLocation TEX = new ResourceLocation("abnormalities", "textures/entity/him.png");
    private static final ResourceLocation TEX_CHASE = new ResourceLocation("abnormalities", "textures/entity/himchase.png");

    public HimRenderer(EntityRendererProvider.Context context) {
        super(context, new HimModel(context.bakeLayer(HimModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(HimEntity entity) {
        return entity.isBoss() ? TEX_CHASE : TEX;
    }
}