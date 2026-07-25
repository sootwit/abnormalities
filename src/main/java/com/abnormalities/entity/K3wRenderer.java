package com.abnormalities.entity;

import com.mojang.authlib.GameProfile;
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
            var conn = mc.getConnection();
            if (conn != null) {
                var info = conn.getPlayerInfo(targetUUID);
                if (info != null) return info.getSkinLocation();
            }
        }
        return STEVE;
    }

    @Override
    protected void setupRotations(K3wEntity entity, PoseStack poseStack, float ageInTicks, float yBodyRot, float partialTick) {
        super.setupRotations(entity, poseStack, ageInTicks, yBodyRot, partialTick);
        float gx = (float) Math.sin(ageInTicks * 7.3F) * 0.03F;
        float gz = (float) Math.cos(ageInTicks * 5.1F) * 0.03F;
        poseStack.translate(gx, 0, gz);
        if ((float) Math.sin(ageInTicks * 0.5F) > 0.92F) {
            float sx = 1.0F + (float) Math.sin(ageInTicks * 11.7F) * 0.075F;
            float sy = 1.0F + (float) Math.cos(ageInTicks * 8.3F) * 0.04F;
            poseStack.scale(sx, sy, sx);
        }
    }
}
