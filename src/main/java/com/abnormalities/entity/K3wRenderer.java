package com.abnormalities.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class K3wRenderer extends MobRenderer<K3wEntity, K3wModel> {
    private static final ResourceLocation STEVE = new ResourceLocation("textures/entity/player/wide/steve.png");

    public K3wRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new K3wModel(ctx.bakeLayer(K3wModel.LAYER_LOCATION)), 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(K3wEntity entity) {
        UUID targetUUID = entity.getTargetUUID();
        if (targetUUID != null) {
            var mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.getUUID().equals(targetUUID)) {
                return mc.player.getSkinTextureLocation();
            }
        }
        return STEVE;
    }

    @Override
    protected void setupRotations(K3wEntity entity, PoseStack poseStack, float ageInTicks, float yBodyRot, float partialTick) {
        super.setupRotations(entity, poseStack, ageInTicks, yBodyRot, partialTick);
        float gx = (float) (Math.random() - 0.5) * 0.03F;
        float gz = (float) (Math.random() - 0.5) * 0.03F;
        poseStack.translate(gx, 0, gz);
        if (Math.random() < 0.08) {
            float sx = 1.0F + (float) (Math.random() - 0.5) * 0.15F;
            float sy = 1.0F + (float) (Math.random() - 0.5) * 0.08F;
            poseStack.scale(sx, sy, sx);
        }
    }
}
