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

public class XyzRenderer extends EntityRenderer<XyzEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("abnormalities", "textures/entity/xyz.png");

    public XyzRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(XyzEntity entity) {
        return TEXTURE;
    }

    @Override
    public int getBlockLightLevel(XyzEntity entity, BlockPos pos) {
        return 15;
    }

    @Override
    public void render(XyzEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        var mc = Minecraft.getInstance();
        var player = mc.cameraEntity;
        if (player == null) return;

        float hw = 2.0F;
        float hh = 30.0F;

        poseStack.pushPose();
        poseStack.translate(0, hh, 0);

        Vec3 camPos = player.getEyePosition(partialTick);
        double dx = entity.getX() - camPos.x;
        double dz = entity.getZ() - camPos.z;
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;

        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));

        ResourceLocation tex = getTextureLocation(entity);
        RenderType renderType = RenderType.entityCutoutNoCull(tex);
        VertexConsumer vc = bufferSource.getBuffer(renderType);

        var m = poseStack.last().pose();
        var n = poseStack.last().normal();
        int light = 15 << 20 | 15 << 4;

        vc.vertex(m, -hw, -hh, 0).color(1.0F, 1.0F, 1.0F, 1.0F).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0, 0, 1).endVertex();
        vc.vertex(m, hw, -hh, 0).color(1.0F, 1.0F, 1.0F, 1.0F).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0, 0, 1).endVertex();
        vc.vertex(m, hw, hh, 0).color(1.0F, 1.0F, 1.0F, 1.0F).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0, 0, 1).endVertex();
        vc.vertex(m, -hw, hh, 0).color(1.0F, 1.0F, 1.0F, 1.0F).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0, 0, 1).endVertex();

        if (entity.isActive()) {
            var font = Minecraft.getInstance().font;
            String amountStr = entity.getAmount() + "x " + entity.getRequestedItemName();
            float textW = font.width(amountStr);
            float scale = 0.025F;
            poseStack.pushPose();
            poseStack.translate(0.0F, -hh + 6.0F, 0.05F);
            poseStack.scale(-scale, -scale, scale);
            font.drawInBatch(net.minecraft.network.chat.Component.literal(amountStr).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE),
                    -textW / 2.0F, 0, 0xFFFFFFFF, false, poseStack.last().pose(), bufferSource,
                    net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, 15 << 20 | 15 << 4);
            poseStack.popPose();
        }

        poseStack.popPose();
    }
}
