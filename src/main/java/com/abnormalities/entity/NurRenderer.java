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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class NurRenderer extends EntityRenderer<NurEntity> {
    private static final ResourceLocation TEXTURE_STALK = new ResourceLocation("abnormalities", "textures/entity/nur_stalk.png");
    private static final ResourceLocation TEXTURE_CHASE = new ResourceLocation("abnormalities", "textures/entity/nur_chase.png");
    public NurRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.0F;
    }
    @Override
    public ResourceLocation getTextureLocation(NurEntity entity) {
        return entity.isChasing() ? TEXTURE_CHASE : TEXTURE_STALK;
    }
    @Override
    public int getBlockLightLevel(NurEntity entity, BlockPos pos) {
        return 15;
    }
    @Override
    public void render(NurEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        var mc = Minecraft.getInstance();
        var player = mc.cameraEntity;
        if (player == null) return;
        float hw = 2.0F;
        float hh = 2.67F;
        poseStack.pushPose();
        poseStack.translate(0, hh, 0);
        Vec3 camPos = player.getEyePosition(partialTick);
        double dx = entity.getX() - camPos.x;
        double dy = (entity.getY() + hh) - camPos.y;
        double dz = entity.getZ() - camPos.z;
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        float pitch = (float) -(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * (180.0D / Math.PI));
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        ResourceLocation tex = getTextureLocation(entity);
        RenderType renderType = entity.isChasing() ?
                RenderType.entityTranslucent(tex) :
                RenderType.entityCutoutNoCull(tex);
        VertexConsumer vc = bufferSource.getBuffer(renderType);
        var m = poseStack.last().pose();
        var n = poseStack.last().normal();
        int light = 15 << 20 | 15 << 4;
        int alpha = 255;
        vc.vertex(m, -hw, -hh, 0).color(255, 255, 255, alpha).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0, 0, 1).endVertex();
        vc.vertex(m, hw, -hh, 0).color(255, 255, 255, alpha).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0, 0, 1).endVertex();
        vc.vertex(m, hw, hh, 0).color(255, 255, 255, alpha).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0, 0, 1).endVertex();
        vc.vertex(m, -hw, hh, 0).color(255, 255, 255, alpha).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0, 0, 1).endVertex();
        poseStack.popPose();
    }
}
