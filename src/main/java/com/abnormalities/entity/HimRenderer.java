package com.abnormalities.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class HimRenderer extends GeoEntityRenderer<HimEntity> {
    public HimRenderer(EntityRendererProvider.Context context) {
        super(context, new HimModel());
        this.shadowRadius = 0.5F;
    }

    @Override
    public void render(HimEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
}
