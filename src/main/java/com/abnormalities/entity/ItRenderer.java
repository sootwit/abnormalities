package com.abnormalities.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class ItRenderer extends EntityRenderer<ItEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("abnormalities", "textures/entity/it.png");
    private final ItModel model;

    public ItRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.0F;
        this.model = new ItModel(ctx.bakeLayer(ItModel.LAYER_LOCATION));
    }

    @Override
    public ResourceLocation getTextureLocation(ItEntity entity) {
        return TEXTURE;
    }

    @Override
    public int getBlockLightLevel(ItEntity entity, BlockPos pos) {
        return 15;
    }

    @Override
    public void render(ItEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        var mc = Minecraft.getInstance();
        var camera = mc.cameraEntity;
        if (camera == null) return;

        poseStack.pushPose();
        poseStack.translate(0, -1.5F, 0);

        Vec3 camPos = camera.getEyePosition(partialTick);
        double dx = entity.getX() - camPos.x;
        double dz = entity.getZ() - camPos.z;
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));

        float limbSwing = entity.walkAnimation.position(partialTick);
        float limbSwingAmount = entity.walkAnimation.speed(partialTick);
        this.model.setupAnim(entity, limbSwing, limbSwingAmount, entity.tickCount + partialTick, 0.0F, 0.0F);

        ResourceLocation tex = getTextureLocation(entity);
        RenderType renderType = RenderType.entityCutoutNoCull(tex);
        VertexConsumer vc = bufferSource.getBuffer(renderType);
        int light = 15 << 20 | 15 << 4;
        this.model.renderToBuffer(poseStack, vc, light, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);

        poseStack.popPose();
    }
}