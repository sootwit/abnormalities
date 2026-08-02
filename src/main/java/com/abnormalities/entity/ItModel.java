package com.abnormalities.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

public class ItModel extends EntityModel<ItEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("abnormalities", "it"), "main");
    private static final float FPS = 12.0F;
    private static final float FRAME = 1.0F / (FPS / 20.0F);

    private final ModelPart root;

    public ItModel(ModelPart root) {
        this.root = root.getChild("figure");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("figure",
                CubeListBuilder.create().texOffs(10, 0)
                        .addBox(-27.0F, -87.0F, -0.1F, 54.0F, 87.0F, 0.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(ItEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isFrozen()) {
            this.root.y = 0.0F;
            this.root.xRot = 0.0F;
            return;
        }
        float snapped = snapFrame(limbSwing * 2.0F);
        float bob = Math.abs((float) Math.sin(snapped * (float) Math.PI)) * 1.2F;
        this.root.y = -bob;
        this.root.xRot = (float) Math.sin(snapped * (float) Math.PI) * 0.08F;
    }

    private float snapFrame(float x) {
        return Math.round(x * FRAME) / FRAME;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}