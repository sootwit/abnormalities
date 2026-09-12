package com.abnormalities.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class K3wRenderer extends MobRenderer<K3wEntity, K3wModel> {
    private static final ResourceLocation STEVE = new ResourceLocation("textures/entity/player/wide/steve.png");
    private static final ResourceLocation STEVE_SLIM = new ResourceLocation("textures/entity/player/slim/steve.png");
    private static final java.util.Map<UUID, ResourceLocation> SKIN_CACHE = new java.util.HashMap<>();
    private static final java.util.Map<UUID, Boolean> MODEL_CACHE = new java.util.HashMap<>();
    private final K3wModel wideModel;
    private final K3wModel slimModel;

    public K3wRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new K3wModel(ctx.bakeLayer(K3wModel.LAYER_LOCATION), false), 0.0F);
        this.wideModel = this.model;
        this.slimModel = new K3wModel(ctx.bakeLayer(K3wModel.LAYER_LOCATION_SLIM), true);
    }

    @Override
    public ResourceLocation getTextureLocation(K3wEntity entity) {
        UUID targetUUID = entity.getTargetUUID();
        if (targetUUID == null) return STEVE;
        var mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getUUID().equals(targetUUID)) {
            boolean slim = mc.player.getModelName().equals("slim");
            MODEL_CACHE.put(targetUUID, slim);
            this.model = slim ? slimModel : wideModel;
            ResourceLocation skin = mc.player.getSkinTextureLocation();
            entity.setSkinLocation(skin.toString());
            return skin;
        }
        var conn = mc.getConnection();
        if (conn != null) {
            var info = conn.getPlayerInfo(targetUUID);
            if (info != null) {
                ResourceLocation skin = info.getSkinLocation();
                SKIN_CACHE.put(targetUUID, skin);
                boolean slim = info.getModelName().equals("slim");
                MODEL_CACHE.put(targetUUID, slim);
                this.model = slim ? slimModel : wideModel;
                entity.setSkinLocation(skin.toString());
                return skin;
            }
        }
        String stored = entity.getSkinLocation();
        if (stored != null && !stored.isEmpty()) {
            try {
                ResourceLocation skin = new ResourceLocation(stored);
                boolean slim = MODEL_CACHE.getOrDefault(targetUUID, false);
                this.model = slim ? slimModel : wideModel;
                return skin;
            } catch (Exception ignored) {}
        }
        boolean slim = MODEL_CACHE.getOrDefault(targetUUID, false);
        this.model = slim ? slimModel : wideModel;
        return SKIN_CACHE.getOrDefault(targetUUID, slim ? STEVE_SLIM : STEVE);
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
